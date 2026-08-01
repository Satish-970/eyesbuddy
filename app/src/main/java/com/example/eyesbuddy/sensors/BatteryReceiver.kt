package com.example.eyesbuddy.sensors

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Listens for charger connect/disconnect and battery level changes.
 * ACTION_POWER_CONNECTED fires exactly once when the cable is plugged in,
 * which is what triggers the one-shot "Excited" emotion; ongoing charging
 * state (for the calmer "Happy" baseline) comes from ACTION_BATTERY_CHANGED.
 */
class BatteryReceiver(private val context: Context) {

    private val _isCharging = MutableStateFlow(false)
    val isCharging: StateFlow<Boolean> = _isCharging

    private val _batteryLevel = MutableStateFlow(100)
    val batteryLevel: StateFlow<Int> = _batteryLevel

    private val _justConnected = MutableStateFlow(0L) // pulse counter
    val justConnected: StateFlow<Long> = _justConnected

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    _isCharging.value = true
                    _justConnected.value = _justConnected.value + 1
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    _isCharging.value = false
                }
                Intent.ACTION_BATTERY_CHANGED -> {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    if (level >= 0 && scale > 0) {
                        _batteryLevel.value = (level * 100) / scale
                    }
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    _isCharging.value = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
                }
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
            // already unregistered - safe to ignore
        }
    }
}
