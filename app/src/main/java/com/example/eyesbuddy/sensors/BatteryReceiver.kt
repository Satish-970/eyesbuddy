package com.example.eyesbuddy.sensors

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Listens for charger and battery events used by the companion personality. */
class BatteryReceiver(private val context: Context) {

    private val _isCharging = MutableStateFlow(false)
    val isCharging: StateFlow<Boolean> = _isCharging

    private val _batteryLevel = MutableStateFlow(100)
    val batteryLevel: StateFlow<Int> = _batteryLevel

    private val _justConnected = MutableStateFlow(0L)
    val justConnected: StateFlow<Long> = _justConnected

    private val _justDisconnected = MutableStateFlow(0L)
    val justDisconnected: StateFlow<Long> = _justDisconnected

    private val _justFull = MutableStateFlow(0L)
    val justFull: StateFlow<Long> = _justFull

    private var wasFull = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    _isCharging.value = true
                    _justConnected.value = _justConnected.value + 1
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    _isCharging.value = false
                    _justDisconnected.value = _justDisconnected.value + 1
                }
                Intent.ACTION_BATTERY_CHANGED -> updateBattery(intent)
            }
        }
    }

    fun register() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        context.registerReceiver(receiver, filter)
    }

    fun unregister() {
        try {
            context.unregisterReceiver(receiver)
        } catch (_: IllegalArgumentException) {
            // Already unregistered.
        }
    }

    private fun updateBattery(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level >= 0 && scale > 0) {
            _batteryLevel.value = (level * 100) / scale
        }
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        _isCharging.value = charging

        val fullNow = charging && _batteryLevel.value >= 100
        if (fullNow && !wasFull) {
            _justFull.value = _justFull.value + 1
        }
        wasFull = fullNow
    }
}
