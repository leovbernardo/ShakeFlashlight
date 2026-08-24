# Shake Flashlight

Shake your phone to toggle the flashlight on/off — works even when the screen is locked.

## What's implemented

- **Toggle behavior**: first shake turns the flashlight on, next shake turns it off.
- **Works while locked**: a foreground service keeps the accelerometer listener and torch
  control alive independent of the screen/activity, with a persistent low-priority
  notification (required by Android for background sensor + camera use).
- **Adjustable sensitivity**: a slider (Settings screen in the app) maps to the
  acceleration threshold used to detect a shake — slide toward "less sensitive" to
  cut down on accidental triggers from walking/pocket movement.
- **Anti-double-trigger logic**: a single hard shake won't fire multiple toggles —
  the detector requires acceleration to drop back down before it will arm again,
  plus a 700ms debounce.
- **Quick control from the notification**: a "Stop" action to kill the service
  without opening the app.

## Build it

1. Install [Android Studio](https://developer.android.com/studio) (latest stable).
2. Open this folder (`ShakeFlashlight/`) as a project — File → Open.
3. Let Gradle sync (Android Studio will generate the Gradle wrapper automatically
   on first sync if it's missing).
4. Connect your Galaxy S26U via USB with USB debugging enabled, and hit Run ▶,
   **or** build a release APK: Build → Generate Signed App Bundle / APK → APK,
   create/select a keystore, build the release variant.
5. Copy the resulting `app-release.apk` to your phone and install it (you'll need
   to allow "install unknown apps" for whichever app you use to open the file).

## Important: OneUI battery optimization

Samsung's OneUI is aggressive about killing background services to save battery.
For the shake detection to keep working reliably while the screen is locked:

1. Go to **Settings → Apps → Shake Flashlight → Battery**.
2. Set it to **Unrestricted** (not "Optimized" or "Sleeping apps").
3. Also check **Settings → Battery and device care → Background usage limits**
   and make sure Shake Flashlight isn't added to "Sleeping apps" or "Deep sleeping apps".

Without this, OneUI may suspend the service after a while even though it's a
foreground service, especially overnight or under battery saver.

## Building from your phone only (no PC)

A GitHub Actions workflow (`.github/workflows/build.yml`) is already included.
Push this project to a GitHub repo and it builds a debug APK for you in the
cloud — see the step-by-step in the chat where this project was generated.

If the build fails with an error about `platforms;android-36` not being found,
edit `app/build.gradle.kts` and `.github/workflows/build.yml`, lowering
`compileSdk`/`targetSdk` and the platform/build-tools version to the latest
one actually available (e.g. `35` / `platforms;android-35`), then push again.

## Where to tweak things

- `Prefs.kt` — sensitivity range (`MIN_THRESHOLD` / `MAX_THRESHOLD`, in m/s² above
  gravity) and the debounce window constant is in `ShakeDetectorService.kt`
  (`DEBOUNCE_MS`).
- `ShakeDetectorService.kt` — the core shake-detection state machine and torch control.
- `MainActivity.kt` — UI and permission handling.

## Permissions requested

- `CAMERA` — required to control the torch via `CameraManager`.
- `POST_NOTIFICATIONS` — required (Android 13+) to show the foreground service notification.
- `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_CAMERA` — required (Android 14+) to run a
  foreground service that uses the camera.
