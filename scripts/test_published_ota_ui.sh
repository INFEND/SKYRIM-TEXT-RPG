#!/usr/bin/env bash
# Use a disposable emulator with the preceding public, signed release installed.
set -euo pipefail
expected_version=$1
adb install -g "$RUNNER_TEMP/SKYRIM-TEXT-RPG.apk"
adb shell am start -W -n dev.infend.skyrimtext/.MainActivity
for attempt in $(seq 1 36); do
  if adb shell uiautomator dump /sdcard/ota-window.xml >/dev/null 2>&1 &&
     adb pull /sdcard/ota-window.xml "$RUNNER_TEMP/update-window.xml" >/dev/null 2>&1 &&
     python3 - "$RUNNER_TEMP/update-window.xml" "$expected_version" <<'PY'
import sys
import xml.etree.ElementTree as ET
labels = {node.get("text", "") for node in ET.parse(sys.argv[1]).iter("node")}
required = {"Доступно обновление", "SKYRIM TEXT RPG " + sys.argv[2], "ОБНОВИТЬ"}
sys.exit(0 if required <= labels else 1)
PY
  then
    adb exec-out screencap -p > "$RUNNER_TEMP/update-screen.png"
    echo "The signed game offered version $expected_version in its update dialog."
    exit 0
  fi
  sleep 4
done
adb exec-out screencap -p > "$RUNNER_TEMP/update-screen.png"
echo "Expected in-game update offer for $expected_version was not visible" >&2
exit 1
