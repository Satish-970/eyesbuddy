package com.example.eyesbuddy.sensors

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.eyesbuddy.domain.motion.MotionInterpreter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Reads the gyroscope only while started; no dangerous Android permission is required. */
class GyroscopeSensorSource(context: Context, private val interpreter: MotionInterpreter = MotionInterpreter()) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val _gaze = MutableStateFlow(0f to 0f)
    val gaze: StateFlow<Pair<Float, Float>> = _gaze
    val isAvailable: Boolean = context.packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_GYROSCOPE)

    fun start() {
        sensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_GYROSCOPE && event.values.size >= 3) {
            _gaze.value = interpreter.gazeFromRotation(event.values[0], event.values[1], event.values[2])
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
