package com.example.eyesbuddy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.eyesbuddy.animation.BlinkManager
import com.example.eyesbuddy.animation.EmotionEngine
import com.example.eyesbuddy.data.Emotion
import com.example.eyesbuddy.data.EyeState
import com.example.eyesbuddy.sensors.BatteryReceiver
import com.example.eyesbuddy.sensors.MotionSensor
import kotlinx.coroutines.delay

/**
 * Wires together every piece described in the architecture:
 *  - BlinkManager for natural blinking + sleep
 *  - MotionSensor for tilt-based look direction + shake -> Surprised
 *  - BatteryReceiver for charging -> Happy/Excited, low battery -> Sleepy
 *  - EmotionEngine as the single source of truth for current Emotion
 *  - tap gestures for Curious / Angry
 *
 * Also implements the two battery-saving behaviours from the spec:
 *  - after INACTIVITY_TIMEOUT_MS of no touch, eyes go to sleep (BlinkManager.setSleeping)
 *  - render throttling is naturally cheap here since Compose only redraws on
 *    state change; the flows above already emit at a low, sensor-driven rate.
 */
@Composable
fun EyesScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val blinkManager = remember { BlinkManager(scope).apply { start() } }
    val emotionEngine = remember { EmotionEngine(scope) }
    val motionSensor = remember { MotionSensor(context) }
    val batteryReceiver = remember { BatteryReceiver(context) }

    val eyeOpen by blinkManager.eyeOpenAmount.collectAsState(initial = 1f)
    val emotion by emotionEngine.emotion.collectAsState(initial = Emotion.NEUTRAL)
    val tilt by motionSensor.tilt.collectAsState(initial = 0f to 0f)
    val shakePulse by motionSensor.shakePulse.collectAsState(initial = 0L)
    val isCharging by batteryReceiver.isCharging.collectAsState(initial = false)
    val batteryLevel by batteryReceiver.batteryLevel.collectAsState(initial = 100)
    val justConnectedPulse by batteryReceiver.justConnected.collectAsState(initial = 0L)

    var lastInteractionMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val isAsleep = remember { mutableStateOf(false) }

    val inactivityTimeoutMs = 120_000L // 2 minutes idle -> sleep

    // Lifecycle: start/stop sensors + receiver alongside this composable.
    DisposableEffect(Unit) {
        motionSensor.start()
        batteryReceiver.register()
        onDispose {
            motionSensor.stop()
            batteryReceiver.unregister()
        }
    }

    // Feed charging + battery level into the emotion engine.
    LaunchedEffect(isCharging) { emotionEngine.setCharging(isCharging) }
    LaunchedEffect(batteryLevel) { emotionEngine.setBatteryLevel(batteryLevel) }
    LaunchedEffect(justConnectedPulse) {
        if (justConnectedPulse > 0) {
            emotionEngine.reportChargerJustConnected()
            wake(blinkManager) { lastInteractionMs = System.currentTimeMillis(); isAsleep.value = false }
        }
    }
    LaunchedEffect(shakePulse) {
        if (shakePulse > 0) {
            emotionEngine.reportShake()
            wake(blinkManager) { lastInteractionMs = System.currentTimeMillis(); isAsleep.value = false }
        }
    }

    // Idle watchdog: checks periodically rather than using a heavy timer per-frame.
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            val idleFor = System.currentTimeMillis() - lastInteractionMs
            val shouldSleep = idleFor > inactivityTimeoutMs
            if (shouldSleep != isAsleep.value) {
                isAsleep.value = shouldSleep
                blinkManager.setSleeping(shouldSleep)
                emotionEngine.setIdle(shouldSleep)
            }
        }
    }

    val state = EyeState(
        lookX = if (isAsleep.value) 0f else tilt.first,
        lookY = if (isAsleep.value) 0f else tilt.second,
        eyeOpenAmount = eyeOpen,
        pupilScale = if (emotion == Emotion.SURPRISED) 1.3f else 1f,
        emotion = emotion,
        isAsleep = isAsleep.value,
        batteryLevel = batteryLevel,
        isCharging = isCharging
    )

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // pure black background - OLED power saving
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        lastInteractionMs = System.currentTimeMillis()
                        if (isAsleep.value) {
                            isAsleep.value = false
                            blinkManager.setSleeping(false)
                            emotionEngine.setIdle(false)
                            blinkManager.blinkOnce()
                        }
                        emotionEngine.reportTap()
                    }
                )
            },
        horizontalArrangement = Arrangement.Center
    ) {
        Eye(state = state, size = 96.dp)
        Spacer(modifier = Modifier.width(48.dp))
        Eye(state = state, size = 96.dp)
    }
}

private fun wake(blinkManager: BlinkManager, onWoken: () -> Unit) {
    blinkManager.setSleeping(false)
    blinkManager.blinkOnce()
    onWoken()
}
