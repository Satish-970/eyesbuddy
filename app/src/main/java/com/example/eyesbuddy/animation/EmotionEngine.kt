@file:Suppress("unused")

package com.example.eyesbuddy.animation

import com.example.eyesbuddy.data.BlinkHint
import com.example.eyesbuddy.data.CompanionAction
import com.example.eyesbuddy.data.CompanionBehavior
import com.example.eyesbuddy.data.CompanionEffect
import com.example.eyesbuddy.data.FaceExpression
import com.example.eyesbuddy.data.Emotion
import com.example.eyesbuddy.data.WinkSide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

/**
 * Finite-state personality engine.
 * Every second it observes context, chooses a weighted action, and emits a new
 * behavior snapshot. Event methods briefly override the baseline for touch,
 * shake, charge, and full-battery reactions.
 */
class EmotionEngine(private val scope: CoroutineScope) {

    private val _behavior = MutableStateFlow(CompanionBehavior())
    val behavior: StateFlow<CompanionBehavior> = _behavior

    private var isCharging = false
    private var isIdle = false
    private var batteryLevel = 100
    private var transientJob: Job? = null
    private var loopJob: Job? = null

    init {
        startLoop()
    }

    fun reportTap() {
        showTransient(
            behaviorFor(CompanionAction.CURIOUS, Emotion.CURIOUS).copy(
                blinkHint = BlinkHint.WINK,
                winkSide = if (Random.nextBoolean()) WinkSide.LEFT else WinkSide.RIGHT
            ),
            1100
        )
    }

    fun reportDoubleTap() {
        showTransient(
            behaviorFor(CompanionAction.SMILE, Emotion.PLAYFUL).copy(
                blinkHint = BlinkHint.WINK,
                winkSide = WinkSide.RIGHT,
                smile = 1f
            ),
            1300
        )
    }

    fun reportLongPress() {
        showTransient(
            behaviorFor(CompanionAction.PEEK, Emotion.SHY).copy(
                lookY = 0.35f,
                glow = 0.36f,
                blinkHint = BlinkHint.HALF_BLINK
            ),
            1800
        )
    }

    fun reportShake() {
        showTransient(
            behaviorFor(CompanionAction.REACT, Emotion.SURPRISED).copy(
                effect = CompanionEffect.DIZZY,
                pupilScale = 1.35f,
                blinkHint = BlinkHint.DOUBLE_BLINK
            ),
            1700
        )
    }

    fun reportTouchDragStart() {
        showTransient(
            behaviorFor(CompanionAction.CURIOUS, Emotion.CURIOUS).copy(
                blinkHint = BlinkHint.BLINK,
                glow = 0.8f,
                smile = 0.6f
            ),
            1200
        )
    }

    fun reportTouchDragEnd() {
        showTransient(
            behaviorFor(CompanionAction.OBSERVE, Emotion.RELAXED).copy(
                blinkHint = BlinkHint.SLOW_BLINK,
                glow = 0.55f
            ),
            900
        )
    }

    fun reportFaceExpression(expression: FaceExpression, confidence: Float = 0.7f) {
        val behavior = when (expression) {
            FaceExpression.HAPPY -> behaviorFor(CompanionAction.SMILE, Emotion.HAPPY).copy(
                smile = 1f,
                glow = 0.9f,
                blinkHint = BlinkHint.BLINK
            )
            FaceExpression.PLAYFUL -> behaviorFor(CompanionAction.STRETCH, Emotion.PLAYFUL).copy(
                smile = 0.9f,
                glow = 0.95f,
                effect = CompanionEffect.SPARKLES
            )
            FaceExpression.CURIOUS -> behaviorFor(CompanionAction.CURIOUS, Emotion.CURIOUS).copy(
                glow = 0.75f
            )
            FaceExpression.SLEEPY -> behaviorFor(CompanionAction.SLEEPY, Emotion.SLEEPY).copy(
                blinkHint = BlinkHint.SLOW_BLINK,
                glow = 0.3f,
                eyeSquish = 0.68f
            )
            FaceExpression.SURPRISED -> behaviorFor(CompanionAction.REACT, Emotion.SURPRISED).copy(
                pupilScale = 1.28f,
                glow = 0.9f,
                blinkHint = BlinkHint.DOUBLE_BLINK
            )
            FaceExpression.SHY -> behaviorFor(CompanionAction.PEEK, Emotion.SHY).copy(
                glow = 0.42f,
                smile = 0.22f
            )
            FaceExpression.NEUTRAL -> behaviorFor(CompanionAction.OBSERVE, baselineEmotion()).copy(
                glow = 0.55f * confidence.coerceIn(0.45f, 1f)
            )
            FaceExpression.LOST, FaceExpression.UNKNOWN -> behaviorFor(CompanionAction.OBSERVE, Emotion.RELAXED)
        }

        showTransient(behavior, if (expression == FaceExpression.SLEEPY) 1800 else 1300)
    }


