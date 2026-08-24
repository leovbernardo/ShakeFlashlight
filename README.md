# Shake Flashlight

Shake your phone 3 times in a row to toggle the flashlight on or off — works
even when the screen is locked.

## Features

- **Gesture**: 3 shakes within 1.5 seconds toggles the flashlight. Requiring
  a short burst (not just one shake) prevents accidental triggers from
  bumps, drops, or normal handling.
- **Works while locked**: a foreground service keeps the accelerometer
  listener and torch control running independent of the screen, with a
  persistent low-priority notification (required by Android for background
  sensor + camera use).
- **Adjustable sensitivity**: a slider in the app controls how hard a shake
  needs to be to count as one.
- **Notification control**: a "Stop" action to kill the service without
  opening the app.

## Build it

**With Android Studio:**
1. Install [Android Studio](https://developer.android.com/studio).
2. Open this folder as a project and let Gradle sync.
3. Run on a connected device, or Build → Generate Signed App Bundle / APK
   for a release build.

**From a browser only (e.g. on your phone), using GitHub Actions:**
A workflow at `.github/workflows/build.yml` is already included. Push this
project to a GitHub repo and it builds a debug APK in the cloud — download it
from the repo's Actions tab under Artifacts once the run finishes.

If the build fails on `platforms;android-36` not being found, lower
`compileSdk`/`targetSdk` in `app/build.gradle.kts` and the platform version in
the workflow file to the latest one actually available (e.g. `35`).

## Battery optimization

Background service reliability varies by manufacturer. If shakes stop being
detected after a while (especially overnight or under battery saver), find
your phone's battery settings for this app and set it to unrestricted /
exempt from sleep, rather than "optimized."

## Where to tweak things

- `Prefs.kt` — sensitivity range (`MIN_THRESHOLD` / `MAX_THRESHOLD`, m/s²
  above gravity), set by the in-app slider.
- `ShakeDetectorService.kt` — core shake-detection logic and torch control.
  `SHAKES_REQUIRED` (default 3) sets shakes per gesture, `SHAKE_WINDOW_MS`
  (default 1500) sets the time window, `MIN_PEAK_GAP_MS` filters sensor noise.
- `MainActivity.kt` — UI and permission handling.

## Permissions

- `CAMERA` — controls the torch via `CameraManager`.
- `POST_NOTIFICATIONS` — shows the foreground service notification (Android 13+).
- `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_CAMERA` — required to run a
  foreground service that uses the camera (Android 14+).
