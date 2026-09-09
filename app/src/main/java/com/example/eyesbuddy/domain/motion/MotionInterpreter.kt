package com.example.eyesbuddy.domain.motion

import kotlin.math.atan2
import kotlin.math.sqrt

/** Converts accelerometer and gyroscope samples into display-neutral eye motion values. */
class MotionInterpreter {
    fun tiltFromAcceleration(x: Float, y: Float, gravity: Float): Pair<Float, Float> =
        (x / gravity).coerceIn(-1f, 1f) to (y / gravity).coerceIn(-1f, 1f)

    fun gazeFromRotation(x: Float, y: Float, z: Float): Pair<Float, Float> {
        val horizontal = atan2(y.toDouble(), sqrt((x * x + z * z).toDouble())).toFloat()
        val vertical = atan2(x.toDouble(), sqrt((y * y + z * z).toDouble())).toFloat()
        return (horizontal / 1.2f).coerceIn(-1f, 1f) to (vertical / 1.2f).coerceIn(-1f, 1f)
    }
}
