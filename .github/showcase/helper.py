"""Small helpers for the emulator run: find things on screen from adb dumps."""
import re
import subprocess
import sys
import xml.etree.ElementTree as ET


def adb(*args):
    return subprocess.run(["adb", *args], capture_output=True, text=True).stdout


def center(bounds):
    a, b, c, d = map(int, re.findall(r"\d+", bounds))
    return (a + c) // 2, (b + d) // 2


def pip_center():
    """Middle of the picture-in-picture window, from the window dump (a guess if not found)."""
    dump = adb("shell", "dumpsys", "window", "windows")
    for block in dump.split("Window #")[1:]:
        if "PlayerActivity" in block.split("\n", 1)[0]:
            m = re.search(r"(?:mFrame|frame)=\[(\d+),(\d+)\]\[(\d+),(\d+)\]", block)
            if m:
                a, b, c, d = map(int, m.groups())
                if c - a < 1000:  # smaller than the screen: this is the PiP window
                    return (a + c) // 2, (b + d) // 2
    return 770, 1570


def find_text(needle):
    """Centre of the first on-screen element whose text or description contains `needle`."""
    adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
    xml = adb("shell", "cat", "/sdcard/ui.xml")
    try:
        root = ET.fromstring(xml[xml.index("<"):])
    except Exception:
        return None
    for node in root.iter("node"):
        if needle.lower() in (node.get("content-desc", "") + " " + node.get("text", "")).lower():
            return center(node.get("bounds"))
    return None


if __name__ == "__main__":
    cmd = sys.argv[1]
    if cmd == "pip-center":
        print(*pip_center())
    elif cmd == "find":
        pos = find_text(sys.argv[2])
        if pos:
            print(*pos)
        else:
            sys.exit(1)
