#!/usr/bin/env bash
set -euo pipefail
expected_version=$1
package=dev.infend.skyrimtext
install_info(){
  adb shell dumpsys package "$package" | sed -nE 's/^[[:space:]]*(firstInstallTime|userId)=([^[:cntrl:]]+).*/\1=\2/p'
}
original_install=$(install_info)
test -n "$original_install"
adb install -r "$RUNNER_TEMP/new-release/SKYRIM-TEXT-RPG.apk"
test "$(install_info)" = "$original_install"
adb shell dumpsys package "$package" > "$RUNNER_TEMP/installed-version.txt"
python3 - "$RUNNER_TEMP/installed-version.txt" "$expected_version" <<'PY'
from pathlib import Path
import re, sys
installed = Path(sys.argv[1]).read_text()
assert re.search(r"^\s*versionName=" + re.escape(sys.argv[2]) + r"\s*$", installed, re.M)
PY
adb shell am force-stop "$package"
adb shell am start -W -n "$package/.MainActivity"
for attempt in $(seq 1 18); do
  if adb shell uiautomator dump /sdcard/updated-game.xml >/dev/null 2>&1 &&
     adb pull /sdcard/updated-game.xml "$RUNNER_TEMP/updated-game.xml" >/dev/null 2>&1 &&
     python3 - "$RUNNER_TEMP/updated-game.xml" "$package" <<'PY'
import sys
import xml.etree.ElementTree as ET
labels = [n.get("text", "").strip() for n in ET.parse(sys.argv[1]).iter("node")
          if n.get("package") == sys.argv[2]]
sys.exit(0 if sum(bool(label) for label in labels) >= 2 else 1)
PY
  then
    adb exec-out screencap -p > "$RUNNER_TEMP/updated-game.png"
    echo "The signed upgrade preserved the package installation and opened version $expected_version."
    exit 0
  fi
  sleep 2
done
echo "No game window opened after the Android package upgrade." >&2
exit 1
