"""Writes the sample subtitles: phrases of different lengths, so the size fitting is visible."""
import sys

PHRASES = [
    "Short line",
    "A medium length phrase that fits on one line",
    "A much longer phrase that needs two lines, because it keeps going on and on and on",
    "And an even longer one, the kind that shows how the size shrinks so that the whole text still fits in the area at the bottom of the window",
    "Back to something short",
    "Two quick words",
]

def stamp(sec):
    ms = int(round(sec * 1000))
    return "%02d:%02d:%02d,%03d" % (ms // 3600000, ms // 60000 % 60, ms // 1000 % 60, ms % 1000)

out = []
t = 1.5
n = 1
while t < 118:
    text = PHRASES[(n - 1) % len(PHRASES)]
    dur = 3.5 if len(text) < 30 else 5.5
    out.append("%d\n%s --> %s\n%s\n" % (n, stamp(t), stamp(t + dur), text))
    t += dur + 1.0
    n += 1
open(sys.argv[1], "w", encoding="utf-8").write("\n".join(out))
