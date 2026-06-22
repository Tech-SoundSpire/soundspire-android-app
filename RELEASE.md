# SoundSpire Android — Release & Distribution Guide

How to build, sign, distribute, and update the Android app. The app is **not** on the
Play Store — it's distributed as a signed APK via the website's "Download Android App"
button, and updates are surfaced through an in-app update prompt.

---

## 1. Key facts

| Thing | Value |
|---|---|
| Package name (permanent identity) | `com.aistudio.soundspire.vsqtyz` |
| Signing keystore | `my-upload-key.jks` (alias `upload`) — **back this up; losing it means you can't ship updates** |
| Signing secrets | `keystore.properties` (gitignored) |
| Release SHA-1 (registered in Google Cloud for Sign-In) | `80:C1:E6:92:38:24:0F:FA:BB:36:6E:0F:4D:C1:59:15:38:E8:97:E9` |
| minSdk | 24 (Android 7.0+) |
| Backend / API | `https://app.soundspire.online` |
| APK S3 bucket | `soundspireandroidassets` (region `ap-south-1`, **private**) |
| Current version | `versionCode = 2`, `versionName = "1.1"` |

> ⚠️ Every release APK **must** be signed with the same `my-upload-key.jks`. A different
> key won't install over an existing build (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`) and
> breaks Google Sign-In (its SHA-1 is registered in Google Cloud).

---

## 2. Build a signed release APK

```bash
# 1. Bump the version in app/build.gradle.kts (defaultConfig):
#      versionCode = <previous + 1>   # MUST increase — this is the update gate
#      versionName = "1.1"            # display string

# 2. Build (signing is wired via keystore.properties / env vars)
./gradlew assembleRelease
```

Output:
- `app/build/dist/soundspire-v<versionName>.apk`  ← **the shareable file** (auto-named)
- `app/build/outputs/apk/release/app-release.apk`  ← identical, default name

Verify version + signature before shipping:
```bash
AAPT=$(ls "$HOME/Library/Android/sdk/build-tools/"*/aapt2 | sort -V | tail -1)
APKSIGNER=$(ls "$HOME/Library/Android/sdk/build-tools/"*/apksigner | sort -V | tail -1)
"$AAPT" dump badging app/build/dist/soundspire-v1.1.apk | grep -E "package:|sdkVersion"
"$APKSIGNER" verify --print-certs app/build/dist/soundspire-v1.1.apk | grep "SHA-1"
# SHA-1 must be 80:C1:E6:... (release key)
```

---

## 3. How the website serves the APK

All "Download Android App" buttons (landing header, mobile menu, left Navbar, Explore
banner) link to the **same path: `/download/android`** — they are version-agnostic.

That route (`soundspire-frontend/src/app/download/android/route.ts`) signs a short-lived
S3 URL for `APK_BUCKET/APK_KEY` and 307-redirects the browser to download it, forcing a
download via `Content-Disposition: attachment`. The bucket stays **private**.

**`APK_KEY` is the single thing that decides which APK version the button downloads.**

The in-app update prompt (`UpdateChecker`) is separate: it fetches
`/api/app-version` (`soundspire-frontend/src/app/api/app-version/route.ts`) on launch,
compares `latestVersionCode` against the installed `BuildConfig.VERSION_CODE`, and shows
an update dialog whose "Update" button opens `/download/android`.

---

## 4. Website environment variables

These are **runtime** env vars read by the Next.js server (not build-time). Set them in
the production host's environment, then **restart/redeploy** the server for changes to
take effect. (They are NOT in the repo; each has a hardcoded fallback default.)

### Used by `/download/android` (the Download button)
| Var | Default | Purpose |
|---|---|---|
| `APK_KEY` | `apkReleases/v1.0/soundspire-v1.0.apk` | **S3 path of the APK to serve.** Bump this every release. |
| `APK_BUCKET` | `soundspireandroidassets` | S3 bucket (rarely changes). |
| `APK_AWS_REGION` | `ap-south-1` | S3 region (rarely changes). |
| `APK_DOWNLOAD_NAME` | `soundspire.apk` | Filename saved on the user's device. |

### Used by `/api/app-version` (the in-app update prompt)
| Var | Default | Purpose |
|---|---|---|
| `APK_LATEST_VERSION_CODE` | `1` | **Must equal the new APK's `versionCode`.** Triggers the prompt when > installed version. |
| `APK_LATEST_VERSION_NAME` | `1.0` | Shown in the dialog ("Latest version: 1.1"). |
| `APK_UPDATE_MANDATORY` | `false` | `true` = blocking dialog (no "Later"). Use only for critical updates. |
| `APK_UPDATE_MESSAGE` | generic text | Optional custom dialog message. |

> AWS credentials: the routes use `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` (or the
> `BUCKET_AWS_*` fallbacks). That IAM identity must have `s3:GetObject` on
> `arn:aws:s3:::soundspireandroidassets/apkReleases/*`.

---

## 5. Full release checklist (example: shipping v1.1)

1. **Bump version** in `app/build.gradle.kts`: `versionCode = 2`, `versionName = "1.1"`.
2. **Build**: `./gradlew assembleRelease` → `app/build/dist/soundspire-v1.1.apk`.
3. **Verify** version (`2`) and SHA-1 (`80:C1:E6:...`) as in section 2.
4. **Upload** to S3 at `apkReleases/v1.1/soundspire-v1.1.apk`.
5. **Set website env vars** and redeploy/restart:
   - `APK_KEY = apkReleases/v1.1/soundspire-v1.1.apk`  ← Download button now serves v1.1
   - `APK_LATEST_VERSION_CODE = 2`                      ← existing users get the prompt
   - `APK_LATEST_VERSION_NAME = 1.1`
   - (optional) `APK_UPDATE_MANDATORY = true`, `APK_UPDATE_MESSAGE = "..."`
6. **Done.** New downloaders get v1.1 from the button; existing v1.0 users see the update
   prompt next launch → tap Update → download v1.1.

### Minimum vars that must change per release
- `APK_KEY` → new APK path (else the button serves the OLD apk)
- `APK_LATEST_VERSION_CODE` → new versionCode (else NO ONE is prompted)
- `APK_LATEST_VERSION_NAME` → display only

### Common gotchas
- Forgetting to bump `versionCode` → update prompt never fires.
- Setting `APK_LATEST_VERSION_CODE` higher than the APK actually uploaded → users told to
  update but the download is stale/missing.
- Building with a different signing key → won't install over existing app + breaks Sign-In.
- Existing testers with an older differently-signed build must **uninstall first**.

---

## 6. Install for testing (sideload)

```bash
adb install -r app/build/dist/soundspire-v1.1.apk      # over same-key build
# If signature differs from what's installed:
adb uninstall com.aistudio.soundspire.vsqtyz && adb install app/build/dist/soundspire-v1.1.apk
```
Users sideloading will see a Play Protect warning → "install anyway" is expected for a
non-Play app.

---

## 7. Google Sign-In note

Sign-In is gated on the **signing cert SHA-1 + package name** being registered as an
Android OAuth client in Google Cloud (project `421253082792`). The release key's SHA-1 is
already registered. The web OAuth client `421253082792-...apps.googleusercontent.com` is
the `serverClientId` the app sends; don't change it. Occasional One Tap flakiness
(picker not appearing) is a Google Play Services state issue, not a code bug — it
typically self-recovers.
