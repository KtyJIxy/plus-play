#!/usr/bin/env bash
# Runs inside the emulator job: installs the app, plays a generated sample video and records
# screenshots and a short screen video into showcase-out/.
set -uo pipefail
APK="$1"
OUT=showcase-out
mkdir -p "$OUT"
PKG=com.plusplay.app
VID=/sdcard/Movies/Sample/sample.mp4

shot() { adb exec-out screencap -p > "$OUT/$1.png"; }
wait_ui() { sleep "${1:-2}"; }

adb wait-for-device
adb root >/dev/null 2>&1 || true
adb wait-for-device
adb shell settings put system accelerometer_rotation 0
adb install -r -g "$APK"
adb shell pm grant $PKG android.permission.READ_EXTERNAL_STORAGE || true
adb shell pm grant $PKG android.permission.WRITE_EXTERNAL_STORAGE || true

adb shell mkdir -p /sdcard/Movies/Sample
adb push .github/showcase/sample.mp4 $VID
adb push .github/showcase/sample.srt /sdcard/Movies/Sample/sample.srt
adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file://$VID >/dev/null
wait_ui 3

# Start recording (the emulator's recorder stops after the time limit)
adb shell "screenrecord --time-limit 45 --size 720x1280 /sdcard/demo-portrait.mp4" &
REC=$!

# Main list (opens on the Recent / folder list)
adb shell am start -n $PKG/.MainActivity
wait_ui 5
shot 01-main

# Player settings screen
adb shell am start -n $PKG/.SettingsActivity
wait_ui 3
shot 02-settings
adb shell input keyevent KEYCODE_BACK
wait_ui 1

# Play the sample video
adb shell am start -n $PKG/.PlayerActivity --es VIDEO_PATH "$VID"
wait_ui 6
shot 03-player-subtitle-short
wait_ui 6
shot 04-player-subtitle-medium
wait_ui 7
shot 05-player-subtitle-long

# Controls (tap the middle of the screen)
adb shell input tap 540 800
wait_ui 2
shot 06-controls

# Picture-in-picture: Home gesture
adb shell input keyevent KEYCODE_HOME
wait_ui 3
shot 07-pip

wait "$REC" 2>/dev/null || true
adb pull /sdcard/demo-portrait.mp4 "$OUT/demo.mp4" || true
adb logcat -d -t 300 > "$OUT/logcat.txt" || true
ls -la "$OUT"
