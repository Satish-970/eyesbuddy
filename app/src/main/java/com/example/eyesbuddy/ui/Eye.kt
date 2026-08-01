package com.example.eyesbuddy.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import com.example.eyesbuddy.data.Emotion
import com.example.eyesbuddy.data.EyeState
import kotlin.math.min

/**
 * Draws one eye. All shape decisions (base height, eyelid slant, iris color)
 * come from [state.emotion]; blink/idle/look direction come from the rest
 * of [state]. Pure OLED-friendly: background must stay pure black, eye
 * colors are the only lit pixels.
 */
@Composable
fun Eye(state: EyeState, size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        drawEye(state, this)
    }
}

private fun emotionBaseOpenRatio(emotion: Emotion): Float = when (emotion) {
    Emotion.SLEEPY -> 0.35f
    Emotion.SURPRISED -> 1.25f
    Emotion.ANGRY -> 0.55f
    Emotion.EXCITED -> 1.15f
    Emotion.HAPPY -> 0.85f
    Emotion.CURIOUS -> 1.05f
    Emotion.NEUTRAL -> 1f
}

private fun irisColor(emotion: Emotion): Color = when (emotion) {
    Emotion.ANGRY -> Color(0xFFFF4433)
    Emotion.HAPPY -> Color(0xFF3DDC84)
    Emotion.EXCITED -> Color(0xFFFFD54A)
    Emotion.SURPRISED -> Color(0xFF4FC3F7)
    Emotion.SLEEPY -> Color(0xFF6E7B8B)
    Emotion.CURIOUS -> Color(0xFF64B5F6)
    Emotion.NEUTRAL -> Color(0xFF64DFDF)
}

private fun drawEye(state: EyeState, scope: DrawScope) {
    val w = scope.size.width
    val h = scope.size.height
    val center = Offset(w / 2f, h / 2f)

    val emotionRatio = emotionBaseOpenRatio(state.emotion)
    // Blink (eyeOpenAmount) always wins over the emotion-based openness,
    // so the eye still fully closes to blink even when e.g. surprised.
    val openness = (emotionRatio * state.eyeOpenAmount).coerceIn(0.02f, 1.3f)

    val baseEyeHeight = h * 0.7f
    val eyeHeight = baseEyeHeight * openness
    val eyeWidth = w * 0.8f

    // Sclera: soft dark shape barely visible (keeps mostly-black OLED look),
    // acts as a subtle boundary for the iris/pupil to sit in.
    scope.drawRoundRect(
        color = Color(0xFF141414),
        topLeft = Offset(center.x - eyeWidth / 2f, center.y - eyeHeight / 2f),
        size = androidx.compose.ui.geometry.Size(eyeWidth, eyeHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(eyeHeight / 2f, eyeHeight / 2f)
    )

    // Iris/pupil offset by look direction, clamped so it never leaves the eye shape.
    val maxOffsetX = (eyeWidth / 2f) * 0.35f
    val maxOffsetY = (eyeHeight / 2f) * 0.35f
    val irisOffset = Offset(state.lookX * maxOffsetX, state.lookY * maxOffsetY)

    val irisRadius = min(eyeWidth, eyeHeight) * 0.32f * state.pupilScale
    scope.drawCircle(
        color = irisColor(state.emotion),
        radius = irisRadius,
        center = center + irisOffset
    )

    val pupilRadius = irisRadius * 0.45f
    scope.drawCircle(
        color = Color.Black,
        radius = pupilRadius,
        center = center + irisOffset
    )

    // Small highlight for life-like sparkle.
    scope.drawCircle(
        color = Color.White.copy(alpha = 0.85f),
        radius = irisRadius * 0.15f,
        center = center + irisOffset + Offset(-irisRadius * 0.3f, -irisRadius * 0.3f)
    )
}