    fun reportChargerJustConnected() {
        showTransient(
            behaviorFor(CompanionAction.CELEBRATE, Emotion.CHARGING).copy(
                effect = CompanionEffect.SPARKLES,
                blinkHint = BlinkHint.SLOW_BLINK,
                smile = 1f,
                glow = 0.95f
            ),
            2600
        )
    }

    fun reportChargerDisconnected() {
        showTransient(
            behaviorFor(CompanionAction.REACT, Emotion.SURPRISED).copy(
                lookX = -0.55f,
                blinkHint = BlinkHint.DOUBLE_BLINK
            ),
            1600
        )
    }

    fun reportBatteryFull() {
        showTransient(
            behaviorFor(CompanionAction.CELEBRATE, Emotion.FULL_BATTERY).copy(
                effect = CompanionEffect.SPARKLES,
                smile = 1f,
                glow = 1f
            ),
            2400
        )
    }

    fun setCharging(charging: Boolean) {
        isCharging = charging
    }

    fun setIdle(idle: Boolean) {
        isIdle = idle
    }

    fun setBatteryLevel(level: Int) {
        batteryLevel = level.coerceIn(0, 100)
    }

    private fun startLoop() {
        loopJob?.cancel()
        loopJob = scope.launch {
            while (true) {
                delay(1000.milliseconds)
                if (transientJob?.isActive != true) {
                    _behavior.value = chooseNextBehavior()
                }
            }
        }
    }

    private fun showTransient(behavior: CompanionBehavior, durationMs: Long) {
        transientJob?.cancel()
        _behavior.value = behavior
        transientJob = scope.launch {
            delay(durationMs.milliseconds)
            _behavior.value = chooseNextBehavior()
        }
    }

    private fun chooseNextBehavior(): CompanionBehavior {
        val baseline = baselineEmotion()
        val weightedActions = buildList {
            add(CompanionAction.OBSERVE to 16)
            add(CompanionAction.LOOK_LEFT to 9)
            add(CompanionAction.LOOK_RIGHT to 9)
            add(CompanionAction.LOOK_UP to 6)
            add(CompanionAction.LOOK_DOWN to 6)
            add(CompanionAction.BLINK to 9)
            add(CompanionAction.DOUBLE_BLINK to 4)
            add(CompanionAction.SMILE to 7)
            add(CompanionAction.CURIOUS to 8)
            add(CompanionAction.THINK to 6)
            add(CompanionAction.PEEK to 4)
            add(CompanionAction.STRETCH to 4)
            add(CompanionAction.SLEEPY to if (baseline == Emotion.SLEEPY) 14 else 3)
            add(CompanionAction.YAWN to if (baseline == Emotion.SLEEPY) 10 else 1)
            add(CompanionAction.CELEBRATE to if (isCharging) 4 else 1)
            add(CompanionAction.REACT to 3)
        }
        val action = weightedPick(weightedActions)
        return behaviorFor(action, baseline).copy(isDimmed = isLateNight())
    }

