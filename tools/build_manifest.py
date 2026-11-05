#!/usr/bin/env python3
"""Produce signed APK release metadata with cumulative, validated history."""
import datetime
import hashlib
import json
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[1]
build = (ROOT / "app/build.gradle").read_text(encoding="utf-8")
version = re.search(r"^\s*versionName\s+'([^']+)'", build, re.M).group(1)
code = int(re.search(r"^\s*versionCode\s+(\d+)", build, re.M).group(1))
assert re.fullmatch(r"[A-Za-z0-9._-]{1,40}", version)
apk = ROOT / "dist/SKYRIM-TEXT-RPG.apk"
digest = hashlib.sha256(apk.read_bytes()).hexdigest()
history_file = ROOT / "release_notes/history.json"
history = json.loads(history_file.read_text(encoding="utf-8"))
if not isinstance(history, list) or not history:
    raise ValueError("Release history must be a non-empty list")
codes = set()
names = set()
previous = 0
for item in history:
    vc, vn, notes = item["versionCode"], item["versionName"], item["notes"]
    if not isinstance(vc, int) or vc <= previous:
        raise ValueError("History must be ordered by versionCode")
    if vc in codes or vn in names or not notes.strip():
        raise ValueError("Duplicate or empty release history entry")
    if not re.fullmatch(r"[A-Za-z0-9._-]{1,40}", vn):
        raise ValueError("Invalid history versionName")
    if not all(line.startswith(("[+] ", "[-] ", "[=] ")) for line in notes.splitlines() if line.strip()):
        raise ValueError("Release notes must use visual markers")
    codes.add(vc)
    names.add(vn)
    previous = vc
current = history[-1]
if current["versionCode"] != code or current["versionName"] != version:
    raise ValueError("Latest history entry doesn't match Android app version")
base = "https://github.com/INFEND/SKYRIM-TEXT-RPG/releases/download/"
payload = {
    "schema": 1,
    "packageName": "dev.infend.skyrimtext",
    "versionCode": code,
    "versionName": version,
    "apkUrl": f"{base}v{version}/SKYRIM-TEXT-RPG.apk",
    "sha256": digest,
    "sizeBytes": apk.stat().st_size,
    "notes": current["notes"],
    "history": history,
    "publishedAt": datetime.datetime.now(datetime.timezone.utc).isoformat(),
}
dist = ROOT / "dist"
dist.mkdir(exist_ok=True)
(dist / "update.json").write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
(dist / "release_notes.md").write_text(current["notes"], encoding="utf-8")
print(f"OTA v{version}, code {code}, hash {digest}, history entries {len(history)}")
