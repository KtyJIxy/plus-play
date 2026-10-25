# Releasing a new version

Everything is done in the browser and one terminal. No clone of the repository is needed.
Examples use `fish`; they also work in bash.

## One-time setup (already done)

- **Signing key:** the repository secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and
  `KEY_PASSWORD` hold the key every release is signed with. Because of it, a new version installs
  over the old one and keeps its settings and saved positions.
- **Keep safe:** the key file `plusplay-release.jks` and its password. Without them the next
  release has to be installed after uninstalling the old app. GitHub cannot show the secrets again.
- **Terminal tool:** `sudo pacman -S github-cli`, then `gh auth login` (GitHub.com, HTTPS, browser).

If the secrets ever have to be recreated (new key, lost secrets), run this once. Save the
printed password in the password manager together with the `.jks` file:

```fish
set pw (openssl rand -base64 18)
echo $pw
keytool -genkeypair -keystore ~/plusplay-release.jks -alias plusplay \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass $pw -keypass $pw -dname "CN=PlusPlayPiP"
base64 -w0 ~/plusplay-release.jks | gh secret set KEYSTORE_BASE64 -R KtyJIxy/plus-play
printf '%s' $pw | gh secret set KEYSTORE_PASSWORD -R KtyJIxy/plus-play
printf '%s' $pw | gh secret set KEY_PASSWORD -R KtyJIxy/plus-play
gh secret set KEY_ALIAS --body plusplay -R KtyJIxy/plus-play
gh secret list -R KtyJIxy/plus-play
```

Restoring the key from a password manager that stored it as text:
`base64 -d < saved.txt > ~/plusplay-release.jks`.

## Every release

1. **Version:** in `app/build.gradle` raise `versionName` (for example `"1.3.1"`) and
   `versionCode` (131; one more every time).
2. **Release notes:** create `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`
   (for example `131.txt`), a few lines starting with `- `. These become the release text.
3. **Merge to `main`:** commit the changes on a branch, open a pull request on GitHub, wait for
   the green check (the build also runs the unit tests), press **Merge**.
   Check that the merge really happened: the pull request page says "Merged" (purple).
4. **Tag the merged `main`:** the tag must be `v` plus `versionName`. This creates it on the
   newest `main` without a local clone:

   ```fish
   gh api repos/KtyJIxy/plus-play/git/refs -f ref=refs/tags/v1.3.1 \
     -f sha=(gh api repos/KtyJIxy/plus-play/commits/main --jq .sha)
   ```

5. **Wait:** open the **Actions** tab. When the run for the tag is green, the **Releases** page has
   the new release with the signed APK (`plus-play-<version>.apk`) and the notes from step 2.
   Do not create the release by hand; the build does it.
6. **Install:** download the APK to the phone and open it. It installs over the old version.

## Something went wrong

- **"Tag vX does not match app version":** the tag was put on a commit where `versionName` is
  different (usually before the merge). Delete the tag, wait for the merge, tag again:

  ```fish
  gh api -X DELETE repos/KtyJIxy/plus-play/git/refs/tags/v1.3.1
  ```

  and repeat step 4. Also delete a half-made release on the Releases page.
- **Build red:** open the failed run in the Actions tab; the red step names the problem. The
  summary of the run lists unit test results.
- **App refuses to install over the old one:** the old one was signed with a different key (builds
  made before the key was added). Uninstall it once; saved settings and positions are lost once.
- **Every push also builds APKs** (Actions run, "Artifacts" section) for testing before a release.
  Those are signed with the same key once the secrets exist.
