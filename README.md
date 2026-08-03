# EMO Screen Companion

A Kotlin + Jetpack Compose Android companion screen inspired by tiny desktop robots: expressive living eyes, a premium OLED clock, date, and weather on a pure black display.

This is not a wallpaper or charging animation. The main screen is Companion Mode: no visible navigation, no clutter, just animated eyes that keep making small decisions every second.

## What is implemented

- Pure black OLED-first Companion Mode.
- Responsive mobile layout that adapts to portrait and landscape without locking the screen orientation.
- Expressive Canvas eyes with spring-driven gaze, pupil dilation, glow, stretch, squish, brows, blush, sleepy/curious/excited/shy/surprised/charging/full/low-battery states.
- One-second finite-state behavior loop: observe, choose weighted action, perform, repeat.
- Natural blink system with random blinks, double blinks, slow blinks, half-blinks, and winks.
- Touch reactions: tap = curious, double tap = playful wink and 12/24-hour toggle, long press = shy plus hidden settings panel.
- Drag/release reactions: dragging makes the companion track your finger, and lift-off triggers a short "drop" reaction.
- Tilt tracking from the accelerometer, shake reaction, charging wake-up, unplug surprise, full-battery celebration.
- Digit-by-digit flip clock with seconds, AM/PM support, and adjustable brightness.
- Live weather card with real-time temperature, condition, location, feels-like, and update time.
- Front-camera face sensing that shows thought bubbles like "you’re back" and reacts to detected face presence/mood cues.
- Optional loud-sound detector class kept off by default until microphone permission is requested.

## Project structure

```text
app/src/main/java/com/example/eyesbuddy/
  animation/   BlinkManager.kt, EmotionEngine.kt
  data/        Emotion.kt, EyeState.kt
  sensors/     MotionSensor.kt, BatteryReceiver.kt
  sound/       ClapDetector.kt
  ui/          EyesScreen.kt, Eye.kt
  weather/     WeatherRepository.kt
  MainActivity.kt
```

## Build

This checkout has Gradle wrapper properties but not the generated wrapper scripts/JAR. Build through Android Studio, or push to GitHub and use the included workflow, which regenerates the wrapper before `assembleDebug`.

### Android Studio

1. Open this folder as an Android Studio project.
2. Let Gradle sync.
3. Run the `app` configuration on a phone or emulator.

### GitHub Actions

The workflow at `.github/workflows/build.yml` builds a debug APK and uploads it as `eyes-buddy-debug-apk`.

## Notes

- Weather is fetched live from a public endpoint and cached locally for fallback when the network is unavailable.
- Face sensing uses the front camera and needs camera permission granted on the device.
- Hilt and a full settings/onboarding stack are not added yet; the core architecture is separated enough to introduce them cleanly.
- The app intentionally avoids `java.time` so minSdk 24 works without extra desugaring dependencies.
