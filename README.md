<p align="center">
  <img src="app/src/main/ic_launcher-playstore.png" width="128" alt="PlusPlayPiP icon">
</p>

# PlusPlayPiP

[![Build APK](https://github.com/KtyJIxy/plus-play/actions/workflows/build-apk.yml/badge.svg)](https://github.com/KtyJIxy/plus-play/actions/workflows/build-apk.yml)
[![Release](https://img.shields.io/github/v/release/KtyJIxy/plus-play)](https://github.com/KtyJIxy/plus-play/releases/latest)
[![Android 5.0+](https://img.shields.io/badge/Android-5.0%2B-3ddc84)](#download)
[![License: MIT](https://img.shields.io/github/license/KtyJIxy/plus-play)](LICENSE)

A small, no-frills Android video player written in Kotlin. It plays through Android's built-in
`MediaPlayer`, with no extra libraries beyond AndroidX, so the app stays light.

This is a fork of [kmfoysal06/plus-play](https://github.com/kmfoysal06/plus-play), starting from
upstream **v1.0.67**. It adds picture-in-picture, fullscreen in both orientations,
fit/crop/stretch scaling, subtitle delay and size, automatic resume and a green look.

## Screenshots

| Full screen | Picture-in-picture |
|---|---|
| <img src="https://raw.githubusercontent.com/KtyJIxy/plus-play/showcase-media/full.gif" width="480" alt="Full screen playback with subtitles"> | <img src="https://raw.githubusercontent.com/KtyJIxy/plus-play/showcase-media/pip.gif" width="190" alt="Picture-in-picture, switching its size"> |

Subtitles use the size you set and shrink only when a phrase would not fit the allowed lines.
In full screen there is plenty of room, so most phrases keep the full size on one line. In
picture-in-picture the size scales with the small window, so the fitting is easy to see: short
phrases stay large and longer ones shrink and wrap onto two lines.

<p align="center">
  <img src="https://raw.githubusercontent.com/KtyJIxy/plus-play/showcase-media/pip-sizes.png" width="720" alt="Picture-in-picture in three sizes">
</p>
<p align="center">Picture-in-picture: small, big and biggest</p>

| | | |
|---|---|---|
| <img src="https://raw.githubusercontent.com/KtyJIxy/plus-play/showcase-media/06-subtitle-longest.png" width="330" alt="A very long subtitle on two lines"> | <img src="https://raw.githubusercontent.com/KtyJIxy/plus-play/showcase-media/07-controls.png" width="330" alt="Player controls"> | <img src="https://raw.githubusercontent.com/KtyJIxy/plus-play/showcase-media/02-settings.png" width="150" alt="Player settings"> |
| A very long subtitle on two lines | Player controls | Player settings |

<sub>Sample video: <a href="https://peach.blender.org/">Big Buck Bunny</a>, © Blender Foundation, CC BY 3.0. Pictures made on an Android emulator.</sub>

## Download

Get the latest APK from the [Releases page](https://github.com/KtyJIxy/plus-play/releases),
copy it to your phone and open it (Android asks you to allow installing from unknown sources
the first time).

- Android 5.0 (API 21) or newer. Picture-in-picture needs Android 8.0 or newer; on older
  versions the PiP button is hidden and the rest works as usual.
- Storage permission, to list the videos on the device.

Every commit is also built by GitHub Actions; see [Building](#building).

## What this fork adds

### Picture-in-picture (Android 8.0+)
- A PiP button in the player's top bar, and automatic PiP when you leave the app (Home gesture)
  while a video is playing.
- The window takes the video's shape (Android only accepts shapes between 1:2.39 and 2.39:1).
- Rewind 10 s, play/pause and forward 10 s buttons inside the window. Phones that allow only
  three buttons lose forward, to make room for the size button below.
- **Window size (experimental):** a button in the PiP window switches between three sizes:
  small (the video's own shape), big (2.0:1) and biggest (2.38:1). A wider shape gives a wider
  window, and the video is cropped at the top and bottom to fill it. The choice is remembered.
  Android 8.0 also grows the window by about half while its controls are showing, so the
  biggest size fills the screen width when you tap the window.
- Playback continues in the window. Tapping it returns to full screen; closing it with the
  cross closes the player and keeps the resume position.
- Subtitles shrink to fit the window.
- The window size is chosen by Android, not by the app. Wide size works by changing the shape,
  which is the only lever an app has. On Android 12+ you can also pinch the window to resize it.

### Fullscreen
- Hides the navigation bar as well as the status bar; swipe from the screen edge to show the
  bars briefly. Works in landscape and in portrait, so vertical video can fill the screen.

### Fit / Crop / Stretch
- A top-bar button cycles **Fit** (whole picture, black bars), **Crop** (fill the screen, edges
  cut off) and **Stretch** (fill the screen, picture distorted). The choice is remembered.

### Subtitle delay
- Settings (gear icon) has a delay control with -1 s, -0.1 s, +0.1 s and +1 s buttons; tap the
  label to reset. A positive value makes subtitles appear later. Saved per video.
- `.srt` parsing tolerates a UTF-8 byte order mark (it used to drop the first line) and
  different spacing around `-->`.

### Subtitle look and size
- White text with a black outline and no background box.
- Settings (gear icon) has a **Size** control with -4, -1, +1 and +4 buttons; tap the label to
  reset. While you adjust it, sample text is shown if no subtitle is on screen.
- The size is the *largest* size used. For every subtitle the app picks the biggest size at
  which the whole text, one line or several, fits a box over the bottom quarter of the window
  (90% of its width) and within a set number of lines (2 by default). Short phrases get the
  full size, long ones shrink to fit. In full screen most phrases fit at the set size, so the
  fitting shows mostly in picture-in-picture, where the area is taller (33%) because the window
  is small.
- The size is set in dp, so it ignores the system-wide font size setting, and it applies to
  all videos.
- In picture-in-picture the largest size is scaled down in proportion to the window width, so a
  size chosen for full screen also suits the small window.

### Player settings
Main screen, settings (sliders icon), **Player settings**. They apply to every video, before any
launch: largest subtitle size, lines allowed and area height (full screen and picture-in-picture
separately), automatic resume on/off, default picture scaling, default picture-in-picture size,
screen orientation, and whether the app opens on recent videos.

### Orientation
- The rotate button's choice is remembered **per video**, so a vertical video does not open
  sideways and a wide one does not open upright. Without a choice of its own a video uses the
  app-wide default: match the video's shape (default), landscape, portrait or follow the phone.

### Recent videos
- The app opens on the videos played last (newest first); "Recent" is also the first entry of
  the folder list. Switch this off in Player settings.

### Subtitle memory
- The subtitle file you pick for a video is remembered (with its delay), and loaded again next
  time. Without a pick, an `.srt` in the same folder named like the video is used
  (`Movie.srt`, `Movie.en.srt`).

### Resume position
- The position is saved however you leave: Back, closing PiP, swiping the app away, or the system
  killing it. It is also saved every 10 s while playing.
- The last 30 videos are remembered (upstream: 3). Reopening a video continues from where you
  left off, without asking; a "Start over" chip shows for a few seconds if you want the
  beginning.
- The first 5 s are not worth resuming, and the last 5 s count as finished. A video that plays
  to the end is forgotten.

### Smaller changes
- Times show hours when needed (1:02:03), in the player and in the video list.
- Green colour scheme and a new icon.
- A message appears when a file cannot be played, instead of the player closing silently.

## What it costs

- **Behaviour change from upstream:** pressing Back no longer erases the saved position.
- **Size:** the new features add a small amount of code. Release builds are shrunk (R8) and
  debug builds are several MB larger, so compare like with like. Exact APK sizes are listed in
  each build's run summary and in the release assets.
- **Background work:** a small preferences write every 10 s during playback, and subtitles are
  checked 4 times a second instead of twice while a subtitle file is loaded.
- **Top bar:** two more buttons, so the video title gets less room in portrait.
- **Testing:** unit tests cover the logic that does not need a phone (time format, subtitle
  parsing, resume rules, subtitle size fitting) and run on every push. Everything else is tried
  by hand on one phone.

## Limits (unchanged from upstream)

- Playback uses Android's `MediaPlayer`, so it can only play what the phone's decoders handle.
  There is no software decoding: files the phone cannot decode (for example many 10-bit H.264
  "Hi10P" files) may fail to play.
- Only `.srt` subtitles (no `.ass`), loaded from a file next to the video with the same name or
  picked from the settings menu. No embedded subtitle or audio track selection.
- Lists `.mp4`, `.mkv`, `.avi` and `.mov` files.

## Using the player

| Gesture | Action |
|---|---|
| Swipe left / right | Seek (10 s per 100 px) |
| Double-tap left / right | Seek 10 s back / forward |
| Double-tap middle | Play / pause |
| Single tap | Show / hide the controls |

Top bar, left to right: back, title, fit/crop/stretch, picture-in-picture, rotate, settings
(subtitle files, delay and size), lock (disables gestures).

## Building

### GitHub Actions
Every push builds a debug and a release APK and runs the unit tests
(`.github/workflows/build-apk.yml`). The APKs are attached to the run as downloadable artifacts,
and the run summary lists their sizes and the test results. Pushing a tag such as `v1.3.1`,
matching `versionName` in `app/build.gradle`, publishes the signed release APK as a GitHub release,
with notes from `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`.

Forks have Actions switched off until they are enabled once in the **Actions** tab.

**Signing:** without configuration each build is signed with a throwaway key, so a new build
cannot be installed over the previous one. To get a stable signature, create a key and store it
as the repository secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD`;
the build then signs every APK with it.

```bash
keytool -genkeypair -keystore release.jks -alias release -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 release.jks | gh secret set KEYSTORE_BASE64
gh secret set KEYSTORE_PASSWORD
gh secret set KEY_ALIAS --body release
gh secret set KEY_PASSWORD
```

Keep the key file and its passwords safe: without them, updates can only be installed after
uninstalling the app.

### Locally
Needs the Android SDK (platform 33, build-tools 34.0.0) and a JDK between 17 and 21: the bundled
Gradle 8.7 fails on newer JDKs with "Unsupported class file major version".

```bash
./gradlew assembleDebug
# app/build/outputs/apk/debug/app-debug.apk
```

## Project structure

```
app/src/main/
├── java/com/plusplay/app/
│   ├── MainActivity.kt        # folder and video list
│   ├── FolderAdapter.kt       # list adapter
│   ├── PlayerActivity.kt      # player: gestures, subtitles, PiP, resume
│   ├── SubtitleParser.kt      # .srt reading
│   ├── SubtitleFit.kt         # picks the subtitle size that fits
│   ├── ResumePolicy.kt        # which saved positions are worth resuming
│   ├── TimeFormat.kt          # M:SS / H:MM:SS
│   ├── ScalableVideoView.kt   # VideoView with fit / crop / stretch
│   └── VideoFile.kt           # data model
├── res/                       # layouts, drawables, fonts, strings
└── AndroidManifest.xml
app/src/test/                  # unit tests
```

Built on core Android APIs only: `VideoView`, `MediaStore`, `RecyclerView` and `GestureDetector`.

## License

MIT, see [LICENSE](LICENSE). Original work by Kazi Mohammad Foysal. The fork's changes were
written by Claude (Anthropic), directed and tested by KtyJIxy.
