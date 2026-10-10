#!/usr/bin/env bash
set -euo pipefail
TASK_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
python3 "$TASK_ROOT/scripts/setup-toolchain.py"
source "$TASK_ROOT/scripts/env.sh"
license_log="$TASK_ROOT/.cache/sdk-licenses.log"
set +o pipefail
yes 2>/dev/null | sdkmanager --sdk_root="$ANDROID_HOME" --licenses >"$license_log" 2>&1
license_status=${PIPESTATUS[1]}
set -o pipefail
if [[ "$license_status" != 0 ]]; then tail -n 30 "$license_log"; exit "$license_status"; fi
sdkmanager --sdk_root="$ANDROID_HOME" 'platforms;android-36' 'build-tools;36.0.0' 'platform-tools'
python3 -m pip install --disable-pip-version-check --cache-dir "$TASK_ROOT/.cache/pip" --target "$TASK_ROOT/.toolchain/python" 'adb-shell==0.4.4'
javac -version
