package com.example.eyesbuddy.weather

import com.example.eyesbuddy.data.WeatherSnapshot
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Offline-first weather source. It keeps a cached pleasant default so the
 * companion screen remains complete without requiring a network key.
 */
class WeatherRepository {
    private val _weather = MutableStateFlow(WeatherSnapshot())
    val weather: StateFlow<WeatherSnapshot> = _weather

    suspend fun refresh() {
        delay(200)
        _weather.value = WeatherSnapshot(temperature = 28, condition = "Clear", icon = "sun")
    }
}
