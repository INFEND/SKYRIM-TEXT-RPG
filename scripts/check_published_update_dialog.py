#!/usr/bin/env python3
"""Check the automatic offer or explicitly request it in the installed game's settings."""
import os
from pathlib import Path
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

PACKAGE = "dev.infend.skyrimtext"
TEMP = Path(os.environ["RUNNER_TEMP"])


def adb(*args, check=True):
    return subprocess.run(["adb", *args], capture_output=True, check=check, timeout=35)


def window():
    try:
        adb("shell", "uiautomator", "dump", "/sdcard/ota-window.xml")
        output = TEMP / "update-window.xml"
        adb("pull", "/sdcard/ota-window.xml", str(output))
        return [node.attrib for node in ET.parse(output).iter("node") if node.get("package") == PACKAGE]
    except (subprocess.SubprocessError, ET.ParseError, OSError):
        return []


def tap(nodes, label):
    for n in nodes:
        if n.get("text", "").casefold() != label.casefold():
            continue
        bounds = [int(x) for x in re.findall(r"\d+", n.get("bounds", ""))]
        if len(bounds) == 4 and bounds[0] < bounds[2] and bounds[1] < bounds[3]:
            adb("shell", "input", "tap", str((bounds[0]+bounds[2])//2), str((bounds[1]+bounds[3])//2))
            return True
    return False


def scroll(nodes):
    view = next((n for n in nodes if n.get("scrollable") == "true"), None)
    if view is None:
        return
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", view["bounds"]))
    x = str((x1+x2)//2)
    adb("shell", "input", "swipe", x, str(y1+(y2-y1)*4//5), x, str(y1+(y2-y1)//4), "400")


def check_offer(version):
    target = {"Доступно обновление", "SKYRIM TEXT RPG " + version, "ОБНОВИТЬ"}
    stage = "start"
    requested = 0
    for attempt in range(50):
        nodes = window()
        labels = {n.get("text", "") for n in nodes}
        if target <= labels:
            print(f"The installed game offered {version} ({'manual' if requested else 'automatic'} check).")
            return True
        # The first launch can precede the emulator's network connection or the
        # public mirror cache refresh. Use the same manual path available to players.
        if attempt >= 6 and nodes:
            if stage == "start" and tap(nodes, "НАСТРОЙКИ"):
                stage = "settings"
            elif stage == "settings":
                if tap(nodes, "Проверить обновления"):
                    stage, requested = "waiting", requested + 1
                elif any(n.get("content-desc") == "ОБНОВЛЕНИЯ, свёрнуто" for n in nodes):
                    tap(nodes, "ОБНОВЛЕНИЯ")
                else:
                    scroll(nodes)
            elif stage == "waiting" and requested < 3:
                if tap(nodes, "Повторить"):
                    requested += 1
                elif any("актуальн" in label.casefold() for label in labels) and tap(nodes, "Закрыть"):
                    time.sleep(5)
                    stage = "settings"
        time.sleep(2)
    return False


if __name__ == "__main__":
    offered = check_offer(sys.argv[1])
    (TEMP / "update-screen.png").write_bytes(adb("exec-out", "screencap", "-p").stdout)
    if not offered:
        sys.exit("The expected release was not offered inside the game.")
