package com.example.eyesbuddy.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.using
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eyesbuddy.animation.BlinkManager
import com.example.eyesbuddy.animation.EmotionEngine
import com.example.eyesbuddy.data.EyeState
import com.example.eyesbuddy.data.WeatherSnapshot
import com.example.eyesbuddy.sensors.BatteryReceiver
import com.example.eyesbuddy.sensors.MotionSensor
import com.example.eyesbuddy.weather.WeatherRepository
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EyesScreen() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            CompanionMode()
        }
    }
}

@Composable
private fun CompanionMode() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val blinkManager = remember { BlinkManager(scope).apply { start() } }
    val emotionEngine = remember { EmotionEngine(scope) }
    val motionSensor = remember { MotionSensor(context) }
    val batteryReceiver = remember { BatteryReceiver(context) }
    val weatherRepository = remember { WeatherRepository() }

    val behavior by emotionEngine.behavior.collectAsState()
    val eyeOpen by blinkManager.eyeOpenAmount.collectAsState(initial = 1f)
    val leftOpen by blinkManager.leftOpenMultiplier.collectAsState(initial = 1f)
    val rightOpen by blinkManager.rightOpenMultiplier.collectAsState(initial = 1f)
    val tilt by motionSensor.tilt.collectAsState(initial = 0f to 0f)
    val shakePulse by motionSensor.shakePulse.collectAsState(initial = 0L)
    val isCharging by batteryReceiver.isCharging.collectAsState(initial = false)
    val batteryLevel by batteryReceiver.batteryLevel.collectAsState(initial = 100)
    val justConnectedPulse by batteryReceiver.justConnected.collectAsState(initial = 0L)
    val justDisconnectedPulse by batteryReceiver.justDisconnected.collectAsState(initial = 0L)
    val justFullPulse by batteryReceiver.justFull.collectAsState(initial = 0L)
    val weather by weatherRepository.weather.collectAsState()

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var lastInteractionMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var touchLook by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var stageSize by remember { mutableStateOf(IntSize.Zero) }
    var clockBrightness by remember { mutableFloatStateOf(1f) }
    var use24Hour by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        motionSensor.start()
        batteryReceiver.register()
        onDispose {
            motionSensor.stop()
            batteryReceiver.unregister()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1000)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            weatherRepository.refresh()
            delay(60 * 60 * 1000L)
        }
    }

    LaunchedEffect(behavior) {
        blinkManager.perform(behavior.blinkHint, behavior.winkSide)
    }

    LaunchedEffect(isCharging) { emotionEngine.setCharging(isCharging) }
    LaunchedEffect(batteryLevel) { emotionEngine.setBatteryLevel(batteryLevel) }
    LaunchedEffect(justConnectedPulse) {
        if (justConnectedPulse > 0) emotionEngine.reportChargerJustConnected()
    }
    LaunchedEffect(justDisconnectedPulse) {
        if (justDisconnectedPulse > 0) emotionEngine.reportChargerDisconnected()
    }
    LaunchedEffect(justFullPulse) {
        if (justFullPulse > 0) emotionEngine.reportBatteryFull()
    }
    LaunchedEffect(shakePulse) {
        if (shakePulse > 0) emotionEngine.reportShake()
    }

    LaunchedEffect(touchLook) {
        if (touchLook != null) {
            delay(1500)
            touchLook = null
        }
    }

    LaunchedEffect(lastInteractionMs) {
        while (true) {
            val idle = System.currentTimeMillis() - lastInteractionMs > 130_000L
            emotionEngine.setIdle(idle)
            blinkManager.setSleeping(idle)
            delay(5000)
        }
    }

    val touch = touchLook
    val lookX = ((touch?.first ?: behavior.lookX) + tilt.first * 0.26f).coerceIn(-1f, 1f)
    val lookY = ((touch?.second ?: behavior.lookY) + tilt.second * 0.2f).coerceIn(-1f, 1f)

    val state = EyeState(
        lookX = lookX,
        lookY = lookY,
        eyeOpenAmount = eyeOpen,
        leftOpenMultiplier = leftOpen,
        rightOpenMultiplier = rightOpen,
        pupilScale = behavior.pupilScale,
        eyeStretch = behavior.eyeStretch,
        eyeSquish = behavior.eyeSquish,
        headTilt = behavior.headTilt,
        smile = behavior.smile,
        glow = behavior.glow,
        emotion = behavior.emotion,
        effect = behavior.effect,
        isDimmed = behavior.isDimmed,
        batteryLevel = batteryLevel,
        isCharging = isCharging
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        lastInteractionMs = System.currentTimeMillis()
                        use24Hour = !use24Hour
                        emotionEngine.reportDoubleTap()
                    },
                    onLongPress = {
                        lastInteractionMs = System.currentTimeMillis()
                        showSettings = !showSettings
                        emotionEngine.reportLongPress()
                    },
                    onTap = {
                        lastInteractionMs = System.currentTimeMillis()
                        emotionEngine.reportTap()
                    }
                )
            }
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.6f)
                .onSizeChanged { stageSize = it }
                .pointerInput(stageSize) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val position = event.changes.firstOrNull()?.position ?: continue
                            if (stageSize.width > 0 && stageSize.height > 0) {
                                touchLook = normalizeTouch(position, stageSize)
                                lastInteractionMs = System.currentTimeMillis()
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Eye(state = state, size = 138.dp, isLeft = true)
                Spacer(modifier = Modifier.width(42.dp))
                Eye(state = state, size = 138.dp, isLeft = false)
            }
        }

        Box(modifier = Modifier.fillMaxWidth().weight(0.24f), contentAlignment = Alignment.Center) {
            FlipClock(timeMillis = nowMillis, use24Hour = use24Hour, brightness = clockBrightness * if (behavior.isDimmed) 0.72f else 1f)
        }

        BottomInfo(
            timeMillis = nowMillis,
            weather = weather,
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.16f)
                .padding(bottom = 30.dp)
        )
    }

    if (showSettings) {
        CompanionSettings(
            brightness = clockBrightness,
            use24Hour = use24Hour,
            onBrightness = { clockBrightness = it },
            onToggleTime = { use24Hour = !use24Hour },
            onClose = { showSettings = false }
        )
    }
}

