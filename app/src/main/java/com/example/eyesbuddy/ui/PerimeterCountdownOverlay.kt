package com.example.eyesbuddy.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.eyesbuddy.domain.perimeter.PerimeterTimerController
import kotlinx.coroutines.delay

/** Draws a crisp one-minute perimeter progress line without glow or shadow effects. */
@Composable
fun PerimeterCountdownOverlay(color: Color, modifier: Modifier = Modifier) {
    var progress by remember { mutableFloatStateOf(0f) }
    val controller = remember { PerimeterTimerController() }
    LaunchedEffect(Unit) {
        while (true) {
            progress = controller.progressAt(System.currentTimeMillis())
            delay(250L)
        }
    }
    Canvas(modifier = modifier.fillMaxSize()) {
        val inset = 2.dp.toPx()
        val path = Path().apply {
            moveTo(inset, inset)
            lineTo(size.width - inset, inset)
            lineTo(size.width - inset, size.height - inset)
            lineTo(inset, size.height - inset)
            close()
        }
        val measured = PathMeasure()
        measured.setPath(path, false)
        val visible = Path()
        measured.getSegment(0f, measured.length * progress, visible, true)
        drawPath(visible, color = color, style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Butt))
    }
}
