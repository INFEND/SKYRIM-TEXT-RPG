#!/usr/bin/env python3
"""Offline regression for the OTA metadata compatibility contract."""
import hashlib
import json
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[1]
manifest = json.loads((ROOT/"dist/update.json").read_text(encoding="utf-8"))
gradle = (ROOT/"app/build.gradle").read_text(encoding="utf-8")
manager = (ROOT/"app/src/main/java/dev/infend/skyrimtext/UpdateManager.java").read_text(encoding="utf-8")
real_id = "dev.infend.skyrimtext"
legacy_field = "dev.xarkon.link"
version = re.search(r"^\s*versionName\s+'([^']+)'", gradle,re.M).group(1)
code = int(re.search(r"^\s*versionCode\s+(\d+)",gradle,re.M).group(1))
apk = ROOT/"dist/SKYRIM-TEXT-RPG.apk"
assert manifest["packageName"] == legacy_field, "Installed legacy clients will reject this manifest"
assert manifest["apkPackageName"] == real_id
assert manifest["legacyPackageField"] is True
assert f'"{real_id}"' in manager or "BuildConfig.APPLICATION_ID" in manager
assert "archive.packageName" in manager and "getApkContentsSigners" in manager
assert manifest["versionCode"]==code and manifest["versionName"]==version
assert manifest["apkUrl"]==f"https://github.com/INFEND/SKYRIM-TEXT-RPG/releases/download/v{version}/SKYRIM-TEXT-RPG.apk"
assert manifest["sha256"]==hashlib.sha256(apk.read_bytes()).hexdigest()
assert manifest["sizeBytes"]==apk.stat().st_size
thin=ROOT/"dist/SKYRIM-TEXT-RPG-thin.apk"
if thin.is_file():
    entry=manifest["thinApk"]
    assert entry["url"]==f"https://github.com/INFEND/SKYRIM-TEXT-RPG/releases/download/v{version}/SKYRIM-TEXT-RPG-thin.apk"
    assert entry["sha256"]==hashlib.sha256(thin.read_bytes()).hexdigest()
    assert entry["sizeBytes"]==thin.stat().st_size < apk.stat().st_size
assert len(manifest["history"])>=2
assert manifest["history"][-1]["versionCode"]==code
print("OTA compatibility regression PASS (legacy metadata, current package, SHA-256, versions, history)")
