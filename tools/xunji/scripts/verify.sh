#!/usr/bin/env bash
set -euo pipefail
TASK_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
source "$TASK_ROOT/scripts/env.sh"
mkdir -p "$TASK_ROOT/artifacts/test-results"
cd "$TASK_ROOT/android"
./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest --console=plain 2>&1 | tee "$TASK_ROOT/artifacts/test-results/build.log"
python3 "$TASK_ROOT/scripts/summarize-tests.py"
cp app/build/reports/lint-results-debug.* "$TASK_ROOT/artifacts/test-results/"
cp app/build/outputs/apk/debug/app-debug.apk "$TASK_ROOT/artifacts/xunji-0.1.0-debug.apk"
"$ANDROID_HOME/build-tools/36.0.0/apksigner" verify --verbose --print-certs "$TASK_ROOT/artifacts/xunji-0.1.0-debug.apk" > "$TASK_ROOT/artifacts/test-results/signature.txt"
"$ANDROID_HOME/build-tools/36.0.0/aapt" dump badging "$TASK_ROOT/artifacts/xunji-0.1.0-debug.apk" > "$TASK_ROOT/artifacts/test-results/package.txt"
"$ANDROID_HOME/build-tools/36.0.0/aapt" dump permissions "$TASK_ROOT/artifacts/xunji-0.1.0-debug.apk" > "$TASK_ROOT/artifacts/test-results/permissions.txt"
python3 - "$TASK_ROOT" <<'PY'
from pathlib import Path
import sys
root=Path(sys.argv[1]);badging=(root/'artifacts/test-results/package.txt').read_text();permissions=(root/'artifacts/test-results/permissions.txt').read_text()
assert "name='com.xunji.flashcards.debug'" in badging
assert "sdkVersion:'26'" in badging
assert "targetSdkVersion:'36'" in badging
for forbidden in ['android.permission.INTERNET','READ_EXTERNAL_STORAGE','WRITE_EXTERNAL_STORAGE','MANAGE_EXTERNAL_STORAGE']:
    assert forbidden not in permissions,forbidden
PY
cd "$TASK_ROOT/artifacts"
sha256sum xunji-0.1.0-debug.apk > xunji-0.1.0-debug.apk.sha256
cat xunji-0.1.0-debug.apk.sha256
