package com.example.eyesbuddy.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import com.example.eyesbuddy.data.CompanionEffect
import com.example.eyesbuddy.data.Emotion
import com.example.eyesbuddy.data.EyeState
import kotlin.math.min

@Composable
fun Eye(state: EyeState, size: Dp, isLeft: Boolean, modifier: Modifier = Modifier) {
    val lookX by animateFloatAsState(state.lookX, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "lookX")
    val lookY by animateFloatAsState(state.lookY, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "lookY")
    val open by animateFloatAsState(state.eyeOpenAmount, spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium), label = "open")
    val pupil by animateFloatAsState(state.pupilScale, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), label = "pupil")
    val stretch by animateFloatAsState(state.eyeStretch, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "stretch")
    val squish by animateFloatAsState(state.eyeSquish, spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium), label = "squish")
    val smile by animateFloatAsState(state.smile, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "smile")
    val glow by animateFloatAsState(if (state.isDimmed) state.glow * 0.6f else state.glow, label = "glow")

    val sideOpen = if (isLeft) state.leftOpenMultiplier else state.rightOpenMultiplier
    val rendered = state.copy(
        lookX = lookX,
        lookY = lookY,
        eyeOpenAmount = open * sideOpen,
        pupilScale = pupil,
        eyeStretch = stretch,
        eyeSquish = squish,
        smile = smile,
        glow = glow
    )

    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer(rotationZ = state.headTilt * 18f)
    ) {
        drawEye(rendered, isLeft)
    }
}

private fun emotionOpenRatio(emotion: Emotion): Float = when (emotion) {
    Emotion.SLEEPY, Emotion.LOW_BATTERY -> 0.5f
    Emotion.SHY, Emotion.EMBARRASSED -> 0.68f
    Emotion.THINKING -> 0.76f
    Emotion.RELAXED -> 0.9f
    Emotion.HAPPY, Emotion.CHARGING -> 0.98f
    Emotion.CURIOUS, Emotion.PLAYFUL -> 1.06f
    Emotion.EXCITED, Emotion.FULL_BATTERY -> 1.14f
    Emotion.SURPRISED, Emotion.SCARED -> 1.24f
}

private fun emotionColor(emotion: Emotion): Color = when (emotion) {
    Emotion.HAPPY -> Color(0xFF55F0A1)
    Emotion.CURIOUS -> Color(0xFF60D7FF)
    Emotion.SLEEPY -> Color(0xFF7C8EA6)
    Emotion.PLAYFUL -> Color(0xFFFFD166)
    Emotion.THINKING -> Color(0xFFB5A7FF)
    Emotion.SHY, Emotion.EMBARRASSED -> Color(0xFFFF7BB2)
    Emotion.EXCITED, Emotion.FULL_BATTERY -> Color(0xFFFFE66D)
    Emotion.RELAXED -> Color(0xFF70E4D7)
    Emotion.SURPRISED -> Color(0xFF7DD3FC)
    Emotion.SCARED -> Color(0xFFFF6B6B)
    Emotion.CHARGING -> Color(0xFF53F27B)
    Emotion.LOW_BATTERY -> Color(0xFFFF5A4F)
}

