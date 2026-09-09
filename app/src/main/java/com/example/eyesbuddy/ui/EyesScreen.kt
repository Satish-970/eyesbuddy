package com.example.eyesbuddy.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eyesbuddy.animation.BlinkManager
import com.example.eyesbuddy.animation.EmotionEngine
import com.example.eyesbuddy.domain.quotes.LocalQuoteRepository
import com.example.eyesbuddy.domain.quotes.QuoteScheduler
import com.example.eyesbuddy.data.CompanionBehavior
import com.example.eyesbuddy.data.Emotion
import com.example.eyesbuddy.data.FaceExpression
import com.example.eyesbuddy.data.FaceSenseState
import com.example.eyesbuddy.data.EyeState
import com.example.eyesbuddy.data.WeatherSnapshot
import com.example.eyesbuddy.sensors.BatteryReceiver
import com.example.eyesbuddy.sensors.FaceSenseController
import com.example.eyesbuddy.sensors.MotionSensor
import com.example.eyesbuddy.sensors.GyroscopeSensorSource
import com.example.eyesbuddy.sensors.MotionSensorConsentStore
import com.example.eyesbuddy.weather.WeatherRepository
import com.example.eyesbuddy.weather.LocationPermissionGate
import com.example.eyesbuddy.weather.LocationSource
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.Locale
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

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
    val gyroscope = remember { GyroscopeSensorSource(context) }
    val motionConsentStore = remember { MotionSensorConsentStore(context) }
    val locationPermissionGate = remember { LocationPermissionGate(context) }
    val locationSource = remember { LocationSource(context) }
    val batteryReceiver = remember { BatteryReceiver(context) }
    val weatherRepository = remember { WeatherRepository() }
    val quoteRepository = remember { LocalQuoteRepository() }
    val quoteScheduler = remember { QuoteScheduler() }
    val perimeterColorStore = remember { PerimeterColorStore(context) }
    val faceSenseController = remember { FaceSenseController() }
    var cameraGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        cameraGranted = granted
    }
    var locationGranted by remember { mutableStateOf(locationPermissionGate.isGranted) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        locationGranted = granted
    }
    var motionConsent by remember { mutableStateOf<Boolean?>(null) }

    val behavior by emotionEngine.behavior.collectAsState()
    val eyeOpen by blinkManager.eyeOpenAmount.collectAsState()
    val leftOpen by blinkManager.leftOpenMultiplier.collectAsState()
    val rightOpen by blinkManager.rightOpenMultiplier.collectAsState()
    val tilt by motionSensor.tilt.collectAsState()
    val gyroGaze by gyroscope.gaze.collectAsState()
    val shakePulse by motionSensor.shakePulse.collectAsState()
    val isCharging by batteryReceiver.isCharging.collectAsState()
    val batteryLevel by batteryReceiver.batteryLevel.collectAsState()
    val justConnectedPulse by batteryReceiver.justConnected.collectAsState()
    val justDisconnectedPulse by batteryReceiver.justDisconnected.collectAsState()
    val justFullPulse by batteryReceiver.justFull.collectAsState()
    val weather by weatherRepository.weather.collectAsState()
    val faceState: FaceSenseState by faceSenseController.state.collectAsState()

    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var lastInteractionMs by remember { mutableStateOf(System.currentTimeMillis()) }
    var touchLook by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var stageSize by remember { mutableStateOf(IntSize.Zero) }
    var clockBrightness by remember { mutableFloatStateOf(1f) }
    var use24Hour by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var thoughtBubble by remember { mutableStateOf<String?>(null) }
    var perimeterColor by remember { mutableStateOf(Color(0xFF65E6C2)) }

    DisposableEffect(Unit) {
        motionSensor.start()
        if (gyroscope.isAvailable && motionConsent == true) gyroscope.start()
        batteryReceiver.register()
        onDispose {
            motionSensor.stop()
            gyroscope.stop()
            batteryReceiver.unregister()
        }
    }

    LaunchedEffect(Unit) {
        motionConsentStore.consent.collect { motionConsent = it }
    }

    LaunchedEffect(Unit) {
        perimeterColorStore.color.collect { perimeterColor = Color(it) }
    }

    LaunchedEffect(motionConsent) {
        if (motionConsent == true && gyroscope.isAvailable) gyroscope.start()
        else gyroscope.stop()
    }

    LaunchedEffect(gyroscope.isAvailable, motionConsent) {
        if (gyroscope.isAvailable && motionConsent == null) {
            thoughtBubble = "Motion sensors can help me follow your tilt. Allow?"
        }
    }

    LaunchedEffect(locationGranted) {
        if (!locationGranted) {
            thoughtBubble = "Approximate location helps show local weather."
            delay(1800L)
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    DisposableEffect(cameraGranted) {
        if (cameraGranted) {
            faceSenseController.start()
        } else {
            faceSenseController.stop()
        }
        onDispose { faceSenseController.stop() }
    }

    LaunchedEffect(Unit) {
        if (!cameraGranted) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(cameraGranted) {
        if (!cameraGranted) {
            thoughtBubble = "Camera permission needed for face sensing."
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1000.milliseconds)
        }
    }

    LaunchedEffect(Unit) {
        val location = if (locationGranted) locationSource.lastKnownLocation() else null
        weatherRepository.refresh(location?.latitude, location?.longitude)
        while (true) {
            val updatedLocation = if (locationGranted) locationSource.lastKnownLocation() else null
            weatherRepository.refresh(updatedLocation?.latitude, updatedLocation?.longitude)
            delay(20.minutes)
        }
    }

    LaunchedEffect(Unit) {
        val quotes = quoteRepository.quotes()
        while (true) {
            quoteScheduler.nextIfDue(System.currentTimeMillis(), quotes)?.let { quote ->
                if (thoughtBubble == null) thoughtBubble = quote.text
            }
            delay(60_000L)
        }
    }

    LaunchedEffect(behavior) {
        blinkManager.perform(behavior.blinkHint, behavior.winkSide)
        if (thoughtBubble == null && Random.nextInt(100) < 18) {
            val message = randomThought(behavior = behavior, weather = weather, batteryLevel = batteryLevel, isCharging = isCharging)
            if (message != null) {
                thoughtBubble = message
                delay(2600.milliseconds)
                if (thoughtBubble == message) thoughtBubble = null
            }
        }
    }

    LaunchedEffect(faceState.eventKey) {
        if (cameraGranted && faceState.eventKey > 0L) {
            emotionEngine.reportFaceExpression(faceState.expression, faceState.confidence)
            thoughtBubble = faceState.message.ifBlank { faceThought(faceState.expression) }
            delay((if (faceState.detected) 2500 else 2000).milliseconds)
            if (thoughtBubble == faceState.message || thoughtBubble == faceThought(faceState.expression)) {
                thoughtBubble = null
            }
        }
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
            delay(1500.milliseconds)
            touchLook = null
        }
    }

    LaunchedEffect(lastInteractionMs) {
        while (true) {
            val idle = System.currentTimeMillis() - lastInteractionMs > 130_000L
            emotionEngine.setIdle(idle)
            blinkManager.setSleeping(idle)
            delay(5000.milliseconds)
        }
    }

    val touch = touchLook
    val lookX = ((touch?.first ?: behavior.lookX) + tilt.first * 0.20f + gyroGaze.first * 0.12f).coerceIn(-1f, 1f)
    val lookY = ((touch?.second ?: behavior.lookY) + tilt.second * 0.16f + gyroGaze.second * 0.10f).coerceIn(-1f, 1f)

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

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(moodBackdrop(behavior, weather))
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        val wasIdle = System.currentTimeMillis() - lastInteractionMs > 30_000L
                        lastInteractionMs = System.currentTimeMillis()
                        use24Hour = !use24Hour
                        emotionEngine.reportDoubleTap()
                        thoughtBubble = when {
                            wasIdle -> "You’re back. Double energy."
                            use24Hour -> "24-hour mode unlocked."
                            else -> "Back to the cozy clock."
                        }
                    },
                    onLongPress = {
                        lastInteractionMs = System.currentTimeMillis()
                        showSettings = !showSettings
                        emotionEngine.reportLongPress()
                        thoughtBubble = "Shh... I’m thinking in bubbles."
                    },
                    onTap = {
                        val wasIdle = System.currentTimeMillis() - lastInteractionMs > 30_000L
                        lastInteractionMs = System.currentTimeMillis()
                        emotionEngine.reportTap()
                        thoughtBubble = if (wasIdle) {
                            "You’re back. I missed the vibe."
                        } else {
                            "I see you. Stay with me."
                        }
                    }
                )
            }
            .pointerInput(stageSize) {
                detectDragGestures(
                    onDragStart = { offset ->
                        lastInteractionMs = System.currentTimeMillis()
                        thoughtBubble = "Touch sensed. Drag me around."
                        emotionEngine.reportTouchDragStart()
                        if (stageSize.width > 0 && stageSize.height > 0) {
                            touchLook = normalizeTouch(offset, stageSize)
                        }
                    },
                    onDragEnd = {
                        lastInteractionMs = System.currentTimeMillis()
                        thoughtBubble = "Drop released."
                        emotionEngine.reportTouchDragEnd()
                    },
                    onDragCancel = {
                        lastInteractionMs = System.currentTimeMillis()
                        thoughtBubble = "Touch dropped."
                        emotionEngine.reportTouchDragEnd()
                    }
                ) { change, _ ->
                    if (stageSize.width > 0 && stageSize.height > 0) {
                        touchLook = normalizeTouch(change.position, stageSize)
                        lastInteractionMs = System.currentTimeMillis()
                    }
                    change.consume()
                }
            }
            .systemBarsPadding()
            .navigationBarsPadding()
    ) {
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EyesPanel(
                    state = state,
                    behavior = behavior,
                    thoughtBubble = thoughtBubble,
                    isLandscape = true,
                    modifier = Modifier
                        .weight(1.15f)
                        .fillMaxSize()
                        .onSizeChanged { stageSize = it }
                        .pointerInput(stageSize) {
                            detectTapGestures { position ->
                                if (stageSize.width > 0 && stageSize.height > 0) {
                                    touchLook = normalizeTouch(position, stageSize)
                                    lastInteractionMs = System.currentTimeMillis()
                                }
                            }
                        }
                )

                Column(
                    modifier = Modifier
                        .weight(0.95f)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ClockCard(
                        timeMillis = nowMillis,
                        use24Hour = use24Hour,
                        brightness = clockBrightness * if (behavior.isDimmed) 0.72f else 1f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    WeatherCard(
                        weather = weather,
                        timeMillis = nowMillis,
                        modifier = Modifier.fillMaxWidth()
                    )
                    MoodSummary(behavior = behavior, modifier = Modifier.fillMaxWidth())
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                EyesPanel(
                    state = state,
                    behavior = behavior,
                    thoughtBubble = thoughtBubble,
                    isLandscape = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.1f)
                        .onSizeChanged { stageSize = it }
                        .pointerInput(stageSize) {
                            detectTapGestures { position ->
                                if (stageSize.width > 0 && stageSize.height > 0) {
                                    touchLook = normalizeTouch(position, stageSize)
                                    lastInteractionMs = System.currentTimeMillis()
                                }
                            }
                        }
                )

                ClockCard(
                    timeMillis = nowMillis,
                    use24Hour = use24Hour,
                    brightness = clockBrightness * if (behavior.isDimmed) 0.72f else 1f,
                    modifier = Modifier.fillMaxWidth()
                )

                WeatherCard(
                    weather = weather,
                    timeMillis = nowMillis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        PerimeterCountdownOverlay(color = perimeterColor)
    }

    if (gyroscope.isAvailable && motionConsent == null) {
        MotionConsentDialog(
            onAllow = { scope.launch { motionConsentStore.setConsent(true) } },
            onNotNow = { scope.launch { motionConsentStore.setConsent(false) } }
        )
    }

    if (showSettings) {
        CompanionSettings(
            brightness = clockBrightness,
            use24Hour = use24Hour,
            onBrightness = { clockBrightness = it },
            onToggleTime = { use24Hour = !use24Hour },
            onPerimeterColor = {
                perimeterColor = it
                scope.launch { perimeterColorStore.setColor(it.toArgb()) }
            },
            onRevokeMotion = { scope.launch { motionConsentStore.setConsent(false) } },
            onClose = { showSettings = false }
        )
    }
}

private fun normalizeTouch(position: Offset, size: IntSize): Pair<Float, Float> {
    val x = ((position.x / size.width) * 2f - 1f).coerceIn(-1f, 1f)
    val y = ((position.y / size.height) * 2f - 1f).coerceIn(-1f, 1f)
    return x to y
}

@Composable
private fun EyesPanel(
    state: EyeState,
    behavior: CompanionBehavior,
    thoughtBubble: String?,
    isLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val eyeSize = if (isLandscape) 152.dp else 160.dp
        val gap = if (isLandscape) 18.dp else 24.dp

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            ThoughtBubble(
                text = thoughtBubble ?: moodText(behavior),
                emphasized = thoughtBubble != null
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Eye(state = state, size = eyeSize, isLeft = true)
                Spacer(modifier = Modifier.width(gap))
                Eye(state = state, size = eyeSize, isLeft = false)
            }
        }
    }
}

