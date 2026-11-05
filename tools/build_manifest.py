#!/usr/bin/env python3
"""Generate OTA metadata from the actual release APK."""
import datetime
import hashlib
import json
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[1]
source = (ROOT / "app/build.gradle").read_text(encoding="utf-8")
version_name = re.search(r"^\s*versionName\s+'([^']+)'", source, re.M).group(1)
version_code = int(re.search(r"^\s*versionCode\s+(\d+)", source, re.M).group(1))
apk = ROOT / "dist/SKYRIM-TEXT-RPG.apk"
with apk.open("rb") as f:
    sha256 = hashlib.file_digest(f, "sha256").hexdigest()
base = "https://github.com/INFEND/SKYRIM-TEXT-RPG/releases/download/"
notes = [
    "Новая сюжетная кампания «Пепельные печати».",
    "Создание героя, исследование 8 регионов, бои, снаряжение и задания.",
    "Автосохранение, AMOLED-темы и внутриигровые OTA-обновления.",
]
changelog_path = ROOT / "CHANGELOG.md"
if changelog_path.exists():
    changelog = changelog_path.read_text(encoding="utf-8")
    section = re.search(r"^##\s+" + re.escape(version_name) + r"\s*\n(.*?)(?=^##\s+|\Z)", changelog, re.M | re.S)
    if section:
        parsed = [line.strip()[2:] for line in section.group(1).splitlines() if line.strip().startswith("- ")]
        if parsed:
            notes = parsed
payload = {
    "schema": 1,
    "versionCode": version_code,
    "versionName": version_name,
    "apkUrl": f"{base}v{version_name}/SKYRIM-TEXT-RPG.apk",
    "sha256": sha256,
    "publishedAt": datetime.datetime.now(datetime.timezone.utc).isoformat(),
    "changelog": notes,
}
(ROOT / "dist/update.json").write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
(ROOT / "dist/release_notes.md").write_text("\n".join(f"- {note}" for note in notes) + "\n", encoding="utf-8")
print(f"Prepared v{version_name} / versionCode {version_code} / sha256 {sha256}")
