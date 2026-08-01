package com.example.eyesbuddy.data

/**
 * Single immutable snapshot of everything the renderer needs for one frame.
 * Produced by combining BlinkManager + MotionSensor + EmotionEngine output
 * inside EyesScreen.
 */
data class EyeState(
    val lookX: Float = 0f,          // -1f (full left) .. 1f (full right)
    val lookY: Float = 0f,          // -1f (full up) .. 1f (full down)
    val eyeOpenAmount: Float = 1f,  // 0f closed (blink) .. 1f fully open
    val pupilScale: Float = 1f,     // dilation, 1f = normal
    val emotion: Emotion = Emotion.NEUTRAL,
    val isAsleep: Boolean = false,
    val batteryLevel: Int = 100,
    val isCharging: Boolean = false
)
