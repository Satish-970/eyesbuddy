# Eyes Buddy

Minimalist OLED "living eyes" companion app for Android, built with Kotlin +
Jetpack Compose, matching the architecture we discussed:

- Natural blinking (3-8s random interval) via `BlinkManager`
- Tilt-based "looking around" + shake -> Surprised via `MotionSensor`
- Charging -> Happy baseline, charger-just-connected -> Excited, low battery
  -> Sleepy, via `BatteryReceiver`
- Tap -> Curious, rapid repeated taps -> Angry, via `EmotionEngine`
- Sleep after 2 minutes idle, wake on touch/shake/charge, via `EyesScreen`
- Pure `#000000` background, only the eyes are lit -> OLED power saving
- Optional loud-sound detection (`ClapDetector`) using raw mic amplitude only
  - no audio is ever recorded to disk or sent anywhere. It's wired but not
  auto-started; call `ClapDetector.start()` after requesting `RECORD_AUDIO`
  if you want that feature live.

## Getting an installable APK

I can't compile the APK directly from this sandboxed environment (no access
to Google's Maven repo for the Android SDK/Compose compiler). Two easy ways
to get a real `.apk` from here:

### Option A — GitHub Actions (no local setup needed)

1. Push this project to a new GitHub repo.
2. GitHub Actions will automatically run `.github/workflows/build.yml`,
   which builds a debug APK using the real Android SDK on GitHub's runners.
3. Go to the repo's **Actions** tab -> latest run -> download the
   `eyes-buddy-debug-apk` artifact. Unzip it, you'll have `app-debug.apk`.
4. Transfer it to your phone (or use `adb install app-debug.apk`) and enable
   "install unknown apps" for that source if prompted.

You can also trigger it manually anytime from the Actions tab
("Run workflow" button) since the workflow has `workflow_dispatch` enabled.

### Option B — Android Studio (local build + run)

1. Install [Android Studio](https://developer.android.com/studio) (handles
   the SDK download for you).
2. Open this folder as a project (`File > Open`).
3. Let Gradle sync (first sync downloads dependencies, needs internet).
4. Click **Run** to install straight to a connected phone/emulator, or use
   `Build > Build App Bundle(s) / APK(s) > Build APK(s)` to get a `.apk` file
   under `app/build/outputs/apk/debug/`.

## Project structure

```
app/src/main/java/com/example/eyesbuddy/
 ├── ui/            EyesScreen.kt, Eye.kt   (Compose UI + Canvas rendering)
 ├── animation/     BlinkManager.kt, EmotionEngine.kt
 ├── sensors/       MotionSensor.kt, BatteryReceiver.kt
 ├── sound/         ClapDetector.kt         (optional, off by default)
 ├── data/          Emotion.kt, EyeState.kt
 └── MainActivity.kt
```

## Notes / things worth tuning once it's on a real device

- `MotionSensor`'s shake threshold (`shakeThreshold = 12f`) and `ClapDetector`'s
  loudness threshold (`loudnessThreshold = 9000`) are starting points — tune
  them against your actual phone's sensor noise floor.
- Frame rate: Compose only recomputes on state change here, so it's already
  naturally throttled to sensor/animation event rate rather than a fixed
  loop; there's no separate FPS timer to configure.
- To wire up the optional mic feature: request `RECORD_AUDIO` at runtime
  (Android 6+), then call `clapDetector.start(scope)`, and feed
  `clapDetector.loudSoundPulse` into `EmotionEngine`/wake logic the same way
  `shakePulse` is wired in `EyesScreen.kt`.
