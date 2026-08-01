package com.example.eyesbuddy.animation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Drives natural, involuntary blinking.
 * - Blinks every 3-8 seconds (randomised so it never feels mechanical).
 * - Each blink is a quick close/open (~120ms close, ~110ms open).
 * - Exposes eyeOpenAmount as a StateFlow<Float> for the UI to render.
 */
class BlinkManager(private val scope: CoroutineScope) {

    private val _eyeOpenAmount = MutableStateFlow(1f)
    val eyeOpenAmount: StateFlow<Float> = _eyeOpenAmount

    private var forcedClosed = false // used for sleep state, overrides blinking

    fun start() {
        scope.launch {
            while (true) {
                val nextBlinkDelay = Random.nextLong(3000L, 8000L)
                delay(nextBlinkDelay)
                if (!forcedClosed) {
                    doBlink()
                }
            }
        }
    }

    /** Force the eyes fully shut (sleep) or release them back to normal blinking. */
    fun setSleeping(sleeping: Boolean) {
        forcedClosed = sleeping
        if (sleeping) {
            scope.launch { animateTo(0f, 400) }
        } else {
            scope.launch { animateTo(1f, 250) }
        }
    }

    private suspend fun doBlink() {
        animateTo(0f, 120)
        animateTo(1f, 110)
    }

    private suspend fun animateTo(target: Float, durationMs: Int) {
        val steps = 8
        val start = _eyeOpenAmount.value
        val stepDelay = (durationMs / steps).toLong().coerceAtLeast(1)
        for (i in 1..steps) {
            val t = i / steps.toFloat()
            _eyeOpenAmount.update { start + (target - start) * t }
            delay(stepDelay)
        }
        _eyeOpenAmount.update { target }
    }

    /** Trigger a single quick blink on demand, e.g. right after waking up. */
    fun blinkOnce() {
        scope.launch { doBlink() }
    }
}