@Composable
private fun ClockCard(
    timeMillis: Long,
    use24Hour: Boolean,
    brightness: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        color = Color(0xFF0D0F13).copy(alpha = 0.94f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FlipClock(
                timeMillis = timeMillis,
                use24Hour = use24Hour,
                brightness = brightness,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "tap for curiosity · double tap toggles 12/24H",
                color = Color.White.copy(alpha = 0.44f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun WeatherCard(
    weather: WeatherSnapshot,
    timeMillis: Long,
    modifier: Modifier = Modifier
) {
    val icon = when (weather.icon) {
        "rain" -> "☔"
        "storm" -> "⚡"
        "snow" -> "❄"
        "fog" -> "〰"
        "cloud" -> "☁"
        else -> "☀"
    }
    val updatedText = remember(weather.updatedAtMillis) {
        if (weather.updatedAtMillis == 0L) {
            "Waiting for live weather"
        } else {
            "Updated ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(weather.updatedAtMillis))}"
        }
    }

    Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        color = Color(0xFF0B0D11).copy(alpha = 0.92f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(icon, fontSize = 24.sp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = weather.locationName,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        text = weather.description,
                        color = Color.White.copy(alpha = 0.62f),
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
                Text(
                    text = "${weather.temperature}°",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = weather.condition.uppercase(Locale.getDefault()),
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 12.sp,
                    style = TextStyle(letterSpacing = 1.sp)
                )
                Text(
                    text = "Feels like ${weather.feelsLike}°",
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 12.sp
                )
            }

            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = updatedText,
                    color = Color.White.copy(alpha = 0.46f),
                    fontSize = 12.sp
                )
                Text(
                    text = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(timeMillis)),
                    color = Color.White.copy(alpha = 0.46f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun ThoughtBubble(text: String, emphasized: Boolean) {
    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
        color = Color(0xFF11141B).copy(alpha = if (emphasized) 0.98f else 0.82f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = if (emphasized) 0.16f else 0.08f))
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = if (emphasized) 0.95f else 0.72f),
            fontSize = if (emphasized) 13.sp else 12.sp,
            fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
        )
    }
}

@Composable
private fun MoodSummary(
    behavior: CompanionBehavior,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
        color = Color(0xFF0B0D11).copy(alpha = 0.9f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = moodText(behavior),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = behavior.action.name.lowercase(Locale.getDefault()).replace('_', ' '),
                    color = Color.White.copy(alpha = 0.52f),
                    fontSize = 12.sp
                )
            }
            Text(
                text = if (behavior.isDimmed) "sleep mode" else "awake",
                color = Color.White.copy(alpha = 0.46f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun FlipClock(
    timeMillis: Long,
    use24Hour: Boolean,
    brightness: Float,
    modifier: Modifier = Modifier
) {
    val parts = remember(timeMillis, use24Hour) { clockParts(timeMillis, use24Hour) }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ClockDigits(parts.hour, brightness)
        ClockSeparator(brightness)
        ClockDigits(parts.minute, brightness)
        ClockSeparator(brightness)
        ClockDigits(parts.second, brightness)
        if (parts.suffix != null) {
            Spacer(modifier = Modifier.width(10.dp))
            AmPmChip(parts.suffix, brightness)
        }
    }
}

@Composable
private fun ClockDigits(value: String, brightness: Float) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        value.forEach { digit ->
            FlipDigitTile(digit = digit, brightness = brightness)
        }
    }
}

@Composable
private fun FlipDigitTile(digit: Char, brightness: Float) {
    Surface(
        modifier = Modifier.heightIn(min = 54.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        color = Color(0xFF171A20).copy(alpha = (0.92f * brightness).coerceIn(0.45f, 1f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        FlipDigit(value = digit, brightness = brightness)
    }
}

@Composable
private fun ClockSeparator(brightness: Float) {
    Text(
        text = ":",
        color = Color.White.copy(alpha = (0.9f * brightness).coerceIn(0.45f, 1f)),
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        modifier = Modifier.padding(horizontal = 1.dp)
    )
}

@Composable
private fun AmPmChip(suffix: String, brightness: Float) {
    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
        color = Color.White.copy(alpha = (0.1f * brightness).coerceIn(0.05f, 0.2f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Text(
            text = suffix,
            color = Color.White.copy(alpha = (0.84f * brightness).coerceIn(0.4f, 1f)),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

private data class ClockParts(
    val hour: String,
    val minute: String,
    val second: String,
    val suffix: String?
)

private fun clockParts(timeMillis: Long, use24Hour: Boolean): ClockParts {
    val calendar = Calendar.getInstance().apply { timeInMillis = timeMillis }
    val hour24 = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)
    val second = calendar.get(Calendar.SECOND)
    val hour = if (use24Hour) {
        hour24
    } else {
        val hour12 = calendar.get(Calendar.HOUR)
        if (hour12 == 0) 12 else hour12
    }
    val suffix = if (use24Hour) null else if (calendar.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
    return ClockParts(
        hour = hour.toString().padStart(2, '0'),
        minute = minute.toString().padStart(2, '0'),
        second = second.toString().padStart(2, '0'),
        suffix = suffix
    )
}

private fun moodText(behavior: CompanionBehavior): String {
    val emotion = when (behavior.emotion) {
        Emotion.HAPPY -> "happy"
        Emotion.CURIOUS -> "curious"
        Emotion.SLEEPY -> "sleepy"
        Emotion.PLAYFUL -> "playful"
        Emotion.THINKING -> "thinking"
        Emotion.SHY -> "shy"
        Emotion.EXCITED -> "excited"
        Emotion.RELAXED -> "relaxed"
        Emotion.SURPRISED -> "surprised"
        Emotion.SCARED -> "alert"
        Emotion.EMBARRASSED -> "embarrassed"
        Emotion.CHARGING -> "charging"
        Emotion.FULL_BATTERY -> "full"
        Emotion.LOW_BATTERY -> "low battery"
    }
    return "Mood: $emotion"
}

private fun faceThought(expression: FaceExpression): String = when (expression) {
    FaceExpression.HAPPY -> "You’re back and smiling."
    FaceExpression.CURIOUS -> "Curious eyes. I noticed."
    FaceExpression.SLEEPY -> "Sleepy face, soft pace."
    FaceExpression.SURPRISED -> "Surprise detected."
    FaceExpression.PLAYFUL -> "Playful energy."
    FaceExpression.SHY -> "A shy hello."
    FaceExpression.NEUTRAL -> "I can see you."
    FaceExpression.LOST -> "Where’d you go?"
    FaceExpression.UNKNOWN -> "Looking for you..."
}

private fun randomThought(
    behavior: CompanionBehavior,
    weather: WeatherSnapshot,
    batteryLevel: Int,
    isCharging: Boolean
): String? {
    val moods = listOf(
        "You’re back. I was waiting.",
        "Little steps still move forward.",
        "Blink, breathe, continue.",
        "You’re doing better than you think.",
        "Tiny progress is still progress.",
        "Stay soft. Keep going.",
        "One more try."
    )
    val weatherLines = when (weather.icon) {
        "rain" -> listOf("Rain outside, glow inside.", "Soft weather, strong heart.")
        "storm" -> listOf("Stormy skies, steady focus.", "Ride the thunder, keep calm.")
        "snow" -> listOf("Cold air, warm momentum.", "Snow day energy: quiet and cool.")
        "fog" -> listOf("Foggy view, clear intent.", "Keep moving through the mist.")
        else -> listOf("Sunny mood, steady pace.", "Bright skies, brighter mind.")
    }
    val batteryLines = when {
        isCharging -> listOf("Charging up your energy.", "Plugged in and glowing.")
        batteryLevel <= 20 -> listOf("Low battery, high spirit.", "Save power, save yourself.")
        else -> emptyList()
    }
    val emotionLines = when (behavior.emotion) {
        Emotion.SLEEPY -> listOf("We can slow down together.", "Rest is part of progress.")
        Emotion.CURIOUS -> listOf("Curiosity looks good on you.", "Let’s explore a little more.")
        Emotion.HAPPY -> listOf("Good vibes detected.", "That smile suits the room.")
        Emotion.THINKING -> listOf("Thinking mode: engaged.", "Your next idea is close.")
        Emotion.SHY, Emotion.EMBARRASSED -> listOf("No pressure. Just vibes.", "It’s okay to be a little shy.")
        Emotion.EXCITED -> listOf("Big energy, small steps.", "You’re lit up right now.")
        Emotion.RELAXED -> listOf("Soft mode activated.", "Calm is a superpower.")
        Emotion.SURPRISED, Emotion.SCARED -> listOf("Stay with me, you’re safe.", "We’ve got this.")
        Emotion.CHARGING -> listOf("Refilling the battery.", "Energy rising.")
        Emotion.FULL_BATTERY -> listOf("Fully charged and ready.", "Maximum glow unlocked.")
        Emotion.LOW_BATTERY -> listOf("Power saving, but still here.", "A little tired, still strong.")
        Emotion.PLAYFUL -> listOf("Play mode: on.", "Let’s keep it fun.")
    }
    return (emotionLines + batteryLines + weatherLines + moods).randomOrNull()
}

private fun moodBackdrop(behavior: CompanionBehavior, weather: WeatherSnapshot): Brush {
    val top = when (behavior.emotion) {
        Emotion.EXCITED, Emotion.FULL_BATTERY -> Color(0xFF182033)
        Emotion.CURIOUS, Emotion.THINKING -> Color(0xFF141824)
        Emotion.SLEEPY, Emotion.LOW_BATTERY -> Color(0xFF101116)
        Emotion.SHY, Emotion.EMBARRASSED -> Color(0xFF1A1119)
        Emotion.CHARGING -> Color(0xFF0F2017)
        Emotion.SURPRISED, Emotion.SCARED -> Color(0xFF1B1212)
        Emotion.HAPPY, Emotion.PLAYFUL -> Color(0xFF151C14)
        Emotion.RELAXED -> Color(0xFF10131B)
    }
    val middle = when (weather.icon) {
        "rain" -> Color(0xFF0E1B2B)
        "storm" -> Color(0xFF1A1728)
        "snow" -> Color(0xFF16212A)
        "fog" -> Color(0xFF191A1F)
        "cloud" -> Color(0xFF12151D)
        else -> Color.Black
    }
    return Brush.verticalGradient(
        colors = listOf(top, middle, Color.Black)
    )
}

@Composable
private fun CompanionSettings(
    brightness: Float,
    use24Hour: Boolean,
    onBrightness: (Float) -> Unit,
    onToggleTime: () -> Unit,
    onPerimeterColor: (Color) -> Unit,
    onRevokeMotion: () -> Unit,
    onClose: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(red = 0.02f, green = 0.02f, blue = 0.02f, alpha = 0.93f))
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
            Text("Perimeter color", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(Color(0xFF65E6C2), Color(0xFFFFD166), Color(0xFFFF7A90), Color(0xFF7CB7FF)).forEach { color ->
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(28.dp)
                            .background(color)
                            .pointerInput(color) { detectTapGestures { onPerimeterColor(color) } }
                    )
                }
            }
            Text(
                "Disable motion reactions",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                modifier = Modifier.pointerInput(Unit) { detectTapGestures { onRevokeMotion() } }
            )
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

@Composable
private fun MotionConsentDialog(onAllow: () -> Unit, onNotNow: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onNotNow,
        title = { Text("A little motion?", color = Color.White) },
        text = { Text("EyesBuddy would like to use motion sensors to react to how you tilt and move your device. Allow?", color = Color.White.copy(alpha = 0.78f)) },
        confirmButton = {
            Text("Allow", color = Color(0xFF65E6C2), modifier = Modifier.pointerInput(Unit) { detectTapGestures { onAllow() } }.padding(8.dp))
        },
        dismissButton = {
            Text("Not now", color = Color.White.copy(alpha = 0.7f), modifier = Modifier.pointerInput(Unit) { detectTapGestures { onNotNow() } }.padding(8.dp))
        },
        containerColor = Color(0xFF15171C)
    )
}
