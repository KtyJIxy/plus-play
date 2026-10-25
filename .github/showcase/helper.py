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


def pip_frame():
    """(left, top, right, bottom) of the picture-in-picture window, from the window dump."""
    dump = adb("shell", "dumpsys", "window", "windows")
    for block in dump.split("Window #")[1:]:
        if "PlayerActivity" in block.split("\n", 1)[0]:
            m = re.search(r"(?:mFrame|frame)=\[(\d+),(\d+)\]\[(\d+),(\d+)\]", block)
            if m:
                a, b, c, d = map(int, m.groups())
                if c - a < 1000:  # smaller than the screen: this is the PiP window
                    return a, b, c, d
    return 560, 1450, 980, 1700


def pip_center():
    """Middle of the picture-in-picture window."""
    a, b, c, d = pip_frame()
    return (a + c) // 2, (b + d) // 2


def pip_size_button():
    """The size button is the third action in the row along the bottom of the controls."""
    a, b, c, d = pip_frame()
    return int(a + (c - a) * 0.69), int(b + (d - b) * 0.85)


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
    elif cmd == "pip-size-button":
        print(*pip_size_button())
    elif cmd == "find":
        pos = find_text(sys.argv[2])
        if pos:
            print(*pos)
        else:
            sys.exit(1)
