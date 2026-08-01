package com.example.eyesbuddy.animation

import com.example.eyesbuddy.data.Emotion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Central decision-maker for which Emotion is shown.
 * Call the report* functions from EyesScreen whenever a relevant event
 * happens; this class handles priority + auto-decay back to NEUTRAL/SLEEPY.
 *
 * Priority (highest first): ANGRY > SURPRISED > EXCITED > HAPPY > CURIOUS > SLEEPY > NEUTRAL
 */
class EmotionEngine(private val scope: CoroutineScope) {

    private val _emotion = MutableStateFlow(Emotion.NEUTRAL)
    val emotion: StateFlow<Emotion> = _emotion

    private var tapTimestamps = mutableListOf<Long>()
    private var isCharging = false
    private var isIdle = false
    private var batteryLevel = 100

    private var transientJob: kotlinx.coroutines.Job? = null

    fun reportTap() {
        val now = System.currentTimeMillis()
        tapTimestamps.add(now)
        tapTimestamps = tapTimestamps.filter { now - it < 1500 }.toMutableList()

        if (tapTimestamps.size >= 4) {
            showTransient(Emotion.ANGRY, 2000)
        } else {
            showTransient(Emotion.CURIOUS, 1200)
        }
    }

    fun reportShake() {
        showTransient(Emotion.SURPRISED, 1500)
    }

    fun reportChargerJustConnected() {
        showTransient(Emotion.EXCITED, 2000)
    }

    fun setCharging(charging: Boolean) {
        isCharging = charging
        recomputeBaseline()
    }

    fun setIdle(idle: Boolean) {
        isIdle = idle
        recomputeBaseline()
    }

    fun setBatteryLevel(level: Int) {
        batteryLevel = level
        recomputeBaseline()
    }

    /** Shows an emotion immediately, then falls back to the baseline after [durationMs]. */
    private fun showTransient(e: Emotion, durationMs: Long) {
        transientJob?.cancel()
        _emotion.value = e
        transientJob = scope.launch {
            delay(durationMs)
            recomputeBaseline()
        }
    }

    /** The "resting" emotion when nothing transient is overriding it. */
    private fun recomputeBaseline() {
        _emotion.value = when {
            isCharging -> Emotion.HAPPY
            isIdle -> Emotion.SLEEPY
            batteryLevel in 1..15 -> Emotion.SLEEPY
            else -> Emotion.NEUTRAL
        }
    }
}
