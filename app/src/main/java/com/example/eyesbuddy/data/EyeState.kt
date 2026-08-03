package com.example.eyesbuddy.data

/** Which eye receives a one-shot wink instruction. */
enum class WinkSide { NONE, LEFT, RIGHT }

/** Small decorative event shown around the eyes for celebrations. */
enum class CompanionEffect { NONE, SPARKLES, DIZZY }

/** A single immutable renderer snapshot for the expressive eyes. */
data class EyeState(
    val lookX: Float = 0f,
    val lookY: Float = 0f,
    val eyeOpenAmount: Float = 1f,
    val leftOpenMultiplier: Float = 1f,
    val rightOpenMultiplier: Float = 1f,
    val pupilScale: Float = 1f,
    val eyeStretch: Float = 1f,
    val eyeSquish: Float = 1f,
    val headTilt: Float = 0f,
    val smile: Float = 0f,
    val glow: Float = 0.55f,
    val emotion: Emotion = Emotion.RELAXED,
    val effect: CompanionEffect = CompanionEffect.NONE,
    val isDimmed: Boolean = false,
    val batteryLevel: Int = 100,
    val isCharging: Boolean = false
)

/** State produced by the finite-state behavior engine. */
data class CompanionBehavior(
    val emotion: Emotion = Emotion.RELAXED,
    val action: CompanionAction = CompanionAction.OBSERVE,
    val lookX: Float = 0f,
    val lookY: Float = 0f,
    val pupilScale: Float = 1f,
    val eyeStretch: Float = 1f,
    val eyeSquish: Float = 1f,
    val headTilt: Float = 0f,
    val smile: Float = 0.1f,
    val glow: Float = 0.55f,
    val effect: CompanionEffect = CompanionEffect.NONE,
    val blinkHint: BlinkHint = BlinkHint.NATURAL,
    val winkSide: WinkSide = WinkSide.NONE,
    val isDimmed: Boolean = false
)

enum class BlinkHint { NATURAL, BLINK, DOUBLE_BLINK, SLOW_BLINK, HALF_BLINK, WINK }

enum class CompanionAction {
    LOOK_LEFT,
    LOOK_RIGHT,
    LOOK_UP,
    LOOK_DOWN,
    BLINK,
    DOUBLE_BLINK,
    SMILE,
    CURIOUS,
    THINK,
    PEEK,
    STRETCH,
    SLEEPY,
    YAWN,
    CELEBRATE,
    REACT,
    OBSERVE
}

data class WeatherSnapshot(
    val temperature: Int = 28,
    val condition: String = "Clear",
    val icon: String = "sun",
    val locationName: String = "Offline",
    val feelsLike: Int = temperature,
    val description: String = "Pleasant sky",
    val updatedAtMillis: Long = 0L
)
