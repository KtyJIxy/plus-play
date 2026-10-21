# Plus Play

[![Build APK](https://github.com/KtyJIxy/plus-play/actions/workflows/build-apk.yml/badge.svg)](https://github.com/KtyJIxy/plus-play/actions/workflows/build-apk.yml)

A small, no-frills Android video player written in Kotlin. It plays through Android's built-in
`MediaPlayer`, with no extra libraries beyond AndroidX, so the app stays light.

This is a fork of [kmfoysal06/plus-play](https://github.com/kmfoysal06/plus-play), starting from
upstream **v1.0.67**. It adds picture-in-picture, fullscreen in both orientations,
fit/crop/stretch scaling, subtitle delay and more reliable resume.

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
- **Window size (experimental):** a button in the PiP window cycles the window's shape through
  the video's own shape, 2.0:1, 2.2:1 and 2.38:1. A wider shape gives a wider window, and the
  video is cropped at the top and bottom to fill it. The choice is remembered. Android 8.0
  also grows the window by about half while its controls are showing, so the widest shape
  fills the screen width when you tap the window; the narrower shapes grow less.
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
- The size is the *largest* size used. Subtitles shrink automatically so the whole text, one
  line or several, fits in a box over the bottom quarter of the window (90% of its width), and
  never grow past the setting.
- The size is set in dp, so it ignores the system-wide font size setting, and it applies to
  all videos.
- In picture-in-picture the largest size is scaled down in proportion to the window width, so a
  size chosen for full screen also suits the small window.

### Resume position
- The position is saved however you leave: Back, closing PiP, swiping the app away, or the system
  killing it. It is also saved every 10 s while playing.
- The last 30 videos are remembered (upstream: 3). Reopening a video offers "Resume" or
  "Start from Beginning".
- The first 5 s are not worth resuming, and the last 5 s count as finished. A video that plays
  to the end is forgotten.

### Smaller changes
- A message appears when a file cannot be played, instead of the player closing silently.

## What it costs

- **Behaviour change from upstream:** pressing Back no longer erases the saved position.
- **Size:** the new features add a small amount of code. Release builds are shrunk (R8) and
  debug builds are several MB larger, so compare like with like. Exact APK sizes are listed in
  each build's run summary and in the release assets.
- **Background work:** a small preferences write every 10 s during playback, and subtitles are
  checked 4 times a second instead of twice while a subtitle file is loaded.
- **Top bar:** two more buttons, so the video title gets less room in portrait.
- **Testing:** built and run by hand on one phone. There are no automated tests.

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
Every push builds a debug and a release APK (`.github/workflows/build-apk.yml`). Open the run in
the **Actions** tab and download the artifact; the run summary lists the file sizes. New forks
have Actions switched off until you enable them once in the **Actions** tab.

**Releases:** pushing a tag publishes the signed release APK as a GitHub release. The tag must
match `versionName` in `app/build.gradle`, and the release notes come from
`fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`:

```bash
# 1. bump versionName / versionCode in app/build.gradle, add the changelog file, commit, push
git tag v1.2.0
git push origin v1.2.0
```

**Signing:** by default each build is signed with a throwaway key, so a new build cannot be
installed over the previous one (uninstall first, which also clears saved positions). To make
every build and release install over the last one, create a key once and add it as repository
secrets. Run this inside a clone of the repository, and keep the `.jks` file and its passwords
safe: if you lose them, everyone has to uninstall before updating.

```bash
keytool -genkeypair -keystore plusplay-release.jks -alias plusplay \
        -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 plusplay-release.jks | gh secret set KEYSTORE_BASE64
gh secret set KEYSTORE_PASSWORD     # prompts for the password
gh secret set KEY_ALIAS --body plusplay
gh secret set KEY_PASSWORD          # prompts; the same password is fine
```

To make CI builds install over APKs you compile locally instead, use your local debug key
(`~/.android/debug.keystore`, alias `androiddebugkey`, password `android`). That password is
public, so it is fine for testing but not a secure release key.

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
│   ├── ScalableVideoView.kt   # VideoView with fit / crop / stretch
│   └── VideoFile.kt           # data model
├── res/                       # layouts, drawables, fonts, strings
└── AndroidManifest.xml
```

Built on core Android APIs only: `VideoView`, `MediaStore`, `RecyclerView` and `GestureDetector`.

## License

MIT, see [LICENSE](LICENSE). Original work by Kazi Mohammad Foysal. The fork's changes were
written by Claude (Anthropic), directed and tested by KtyJIxy.
