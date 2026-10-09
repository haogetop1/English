#!/usr/bin/env bash
# Source this file from any working directory.
XUNJI_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
export XUNJI_ROOT
export JAVA_HOME="$XUNJI_ROOT/.toolchain/jdk"
export ANDROID_HOME="$XUNJI_ROOT/.toolchain/android-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export GRADLE_USER_HOME="$XUNJI_ROOT/.cache/gradle"
export ANDROID_USER_HOME="$XUNJI_ROOT/.cache/android"
export ANDROID_AVD_HOME="$ANDROID_USER_HOME/avd"
export ANDROID_EMULATOR_HOME="$ANDROID_USER_HOME"
export TMPDIR="$XUNJI_ROOT/.tmp"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/19.0/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
mkdir -p "$GRADLE_USER_HOME" "$ANDROID_USER_HOME" "$ANDROID_AVD_HOME" "$TMPDIR" "$XUNJI_ROOT/.cache/java-home"
# Java tools do not consume HTTPS_PROXY themselves. Keep credentials out of files.
if [[ -z "${XUNJI_ENV_INITIALIZED+x}" ]]; then
  export XUNJI_BASE_JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-}"
  export XUNJI_ENV_INITIALIZED=1
fi
export JAVA_TOOL_OPTIONS="$XUNJI_BASE_JAVA_TOOL_OPTIONS -Duser.home=$XUNJI_ROOT/.cache/java-home"
if [[ -n "${HTTPS_PROXY:-${https_proxy:-}}" ]]; then
  read -r XUNJI_PROXY_HOST XUNJI_PROXY_PORT < <(python3 - <<'PY'
import os, urllib.parse
p=urllib.parse.urlsplit(os.environ.get('HTTPS_PROXY') or os.environ.get('https_proxy',''))
print(p.hostname or '',p.port or 80)
PY
  )
  export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dhttps.proxyHost=$XUNJI_PROXY_HOST -Dhttps.proxyPort=$XUNJI_PROXY_PORT -Dhttp.proxyHost=$XUNJI_PROXY_HOST -Dhttp.proxyPort=$XUNJI_PROXY_PORT -Dhttp.nonProxyHosts=localhost|127.*"
fi
# Use the managed machine's maintained trust store when available.
if [[ -f /etc/ssl/certs/java/cacerts ]]; then
  export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts"
fi