    private fun baselineEmotion(): Emotion {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            batteryLevel <= 15 -> Emotion.LOW_BATTERY
            batteryLevel >= 100 && isCharging -> Emotion.FULL_BATTERY
            isCharging -> Emotion.CHARGING
            isIdle -> Emotion.SLEEPY
            hour in 5..10 -> Emotion.EXCITED
            hour in 11..16 -> Emotion.CURIOUS
            hour in 17..21 -> Emotion.RELAXED
            else -> Emotion.SLEEPY
        }
    }

    private fun behaviorFor(action: CompanionAction, baseline: Emotion): CompanionBehavior {
        val emotion = when (action) {
            CompanionAction.CURIOUS, CompanionAction.PEEK -> Emotion.CURIOUS
            CompanionAction.THINK -> Emotion.THINKING
            CompanionAction.SMILE -> if (baseline == Emotion.CHARGING) Emotion.CHARGING else Emotion.HAPPY
            CompanionAction.STRETCH -> Emotion.PLAYFUL
            CompanionAction.SLEEPY, CompanionAction.YAWN -> Emotion.SLEEPY
            CompanionAction.CELEBRATE -> if (batteryLevel >= 100 && isCharging) Emotion.FULL_BATTERY else Emotion.EXCITED
            CompanionAction.REACT -> Emotion.SURPRISED
            else -> baseline
        }

        val look = when (action) {
            CompanionAction.LOOK_LEFT -> -0.75f to Random.nextFloatIn(-0.15f, 0.2f)
            CompanionAction.LOOK_RIGHT -> 0.75f to Random.nextFloatIn(-0.15f, 0.2f)
            CompanionAction.LOOK_UP -> Random.nextFloatIn(-0.18f, 0.18f) to -0.62f
            CompanionAction.LOOK_DOWN -> Random.nextFloatIn(-0.18f, 0.18f) to 0.62f
            CompanionAction.PEEK -> Random.nextFloatIn(-0.72f, 0.72f) to 0.34f
            CompanionAction.THINK -> -0.32f to -0.25f
            CompanionAction.REACT -> Random.nextFloatIn(-0.9f, 0.9f) to Random.nextFloatIn(-0.45f, 0.45f)
            else -> Random.nextFloatIn(-0.25f, 0.25f) to Random.nextFloatIn(-0.16f, 0.16f)
        }

        val blink = when (action) {
            CompanionAction.BLINK -> BlinkHint.BLINK
            CompanionAction.DOUBLE_BLINK -> BlinkHint.DOUBLE_BLINK
            CompanionAction.YAWN, CompanionAction.SLEEPY -> BlinkHint.SLOW_BLINK
            CompanionAction.PEEK -> BlinkHint.HALF_BLINK
            else -> BlinkHint.NATURAL
        }

        val profile = profileFor(emotion)
        return CompanionBehavior(
            emotion = emotion,
            action = action,
            lookX = look.first,
            lookY = look.second,
            pupilScale = profile.pupilScale * when (action) {
                CompanionAction.REACT -> 1.25f
                CompanionAction.THINK -> 0.9f
                else -> 1f
            },
            eyeStretch = profile.eyeStretch * if (action == CompanionAction.STRETCH) 1.18f else 1f,
            eyeSquish = profile.eyeSquish * if (action == CompanionAction.YAWN) 0.72f else 1f,
            headTilt = when (action) {
                CompanionAction.THINK -> -0.12f
                CompanionAction.PEEK -> 0.1f
                CompanionAction.STRETCH -> Random.nextFloatIn(-0.16f, 0.16f)
                else -> Random.nextFloatIn(-0.06f, 0.06f)
            },
            smile = profile.smile,
            glow = profile.glow,
            effect = if (action == CompanionAction.CELEBRATE) CompanionEffect.SPARKLES else CompanionEffect.NONE,
            blinkHint = blink
        )
    }

    private fun profileFor(emotion: Emotion): EmotionProfile = when (emotion) {
        Emotion.HAPPY -> EmotionProfile(1f, 1.03f, 1f, 0.65f, 0.65f)
        Emotion.CURIOUS -> EmotionProfile(1.08f, 1.06f, 1f, 0.25f, 0.72f)
        Emotion.SLEEPY -> EmotionProfile(0.76f, 0.94f, 0.72f, 0.05f, 0.32f)
        Emotion.PLAYFUL -> EmotionProfile(1.1f, 1.12f, 0.96f, 0.8f, 0.82f)
        Emotion.THINKING -> EmotionProfile(0.88f, 0.98f, 0.82f, 0.0f, 0.46f)
        Emotion.SHY -> EmotionProfile(0.9f, 0.92f, 0.82f, 0.18f, 0.36f)
        Emotion.EXCITED -> EmotionProfile(1.24f, 1.16f, 1.08f, 1f, 0.95f)
        Emotion.RELAXED -> EmotionProfile(0.96f, 1f, 0.92f, 0.18f, 0.52f)
        Emotion.SURPRISED -> EmotionProfile(1.36f, 1.04f, 1.18f, 0.0f, 0.86f)
        Emotion.SCARED -> EmotionProfile(1.24f, 0.92f, 1.15f, 0.0f, 0.72f)
        Emotion.EMBARRASSED -> EmotionProfile(0.86f, 0.9f, 0.82f, 0.25f, 0.42f)
        Emotion.CHARGING -> EmotionProfile(1.08f, 1.08f, 1f, 0.72f, 0.85f)
        Emotion.FULL_BATTERY -> EmotionProfile(1.18f, 1.12f, 1.05f, 1f, 1f)
        Emotion.LOW_BATTERY -> EmotionProfile(0.68f, 0.88f, 0.76f, 0.0f, 0.28f)
    }

    private fun weightedPick(items: List<Pair<CompanionAction, Int>>): CompanionAction {
        val total = items.sumOf { it.second }
        var cursor = Random.nextInt(total)
        for ((action, weight) in items) {
            cursor -= weight
            if (cursor < 0) return action
        }
        return CompanionAction.OBSERVE
    }

    private fun isLateNight(): Boolean = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) in 0..4

    private data class EmotionProfile(
        val pupilScale: Float,
        val eyeStretch: Float,
        val eyeSquish: Float,
        val smile: Float,
        val glow: Float
    )
}

private fun Random.nextFloatIn(min: Float, max: Float): Float =
    min + (nextFloat() * ((max - min) * 1000f).roundToInt() / 1000f)
