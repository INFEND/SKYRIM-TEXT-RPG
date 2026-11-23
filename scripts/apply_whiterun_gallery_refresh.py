#!/usr/bin/env python3
"""Replace only Whiterun frames 04-06, keeping all other archive members byte-identical."""
import hashlib
import os
from pathlib import Path
from tempfile import NamedTemporaryFile
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
ORIGINAL = ROOT / 'app/src/main/assets/gallery/whiterun.zip'
REPLACEMENTS = ROOT / 'artwork_sources/whiterun_20261124/replacements.zip'
SLOTS = {'04.webp', '05.webp', '06.webp'}

def check_webp(name, data):
    if not 16000 < len(data) < 2000000 or data[:4] != b'RIFF' or data[8:12] != b'WEBP':
        raise ValueError(f'Invalid WebP frame: {name}')

def main():
    if not ORIGINAL.is_file() or not REPLACEMENTS.is_file():
        raise FileNotFoundError('Whiterun source or replacement archive missing')
    with ZipFile(REPLACEMENTS) as patch, ZipFile(ORIGINAL) as original:
        if patch.testzip() or original.testzip():
            raise ValueError('Corrupt ZIP archive')
        patch_names = [i.filename for i in patch.infolist()]
        original_names = [i.filename for i in original.infolist()]
        if len(patch_names) != len(set(patch_names)) or set(patch_names) != SLOTS:
            raise ValueError('Replacement ZIP must contain exactly frames 04, 05 and 06')
        if len(original_names) != len(set(original_names)) or not {f'{n:02d}.webp' for n in range(1, 7)}.issubset(original_names):
            raise ValueError('Unexpected original Whiterun gallery layout')
        updates = {name: patch.read(name) for name in SLOTS}
        for name, data in updates.items():
            check_webp(name, data)
        before = {name: original.read(name) for name in original_names}
        if all(before[name] == updates[name] for name in SLOTS):
            print('Whiterun gallery already up to date')
            return
        tmp_path = None
        try:
            with NamedTemporaryFile(prefix='whiterun-', suffix='.zip', dir=ORIGINAL.parent, delete=False) as tmp:
                tmp_path = Path(tmp.name)
            with ZipFile(tmp_path, 'w') as rewritten:
                for info in original.infolist():
                    rewritten.writestr(info, updates.get(info.filename, before[info.filename]))
            with ZipFile(tmp_path) as check:
                if check.testzip() or set(check.namelist()) != set(original_names):
                    raise ValueError('Written gallery ZIP is corrupt')
                for name in original_names:
                    expected = updates[name] if name in SLOTS else before[name]
                    if check.read(name) != expected:
                        raise ValueError(f'Frame altered unexpectedly: {name}')
            os.replace(tmp_path, ORIGINAL)
        finally:
            if tmp_path is not None:
                tmp_path.unlink(missing_ok=True)
    print('Whiterun updated: 04=Златолист / Ветреный район, 05=Йоррваскр, 06=Драконий Предел')
    print('Preserved: 01, 02, 03 and all unmodified members')
    print('Gallery SHA256:', hashlib.sha256(ORIGINAL.read_bytes()).hexdigest())

if __name__ == '__main__':
    main()
