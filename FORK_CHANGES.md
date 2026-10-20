# Fork changes

This fork adds features to [kmfoysal06/plus-play](https://github.com/kmfoysal06/plus-play),
starting from upstream **v1.0.67** (commit `4e9045a`). Where this file and the main README
disagree, this file describes the fork's behaviour.

## What was added

### Picture-in-picture (Android 8.0+)
- A PiP button in the player's top bar, and automatic PiP when you leave the app (Home
  gesture) while a video is playing.
- The PiP window takes the video's shape. Android only accepts shapes between 1:2.39 and
  2.39:1, so extreme aspect ratios are clamped.
- Rewind 10 s, play/pause and forward 10 s buttons inside the PiP window. Phones usually allow
  only three buttons, so when the size button below is shown, forward is left out.
- **Wide size (experimental):** a button in the PiP window switches between the video's own
  shape and the widest shape Android accepts (2.38:1). The window gets wider and the video is
  cropped at the top and bottom to fill it. The choice is remembered.
- Playback continues in the window. Tapping it returns to full screen; closing it with the
  cross closes the player and keeps the resume position.
- Subtitles shrink to fit the small window. Fit / crop / stretch is ignored in PiP (always fit).
- The **size** of the PiP window is chosen by Android, not by the app (the app only supplies
  the shape, which is what wide size uses). On Android 12+ you can
  pinch the window to resize it, or double-tap it to toggle between the normal and the
  maximum size.

### Fullscreen
- The navigation bar is hidden as well as the status bar; swiping from the screen edge shows
  the bars briefly. Works in landscape and in portrait, so vertical video can fill the screen.

### Fit / Crop / Stretch
- A top-bar button cycles **Fit** (whole picture, black bars), **Crop** (fill the screen,
  edges cut off) and **Stretch** (fill the screen, picture distorted).
- The choice is remembered between videos.

### Subtitle delay
- Settings (gear icon) has a **Subtitle delay** row. Buttons shift subtitles by -1 s, -0.1 s,
  +0.1 s and +1 s; tapping the label resets it. A positive value makes subtitles appear later.
- The delay is saved per video.
- `.srt` parsing now tolerates a UTF-8 byte order mark (it used to drop the first cue) and
  different spacing around `-->`.

### Resume position
- The position is saved when you leave by any route: Back, closing PiP, swiping the app
  away, or the system killing it. It is also saved every 10 s while playing.
- The last 30 videos are remembered (upstream: 3). Reopening a video offers
  "Resume" / "Start from Beginning".
- The first 5 s are not worth resuming; within the last 5 s a video counts as finished.
  A video that plays to the end is forgotten.

### Smaller changes
- A message appears when a file cannot be played (for example an unsupported codec), instead
  of the player closing silently.

## What it costs

- **Minimum Android version is unchanged: 5.0 (API 21).** PiP needs Android 8.0, so below that
  the PiP button is hidden and no PiP code runs. This is by design and has not been tested on
  a pre-8.0 device.
- **Size:** about 10 KB of extra code in a release build (an estimate, not a measurement).
  Debug builds are several MB larger than release builds because they are not shrunk; compare
  like with like.
- **Behaviour change from upstream:** pressing Back no longer erases the saved position.
- **Writes and wake-ups:** a small preferences write every 10 s during playback (about 150
  bytes per remembered video), and subtitles are checked 4 times a second instead of twice
  while a subtitle file is loaded.
- **Top bar:** one more button, so the video title gets less room in portrait.

## Limits that are unchanged

- Playback uses Android's built-in `MediaPlayer`, so it can only play what the phone's
  decoders handle. There is no software decoding: 10-bit H.264 ("Hi10P") and similar files
  usually will not play.
- Only `.srt` subtitles (no `.ass`), and no embedded subtitle or audio track selection.

## Status

Built and run by hand on one device. The project has no automated tests.

## Building

### GitHub Actions
Every push builds a debug and a release APK (`.github/workflows/build-apk.yml`). Open the run
in the **Actions** tab and download the artifact. The run summary lists the file sizes.

New forks have Actions switched off until you enable them once in the **Actions** tab.

By default each build is signed with a throwaway key, so a new build cannot be installed over
the previous one (uninstall first, which also clears saved positions). To make every build
install over the last one, add these repository secrets
(**Settings > Secrets and variables > Actions**): `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`,
`KEY_ALIAS`, `KEY_PASSWORD`. For example, to reuse the debug key from your own computer:

```bash
base64 -w0 ~/.android/debug.keystore | gh secret set KEYSTORE_BASE64
gh secret set KEYSTORE_PASSWORD --body android
gh secret set KEY_ALIAS --body androiddebugkey
gh secret set KEY_PASSWORD --body android
```

Builds from that key then install over ones you compiled locally.

### Locally
Needs the Android SDK (platform 33, build-tools 34.0.0) and a JDK between 17 and 21: the
bundled Gradle 8.7 fails on newer JDKs with "Unsupported class file major version".

```bash
./gradlew assembleDebug
# app/build/outputs/apk/debug/app-debug.apk
```