private fun DrawScope.drawEye(state: EyeState, isLeft: Boolean) {
    val w = size.width
    val h = size.height
    val center = Offset(w / 2f, h * 0.48f)
    val color = emotionColor(state.emotion)

    val openness = (emotionOpenRatio(state.emotion) * state.eyeOpenAmount * state.eyeSquish).coerceIn(0.025f, 1.35f)
    val eyeWidth = w * 0.78f * state.eyeStretch
    val eyeHeight = h * 0.58f * openness
    val topLeft = Offset(center.x - eyeWidth / 2f, center.y - eyeHeight / 2f)
    val eyeSize = Size(eyeWidth, eyeHeight)
    val radius = CornerRadius(eyeHeight / 2f, eyeHeight / 2f)

    drawCircle(color.copy(alpha = 0.12f * state.glow), radius = eyeWidth * 0.62f, center = center)
    drawCircle(color.copy(alpha = 0.08f * state.glow), radius = eyeWidth * 0.47f, center = center)

    drawRoundRect(
        color = Color(0xFF090B0D),
        topLeft = topLeft,
        size = eyeSize,
        cornerRadius = radius
    )
    drawRoundRect(
        color = color.copy(alpha = 0.22f + 0.18f * state.glow),
        topLeft = topLeft,
        size = eyeSize,
        cornerRadius = radius,
        style = Stroke(width = w * 0.025f)
    )

    drawBrow(state, isLeft, center, eyeWidth, eyeHeight, color)

    if (state.eyeOpenAmount < 0.74f || state.emotion == Emotion.SLEEPY) {
        val lidY = topLeft.y + eyeHeight * 0.12f
        drawLine(
            color = Color.Black.copy(alpha = 0.33f + 0.12f * (1f - state.eyeOpenAmount)),
            start = Offset(topLeft.x + eyeWidth * 0.06f, lidY),
            end = Offset(topLeft.x + eyeWidth * 0.94f, lidY),
            strokeWidth = w * 0.02f,
            cap = StrokeCap.Round
        )
    }

    val maxOffsetX = eyeWidth * 0.24f
    val maxOffsetY = eyeHeight * 0.22f
    val irisCenter = center + Offset(state.lookX * maxOffsetX, state.lookY * maxOffsetY)
    val irisRadius = min(eyeWidth, eyeHeight.coerceAtLeast(h * 0.18f)) * 0.28f * state.pupilScale

    drawCircle(color.copy(alpha = 0.95f), radius = irisRadius, center = irisCenter)
    drawCircle(Color.Black, radius = irisRadius * 0.48f, center = irisCenter)
    drawCircle(Color.White.copy(alpha = 0.82f), radius = irisRadius * 0.13f, center = irisCenter + Offset(-irisRadius * 0.32f, -irisRadius * 0.32f))

    if (state.smile > 0.05f) {
        val smileWidth = eyeWidth * (0.32f + state.smile * 0.12f)
        val smileTop = center.y + eyeHeight * (0.28f + 0.08f * state.smile)
        drawArc(
            color = color.copy(alpha = 0.55f * state.smile),
            startAngle = 18f,
            sweepAngle = 144f,
            useCenter = false,
            topLeft = Offset(center.x - smileWidth / 2f, smileTop - h * 0.12f),
            size = Size(smileWidth, h * 0.24f),
            style = Stroke(width = w * 0.024f)
        )
    }

    if ((state.emotion == Emotion.SHY || state.emotion == Emotion.EMBARRASSED) && isLeft) {
        drawCircle(Color(0xFFFF5C9A).copy(alpha = 0.18f), radius = w * 0.1f, center = Offset(w * 0.18f, h * 0.7f))
    }

    if (state.emotion == Emotion.SHY || state.emotion == Emotion.EMBARRASSED) {
        val blushX = if (isLeft) w * 0.28f else w * 0.72f
        drawCircle(Color(0xFFFF5C9A).copy(alpha = 0.12f), radius = w * 0.08f, center = Offset(blushX, h * 0.73f))
    }

    if (state.effect == CompanionEffect.SPARKLES) {
        drawSparkles(color)
    }
}

private fun DrawScope.drawBrow(
    state: EyeState,
    isLeft: Boolean,
    center: Offset,
    eyeWidth: Float,
    eyeHeight: Float,
    color: Color
) {
    val lift = when (state.emotion) {
        Emotion.SLEEPY -> -0.12f
        Emotion.LOW_BATTERY -> -0.08f
        Emotion.THINKING -> 0.02f
        Emotion.SHY, Emotion.EMBARRASSED -> 0.03f
        Emotion.RELAXED -> 0.04f
        Emotion.HAPPY -> 0.06f
        Emotion.CURIOUS -> 0.1f
        Emotion.PLAYFUL -> 0.08f
        Emotion.EXCITED, Emotion.CHARGING, Emotion.FULL_BATTERY -> 0.14f
        Emotion.SURPRISED -> 0.2f
        Emotion.SCARED -> 0.16f
    }
    val slant = when (state.emotion) {
        Emotion.SLEEPY, Emotion.LOW_BATTERY -> -0.16f
        Emotion.THINKING -> 0.1f
        Emotion.SHY, Emotion.EMBARRASSED -> -0.04f
        Emotion.RELAXED -> 0.02f
        Emotion.HAPPY -> 0.06f
        Emotion.CURIOUS -> 0.14f
        Emotion.PLAYFUL -> 0.1f
        Emotion.EXCITED, Emotion.CHARGING, Emotion.FULL_BATTERY -> 0.18f
        Emotion.SURPRISED -> 0.26f
        Emotion.SCARED -> -0.22f
    }
    val browY = center.y - eyeHeight * 0.92f + lift * size.height * 0.16f
    val innerX = if (isLeft) center.x + eyeWidth * 0.2f else center.x - eyeWidth * 0.2f
    val outerX = if (isLeft) center.x - eyeWidth * 0.26f else center.x + eyeWidth * 0.26f
    val innerY = browY + slant * eyeHeight * 0.34f
    val outerY = browY - slant * eyeHeight * 0.34f

    drawLine(
        color = color.copy(alpha = 0.42f + 0.3f * state.glow),
        start = Offset(innerX, innerY),
        end = Offset(outerX, outerY),
        strokeWidth = size.width * 0.028f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawSparkles(color: Color) {
    val points = listOf(
        Offset(size.width * 0.18f, size.height * 0.18f),
        Offset(size.width * 0.84f, size.height * 0.24f),
        Offset(size.width * 0.76f, size.height * 0.78f)
    )
    points.forEachIndexed { index, point ->
        val r = size.minDimension * (0.025f + index * 0.006f)
        drawLine(color.copy(alpha = 0.7f), point + Offset(-r, 0f), point + Offset(r, 0f), strokeWidth = r * 0.35f)
        drawLine(color.copy(alpha = 0.7f), point + Offset(0f, -r), point + Offset(0f, r), strokeWidth = r * 0.35f)
    }
}
