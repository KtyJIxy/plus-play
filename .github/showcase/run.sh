#!/usr/bin/env bash
# Runs inside the emulator job: installs the app, plays the sample video and records screenshots
# and two short screen recordings (full screen, and picture-in-picture in all three sizes).
set -uo pipefail
APK="$1"
OUT=showcase-out
H=.github/showcase/helper.py
mkdir -p "$OUT"
PKG=com.plusplay.app
VID=/sdcard/Movies/Sample/sample.mp4

shot() { adb exec-out screencap -p > "$OUT/$1.png"; }
tap() { adb shell input tap "$1" "$2"; }

adb wait-for-device
adb root >/dev/null 2>&1 || true
adb wait-for-device
adb shell settings put system accelerometer_rotation 0
# no "Viewing full screen" popup over the player
adb shell settings put secure immersive_mode_confirmations confirmed
adb install -r -g "$APK"
adb shell pm grant $PKG android.permission.READ_EXTERNAL_STORAGE || true

adb shell mkdir -p /sdcard/Movies/Sample
adb push .github/showcase/sample.mp4 $VID
adb push .github/showcase/sample.srt /sdcard/Movies/Sample/sample.srt
adb shell content call --uri content://media/external/file --method scan_volume --arg external_primary || true
adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file://$VID >/dev/null || true
sleep 8

# Main list and settings
adb shell am start -n $PKG/.MainActivity
sleep 5
shot 01-main
adb shell am start -n $PKG/.SettingsActivity
sleep 3
shot 02-settings
adb shell input keyevent KEYCODE_BACK
sleep 1

# Part 1: full screen (landscape), subtitles of different lengths, then the controls.
# Shots are timed to the subtitle schedule (short, medium, long, longest).
at() { # seconds since the video started
  local now target
  now=$(date +%s.%N); target=$(echo "$T0 + $1" | bc -l)
  local wait; wait=$(echo "$target - $now" | bc -l)
  if (( $(echo "$wait > 0" | bc -l) )); then sleep "$wait"; fi
}
adb shell am start -W -n $PKG/.PlayerActivity --es VIDEO_PATH "$VID" || true
T0=$(date +%s.%N)
sleep 1
adb shell "screenrecord --time-limit 34 /sdcard/full.mp4" &
REC=$!
at 3.3;  shot 03-subtitle-short
at 8.5;  shot 04-subtitle-medium
at 15;   shot 05-subtitle-long
at 22;   shot 06-subtitle-longest
at 26;   tap 1170 540
at 27.5; shot 07-controls
at 31;   tap 1170 540
wait "$REC" 2>/dev/null || true
adb pull /sdcard/full.mp4 "$OUT/full.mp4" || true

# Part 2: picture-in-picture, small -> big -> biggest, subtitles keep fitting the window
adb shell input keyevent KEYCODE_HOME
sleep 4
adb shell "screenrecord --time-limit 60 /sdcard/pip.mp4" &
REC=$!
sleep 3
shot 08-pip-small
sleep 6
shot 08b-pip-small
diag() { { echo "== $1"; adb shell dumpsys activity activities | grep -E "mResumedActivity|PlayerActivity|inPictureInPicture|isInPictureInPictureMode" | head -8; adb logcat -d -b crash | tail -15; adb logcat -d -s PlusPlay:V | tail -5; } >> "$OUT/diag.txt" 2>&1; }
for size in big biggest; do
  read -r X Y < <(python3 $H pip-center)
  tap "$X" "$Y"
  sleep 2
  shot "pip-menu-$size"
  diag "menu-$size"
  # Same as pressing the size button in the window (the player listens for that broadcast)
  adb shell am broadcast -a com.plusplay.app.PIP_CONTROL -p $PKG --ei pip_control 4 >/dev/null
  sleep 1
  diag "after-size-$size"
  sleep 4
  shot "09-pip-$size"
  sleep 4
  shot "09b-pip-$size"
done
wait "$REC" 2>/dev/null || true
adb pull /sdcard/pip.mp4 "$OUT/pip.mp4" || true

adb logcat -d -t 400 > "$OUT/logcat.txt" || true
ls -la "$OUT"
