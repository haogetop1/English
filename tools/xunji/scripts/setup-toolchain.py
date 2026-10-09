#!/usr/bin/env python3
"""Install pinned, verified build tools entirely inside this project."""
import hashlib
import os
from pathlib import Path
import shutil
import tarfile
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / '.toolchain'
DOWNLOADS = TOOLS / 'downloads'
DOWNLOADS.mkdir(parents=True, exist_ok=True)


def obtain(url, digest, algorithm='sha256'):
    path = DOWNLOADS / url.rsplit('/', 1)[1]
    if not path.exists():
        print(f'Downloading {path.name}', flush=True)
        temporary = path.with_suffix(path.suffix + '.part')
        with urllib.request.urlopen(url, timeout=120) as response, temporary.open('wb') as output:
            shutil.copyfileobj(response, output)
        temporary.rename(path)
    with path.open('rb') as handle:
        actual = hashlib.file_digest(handle, algorithm).hexdigest()
    if actual != digest:
        raise RuntimeError(f'Checksum mismatch for {path.name}: {actual}')
    print(f'Verified {path.name}', flush=True)
    return path


def extract_zip(archive, destination):
    with zipfile.ZipFile(archive) as zipped:
        for info in zipped.infolist():
            if not (destination / info.filename).resolve().is_relative_to(destination.resolve()):
                raise RuntimeError('Unsafe archive path')
        zipped.extractall(destination)
        for info in zipped.infolist():
            mode = info.external_attr >> 16
            if mode and not info.is_dir():
                os.chmod(destination / info.filename, mode & 0o777)


if not (TOOLS / 'jdk/bin/javac').exists():
    archive = obtain(
        'https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.12.1%2B1/OpenJDK21U-jdk_x64_linux_hotspot_21.0.12.1_1.tar.gz',
        'ce79869e1307ed8ee1e2baa86a412b1eb5b75d10a01006d788a6f968bcfaee94')
    staging = TOOLS / 'jdk-unpack'
    staging.mkdir(exist_ok=True)
    with tarfile.open(archive) as tar:
        tar.extractall(staging, filter='data')
    next(staging.iterdir()).rename(TOOLS / 'jdk')
    staging.rmdir()

if not (TOOLS / 'gradle-8.13/bin/gradle').exists():
    archive = obtain('https://services.gradle.org/distributions/gradle-8.13-bin.zip',
                     '20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78')
    extract_zip(archive, TOOLS)

sdk = TOOLS / 'android-sdk'
if not (sdk / 'cmdline-tools/19.0/bin/sdkmanager').exists():
    archive = obtain('https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip',
                     '5fdcc763663eefb86a5b8879697aa6088b041e70', 'sha1')
    staging = TOOLS / 'sdk-unpack'
    staging.mkdir(exist_ok=True)
    extract_zip(archive, staging)
    (sdk / 'cmdline-tools').mkdir(parents=True, exist_ok=True)
    (staging / 'cmdline-tools').rename(sdk / 'cmdline-tools/19.0')
    staging.rmdir()

print('Pinned JDK, Gradle and Android command-line tools installed.', flush=True)
