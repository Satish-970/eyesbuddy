package com.example.eyesbuddy.ui

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.perimeterDataStore by preferencesDataStore(name = "eyesbuddy_perimeter")

/** Persists the selected solid perimeter color across configuration and process recreation. */
class PerimeterColorStore(private val context: Context) {
    val color: Flow<Int> = context.perimeterDataStore.data.map { preferences ->
        preferences[COLOR_KEY] ?: DEFAULT_COLOR
    }

    suspend fun setColor(color: Int) {
        context.perimeterDataStore.edit { preferences -> preferences[COLOR_KEY] = color }
    }

    private companion object {
        val COLOR_KEY = intPreferencesKey("perimeter_color")
        const val DEFAULT_COLOR = 0xFF65E6C2.toInt()
    }
}
