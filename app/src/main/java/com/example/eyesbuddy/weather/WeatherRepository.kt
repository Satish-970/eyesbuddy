package com.example.eyesbuddy.weather

import com.example.eyesbuddy.data.WeatherSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Live weather source. It uses a public weather endpoint and keeps the last
 * successful snapshot so the screen still has a friendly fallback when offline.
 */
class WeatherRepository {
    private val _weather = MutableStateFlow(
        WeatherSnapshot(
            temperature = 28,
            condition = "Clear",
            icon = "sun",
            locationName = "Offline",
            feelsLike = 28,
            description = "Pleasant sky",
            updatedAtMillis = 0L
        )
    )
    val weather: StateFlow<WeatherSnapshot> = _weather

    suspend fun refresh(latitude: Double? = null, longitude: Double? = null) {
        withContext(Dispatchers.IO) {
            try {
                val endpoint = if (latitude != null && longitude != null) {
                    "$WEATHER_ENDPOINT?format=j1&lat=$latitude&lon=$longitude"
                } else {
                    WEATHER_ENDPOINT
                }
                val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 7000
                    readTimeout = 7000
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "EyesBuddy/1.0")
                }

                try {
                    val payload = connection.inputStream.bufferedReader().use { it.readText() }
                    val root = JSONObject(payload)
                    val current = root.getJSONArray("current_condition").getJSONObject(0)
                    val nearestArea = root.getJSONArray("nearest_area").getJSONObject(0)

                    val description = current.getJSONArray("weatherDesc").getJSONObject(0).optString("value", "Clear")
                    val locationName = nearestArea.getJSONArray("areaName").getJSONObject(0).optString("value", "Near you")
                    val temperature = current.optString("temp_C", "28").toIntOrNull() ?: 28
                    val feelsLike = current.optString("FeelsLikeC", temperature.toString()).toIntOrNull() ?: temperature

                    _weather.value = WeatherSnapshot(
                        temperature = temperature,
                        condition = conditionLabel(description),
                        icon = iconFor(description),
                        locationName = locationName.ifBlank { "Near you" },
                        feelsLike = feelsLike,
                        description = description,
                        updatedAtMillis = System.currentTimeMillis()
                    )
                } finally {
                    connection.disconnect()
                }
            } catch (_: Exception) {
                if (_weather.value.updatedAtMillis == 0L) {
                    _weather.value = WeatherSnapshot(
                        temperature = 28,
                        condition = "Clear",
                        icon = "sun",
                        locationName = "Offline",
                        feelsLike = 28,
                        description = "Pleasant sky",
                        updatedAtMillis = 0L
                    )
                }
            }
        }
    }

    private fun conditionLabel(description: String): String {
        val normalized = description.trim().lowercase()
        return when {
            normalized.contains("thunder") -> "Storm"
            normalized.contains("rain") || normalized.contains("drizzle") -> "Rain"
            normalized.contains("snow") || normalized.contains("sleet") -> "Snow"
            normalized.contains("fog") || normalized.contains("mist") || normalized.contains("haze") -> "Fog"
            normalized.contains("cloud") -> "Cloudy"
            normalized.contains("clear") || normalized.contains("sun") -> "Clear"
            else -> description.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

    private fun iconFor(description: String): String {
        val normalized = description.trim().lowercase()
        return when {
            normalized.contains("thunder") -> "storm"
            normalized.contains("rain") || normalized.contains("drizzle") -> "rain"
            normalized.contains("snow") || normalized.contains("sleet") -> "snow"
            normalized.contains("fog") || normalized.contains("mist") || normalized.contains("haze") -> "fog"
            normalized.contains("cloud") || normalized.contains("overcast") -> "cloud"
            else -> "sun"
        }
    }

    private companion object {
        const val WEATHER_ENDPOINT = "https://wttr.in/"
    }
}
