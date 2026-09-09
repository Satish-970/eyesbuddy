package com.example.eyesbuddy.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eyesbuddy.domain.clock.DigitFlipState

/** Renders one independently animated mechanical-style split-flap digit. */
@Composable
fun FlipDigit(value: Char, brightness: Float, modifier: Modifier = Modifier) {
    val state = remember { DigitFlipState(value) }
    val changed = state.update(value)
    val rotation by animateFloatAsState(
        targetValue = if (changed) 180f else 0f,
        animationSpec = tween(260),
        label = "digitFlip"
    )
    Box(
        modifier = modifier.width(42.dp).height(58.dp).background(Color(0xFF171A20)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value.toString(),
            color = Color.White.copy(alpha = (0.95f * brightness).coerceIn(0.4f, 1f)),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            fontSize = 30.sp,
            modifier = Modifier.graphicsLayer {
                rotationX = rotation
                cameraDistance = 12f * density
            }
        )
    }
}
