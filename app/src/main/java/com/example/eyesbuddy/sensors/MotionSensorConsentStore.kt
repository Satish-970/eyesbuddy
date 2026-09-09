package com.example.eyesbuddy.sensors

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.motionSensorDataStore by preferencesDataStore(name = "eyesbuddy_settings")

/** Persists the user's explicit in-app choice to use motion sensors. */
class MotionSensorConsentStore(private val context: Context) {
    val consent: Flow<Boolean?> = context.motionSensorDataStore.data.map { preferences ->
        preferences[CONSENT_KEY]
    }

    suspend fun setConsent(allowed: Boolean) {
        context.motionSensorDataStore.edit { preferences ->
            preferences[CONSENT_KEY] = allowed
        }
    }

    private companion object {
        val CONSENT_KEY = booleanPreferencesKey("motion_sensor_consent")
    }
}
