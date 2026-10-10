#!/usr/bin/env python3
"""Direct ADB client for an owned local emulator; avoids adb's global home writes."""
import argparse
from pathlib import Path
import re
import shlex
import sys
import base64
import time

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'.toolchain/python'))
from adb_shell.adb_device import AdbDeviceTcp

parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--port',type=int,default=5555)
parser.add_argument('--remote',help='Guest path for pull')
parser.add_argument('action',choices=['shell','install','instrument','screenshot','pull','read-app'])
parser.add_argument('argument',nargs='?')
args=parser.parse_args()
device=AdbDeviceTcp('127.0.0.1',args.port,default_transport_timeout_s=180)
device.connect(read_timeout_s=30,auth_timeout_s=30)
try:
    if args.action=='shell':
        print(device.shell(args.argument,read_timeout_s=60,timeout_s=180),flush=True)
    elif args.action=='install':
        apk=Path(args.argument).resolve()
        if not apk.is_relative_to(ROOT): raise ValueError('APK must be inside the project')
        remote='/data/local/tmp/'+apk.name
        device.push(str(apk),remote,read_timeout_s=60)
        result=device.shell('pm install -r -t '+shlex.quote(remote),read_timeout_s=180,timeout_s=300)
        print(result,flush=True)
        if result.strip()!='Success': raise RuntimeError('Installation failed')
    elif args.action=='instrument':
        runner='com.xunji.flashcards.debug.test/androidx.test.runner.AndroidJUnitRunner'
        selection=' -e class '+shlex.quote(args.argument) if args.argument else ''
        chunks=[]
        started=time.monotonic()
        for chunk in device.streaming_shell('am instrument -w -r'+selection+' '+runner,read_timeout_s=180):
            if time.monotonic()-started>900: raise TimeoutError('Instrumentation exceeded 15 minutes')
            chunks.append(chunk)
            print(chunk,end='',flush=True)
        result=''.join(chunks)
        match=re.search(r'OK \(([1-9][0-9]*) tests?\)',result)
        if not match or 'FAILURES!!!' in result: raise RuntimeError('Instrumentation did not report a passing non-empty suite')
    elif args.action=='screenshot':
        output=Path(args.argument).resolve()
        if not output.is_relative_to(ROOT): raise ValueError('Screenshot must be inside the project')
        output.parent.mkdir(parents=True,exist_ok=True)
        device.shell('screencap -p /data/local/tmp/xunji-screen.png',read_timeout_s=60)
        device.pull('/data/local/tmp/xunji-screen.png',str(output),read_timeout_s=60)
        print(output)
    elif args.action=='pull':
        output=Path(args.argument).resolve()
        if not output.is_relative_to(ROOT): raise ValueError('Destination must be inside the project')
        if not args.remote: raise ValueError('--remote is required')
        output.parent.mkdir(parents=True,exist_ok=True)
        device.pull(args.remote,str(output),read_timeout_s=180)
        print(output)
    elif args.action=='read-app':
        output=Path(args.argument).resolve()
        if not output.is_relative_to(ROOT): raise ValueError('Destination must be inside the project')
        if not args.remote or args.remote.startswith('/') or '..' in Path(args.remote).parts: raise ValueError('Use a relative app file path')
        encoded=device.shell('run-as com.xunji.flashcards.debug base64 '+shlex.quote(args.remote),read_timeout_s=180)
        output.parent.mkdir(parents=True,exist_ok=True)
        output.write_bytes(base64.b64decode(encoded,validate=False))
        print(output)
finally:
    device.close()
