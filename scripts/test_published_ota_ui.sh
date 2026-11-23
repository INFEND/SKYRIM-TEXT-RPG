#!/usr/bin/env bash
# Exercise the public update offer using the preceding signed Android release.
set -euo pipefail
adb install -g "$RUNNER_TEMP/SKYRIM-TEXT-RPG.apk"
adb shell am start -W -n dev.infend.skyrimtext/.MainActivity
python3 scripts/check_published_update_dialog.py "$1"
