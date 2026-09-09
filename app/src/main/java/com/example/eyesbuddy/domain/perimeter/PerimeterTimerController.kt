package com.example.eyesbuddy.domain.perimeter

/** Calculates a stable one-minute perimeter progress value without Android dependencies. */
class PerimeterTimerController(private val intervalMillis: Long = DEFAULT_INTERVAL_MILLIS) {
    fun progressAt(epochMillis: Long): Float {
        val elapsed = Math.floorMod(epochMillis, intervalMillis)
        return elapsed.toFloat() / intervalMillis.toFloat()
    }

    private companion object {
        const val DEFAULT_INTERVAL_MILLIS = 60_000L
    }
}
