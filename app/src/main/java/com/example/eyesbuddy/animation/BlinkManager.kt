package com.example.eyesbuddy.animation

import com.example.eyesbuddy.data.BlinkHint
import com.example.eyesbuddy.data.WinkSide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Drives natural and expressive blinking independent of the behavior engine. */
class BlinkManager(private val scope: CoroutineScope) {

    private val _eyeOpenAmount = MutableStateFlow(1f)
    val eyeOpenAmount: StateFlow<Float> = _eyeOpenAmount

    private val _leftOpenMultiplier = MutableStateFlow(1f)
    val leftOpenMultiplier: StateFlow<Float> = _leftOpenMultiplier

    private val _rightOpenMultiplier = MutableStateFlow(1f)
    val rightOpenMultiplier: StateFlow<Float> = _rightOpenMultiplier

    private var forcedDrowsy = false
    private var loopJob: Job? = null
    private var actionJob: Job? = null

    fun start() {
        if (loopJob != null) return
        loopJob = scope.launch {
            while (true) {
                delay(Random.nextLong(2400L, 6500L))
                if (!forcedDrowsy) doBlink(95, 120)
            }
        }
    }

    fun setSleeping(sleeping: Boolean) {
        forcedDrowsy = sleeping
        actionJob?.cancel()
        actionJob = scope.launch {
            if (sleeping) animateBothTo(0.34f, 450) else animateBothTo(1f, 260)
        }
    }

    fun perform(hint: BlinkHint, winkSide: WinkSide = WinkSide.NONE) {
        when (hint) {
            BlinkHint.NATURAL -> Unit
            BlinkHint.BLINK -> blinkOnce()
            BlinkHint.DOUBLE_BLINK -> scope.launch {
                doBlink(75, 95)
                delay(130)
                doBlink(75, 110)
            }
            BlinkHint.SLOW_BLINK -> scope.launch { doBlink(260, 320) }
            BlinkHint.HALF_BLINK -> scope.launch {
                animateBothTo(0.45f, 170)
                delay(230)
                animateBothTo(if (forcedDrowsy) 0.34f else 1f, 220)
            }
            BlinkHint.WINK -> wink(winkSide)
        }
    }

    fun blinkOnce() {
        scope.launch { doBlink(95, 120) }
    }

    private fun wink(side: WinkSide) {
        if (side == WinkSide.NONE) return
        actionJob?.cancel()
        actionJob = scope.launch {
            val flow = if (side == WinkSide.LEFT) _leftOpenMultiplier else _rightOpenMultiplier
            animateSingleTo(flow, 0.04f, 115)
            delay(80)
            animateSingleTo(flow, 1f, 150)
        }
    }

    private suspend fun doBlink(closeMs: Int, openMs: Int) {
        actionJob?.cancel()
        animateBothTo(0.03f, closeMs)
        animateBothTo(if (forcedDrowsy) 0.34f else 1f, openMs)
    }

    private suspend fun animateBothTo(target: Float, durationMs: Int) {
        val steps = 10
        val start = _eyeOpenAmount.value
        val delayMs = (durationMs / steps).toLong().coerceAtLeast(1)
        for (i in 1..steps) {
            val t = easeOut(i / steps.toFloat())
            _eyeOpenAmount.update { start + (target - start) * t }
            delay(delayMs)
        }
        _eyeOpenAmount.update { target }
    }

    private suspend fun animateSingleTo(flow: MutableStateFlow<Float>, target: Float, durationMs: Int) {
        val steps = 8
        val start = flow.value
        val delayMs = (durationMs / steps).toLong().coerceAtLeast(1)
        for (i in 1..steps) {
            val t = easeOut(i / steps.toFloat())
            flow.update { start + (target - start) * t }
            delay(delayMs)
        }
        flow.update { target }
    }

    private fun easeOut(t: Float): Float = 1f - (1f - t) * (1f - t)
}
