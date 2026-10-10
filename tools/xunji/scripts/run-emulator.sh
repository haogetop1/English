#!/usr/bin/env bash
set -euo pipefail
TASK_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
source "$TASK_ROOT/scripts/env.sh"
api="${1:-26}"
port="${2:-5556}"
[[ "$api" == 26 || "$api" == 35 ]] || { echo 'API must be 26 or 35'; exit 2; }
[[ "$port" =~ ^[0-9]+$ && "$port" -ge 5554 && "$port" -le 5682 && $((port%2)) -eq 0 ]] || { echo 'Choose an even emulator port from 5554 to 5682'; exit 2; }
python3 - "$port" <<'PY'
import socket,sys
for port in [int(sys.argv[1]),int(sys.argv[1])+1]:
    with socket.socket() as s:
        if s.connect_ex(('127.0.0.1',port))==0:
            sys.exit(f'Port {port} is already in use. Inspect the owned emulator before starting another.')
PY
image="system-images;android-$api;default;x86_64"
sdkmanager --sdk_root="$ANDROID_HOME" 'emulator' "$image"
name="xunji-api$api"
if [[ ! -d "$ANDROID_AVD_HOME/$name.avd" ]]; then
    echo no | avdmanager create avd -n "$name" -k "$image"
    if [[ "$api" == 26 ]]; then
        python3 - "$ANDROID_AVD_HOME/$name.avd/config.ini" <<'PY'
from pathlib import Path
import sys
p=Path(sys.argv[1]);rows=p.read_text().splitlines();values={'hw.lcd.width':'480','hw.lcd.height':'800','hw.lcd.density':'160'}
rows=[r for r in rows if r.split('=')[0] not in values];p.write_text('\n'.join(rows+[f'{k}={v}' for k,v in values.items()])+'\n')
PY
    fi
fi
acceleration=off
[[ -r /dev/kvm && -w /dev/kvm ]] && acceleration=on
nohup "$ANDROID_HOME/emulator/emulator" -avd "$name" -port "$port" -accel "$acceleration" -gpu swiftshader_indirect -no-window -no-audio -no-snapshot -no-boot-anim -memory 1536 -cores 1 >"$TASK_ROOT/.cache/emulator-$api.log" 2>&1 &
echo "$!" > "$TASK_ROOT/.cache/emulator-$api.pid"
echo "Started $name; software boot may take several minutes. Verify readiness:"
echo "python3 scripts/device.py --port $((port+1)) shell 'getprop sys.boot_completed'"
