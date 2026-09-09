package com.example.eyesbuddy.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import com.example.eyesbuddy.domain.motion.MotionInterpreter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.sqrt

/**
 * Wraps the accelerometer to provide:
 *  - tilt (x, y) in range roughly -1f..1f, used to make the pupils "look"
 *    in the direction the phone is tilted.
 *  - shake events, exposed as a one-shot pulse via [shakeEvents].
 *
 * Registered at SENSOR_DELAY_UI (~60Hz cap) which is plenty for eye tracking
 * and cheap on battery. The screen itself is throttled separately by the
 * render loop (30fps active / 5fps idle) regardless of sensor rate.
 */
class MotionSensor(context: Context, private val interpreter: MotionInterpreter = MotionInterpreter()) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val _tilt = MutableStateFlow(0f to 0f) // (x, y)
    val tilt: StateFlow<Pair<Float, Float>> = _tilt

    private val _shakePulse = MutableStateFlow(0L) // increments on every shake
    val shakePulse: StateFlow<Long> = _shakePulse

    private var lastMagnitude = SensorManager.GRAVITY_EARTH
    private var lastShakeTime = 0L
    private val shakeThreshold = 12f // m/s^2 delta that counts as a shake
    private val shakeCooldownMs = 1200L

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val rawX = event.values[0]
        val rawY = event.values[1]
        val z = event.values[2]

        val (x, y) = when (windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_90 -> -rawY to rawX
            Surface.ROTATION_180 -> -rawX to -rawY
            Surface.ROTATION_270 -> rawY to -rawX
            else -> rawX to rawY
        }

        // Normalise tilt to roughly -1f..1f (device flat = 0,0).
        _tilt.value = interpreter.tiltFromAcceleration(x, y, SensorManager.GRAVITY_EARTH)

        // Shake detection: look at sudden change in total acceleration magnitude.
        val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        val delta = kotlin.math.abs(magnitude - lastMagnitude)
        lastMagnitude = magnitude

        val now = System.currentTimeMillis()
        if (delta > shakeThreshold && now - lastShakeTime > shakeCooldownMs) {
            lastShakeTime = now
            _shakePulse.value = _shakePulse.value + 1
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { /* no-op */ }
}