private fun normalizeTouch(position: Offset, size: IntSize): Pair<Float, Float> {
    val x = ((position.x / size.width) * 2f - 1f).coerceIn(-1f, 1f)
    val y = ((position.y / size.height) * 2f - 1f).coerceIn(-1f, 1f)
    return x to y
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun FlipClock(timeMillis: Long, use24Hour: Boolean, brightness: Float) {
    val formatter = remember(use24Hour) {
        SimpleDateFormat(if (use24Hour) "HH:mm" else "hh:mm a", Locale.getDefault())
    }
    val text = formatter.format(Date(timeMillis))
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            (slideInVertically(animationSpec = tween(420)) { height -> height } togetherWith
                slideOutVertically(animationSpec = tween(420)) { height -> -height })
                .using(SizeTransform(clip = false))
        },
        label = "flipClock"
    ) { value ->
        Text(
            text = value,
            color = Color.White.copy(alpha = (0.9f * brightness).coerceIn(0.35f, 1f)),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            fontSize = 48.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun BottomInfo(timeMillis: Long, weather: WeatherSnapshot, modifier: Modifier = Modifier) {
    val dateText = remember(timeMillis / 86_400_000L) {
        SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date(timeMillis))
    }
    val condition = when (weather.icon) {
        "rain" -> "RAIN"
        "cloud" -> "CLOUD"
        else -> "SUN"
    }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dateText,
            color = Color.White.copy(alpha = 0.78f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(18.dp))
        Text(
            text = "$condition ${weather.temperature}C",
            color = Color.White.copy(alpha = 0.72f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CompanionSettings(
    brightness: Float,
    use24Hour: Boolean,
    onBrightness: (Float) -> Unit,
    onToggleTime: () -> Unit,
    onClose: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xEE050505))
                .padding(horizontal = 28.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Companion", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    if (use24Hour) "24H" else "12H",
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.pointerInput(Unit) { detectTapGestures { onToggleTime() } }
                )
            }
            Text("Clock brightness", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
            Slider(value = brightness, onValueChange = onBrightness, valueRange = 0.35f..1f)
            Text(
                "Long press again to close",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 12.sp,
                modifier = Modifier.pointerInput(Unit) { detectTapGestures { onClose() } }
            )
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}
