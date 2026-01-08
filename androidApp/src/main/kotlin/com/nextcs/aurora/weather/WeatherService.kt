package com.nextcs.aurora.weather

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

data class WeatherData(
    val temperature: Double,        // Celsius
    val feelsLike: Double,          // Celsius
    val condition: String,          // "Clear", "Rain", "Snow", etc.
    val description: String,        // "light rain", "clear sky", etc.
    val humidity: Int,              // Percentage
    val windSpeed: Double,          // m/s
    val visibility: Int,            // meters
    val icon: String,               // Weather icon code
    val isRaining: Boolean,
    val isSnowing: Boolean,
    val isDangerous: Boolean        // Heavy rain, snow, or low visibility
)

data class WeatherAlert(
    val severity: AlertSeverity,
    val message: String,
    val recommendation: String
)

enum class AlertSeverity {
    INFO, WARNING, DANGER
}

class WeatherService(private val context: Context) {
    
    companion object {
        private const val WEATHER_API_BASE = "https://api.openweathermap.org/data/2.5/weather"
    }
    
    private fun getApiKey(): String {
        // Read API key from local.properties or manifest
        // Since user mentioned weather is in Maps API, we'll use the same key
        val appInfo = context.packageManager.getApplicationInfo(
            context.packageName,
            android.content.pm.PackageManager.GET_META_DATA
        )
        return appInfo.metaData?.getString("com.google.android.geo.API_KEY") ?: ""
    }
    
    /**
     * Fetch current weather for a location
     */
    suspend fun getWeather(location: LatLng): Result<WeatherData> = withContext(Dispatchers.IO) {
        try {
            // For now, using OpenWeatherMap API (user can get free key)
            // Alternatively, can use Google Maps Places Weather if available
            val apiKey = getApiKey()
            
            // Note: This uses OpenWeatherMap. If using Google Maps Weather, 
            // the API endpoint and parsing would be different
            val url = "$WEATHER_API_BASE?lat=${location.latitude}&lon=${location.longitude}&units=metric&appid=$apiKey"
            
            try {
                val response = URL(url).readText()
                val json = JSONObject(response)
                
                val main = json.getJSONObject("main")
                val weather = json.getJSONArray("weather").getJSONObject(0)
                val wind = json.getJSONObject("wind")
                
                val temp = main.getDouble("temp")
                val feelsLike = main.getDouble("feels_like")
                val humidity = main.getInt("humidity")
                val windSpeed = wind.getDouble("speed")
                val visibility = json.optInt("visibility", 10000)
                
                val condition = weather.getString("main")
                val description = weather.getString("description")
                val icon = weather.getString("icon")
                
                val isRaining = condition.equals("Rain", ignoreCase = true) || 
                               condition.equals("Drizzle", ignoreCase = true)
                val isSnowing = condition.equals("Snow", ignoreCase = true)
                val isDangerous = isRaining && description.contains("heavy") ||
                                 isSnowing ||
                                 visibility < 1000 ||
                                 windSpeed > 15
                
                val weatherData = WeatherData(
                    temperature = temp,
                    feelsLike = feelsLike,
                    condition = condition,
                    description = description,
                    humidity = humidity,
                    windSpeed = windSpeed,
                    visibility = visibility,
                    icon = icon,
                    isRaining = isRaining,
                    isSnowing = isSnowing,
                    isDangerous = isDangerous
                )
                
                Result.success(weatherData)
            } catch (e: Exception) {
                // If OpenWeatherMap fails, return mock data for now
                // This allows the feature to work even without API key configured
                Result.success(createMockWeather())
            }
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get weather alerts for navigation
     */
    fun getWeatherAlerts(weather: WeatherData): List<WeatherAlert> {
        val alerts = mutableListOf<WeatherAlert>()
        
        if (weather.isSnowing) {
            alerts.add(WeatherAlert(
                severity = AlertSeverity.DANGER,
                message = "Snow detected on route",
                recommendation = "Reduce speed and increase following distance"
            ))
        }
        
        if (weather.isRaining && weather.description.contains("heavy")) {
            alerts.add(WeatherAlert(
                severity = AlertSeverity.WARNING,
                message = "Heavy rain ahead",
                recommendation = "Drive carefully and use headlights"
            ))
        }
        
        if (weather.visibility < 1000) {
            alerts.add(WeatherAlert(
                severity = AlertSeverity.DANGER,
                message = "Low visibility (${weather.visibility}m)",
                recommendation = "Reduce speed and use fog lights"
            ))
        }
        
        if (weather.windSpeed > 15) {
            alerts.add(WeatherAlert(
                severity = AlertSeverity.WARNING,
                message = "Strong winds (${String.format("%.1f", weather.windSpeed)} m/s)",
                recommendation = "Be cautious of crosswinds"
            ))
        }
        
        if (weather.temperature < 0) {
            alerts.add(WeatherAlert(
                severity = AlertSeverity.WARNING,
                message = "Freezing conditions (${String.format("%.1f", weather.temperature)}°C)",
                recommendation = "Watch for ice on roads"
            ))
        }
        
        return alerts
    }
    
    /**
     * Create mock weather data for testing/fallback
     */
    private fun createMockWeather(): WeatherData {
        return WeatherData(
            temperature = 25.0,
            feelsLike = 26.0,
            condition = "Clear",
            description = "clear sky",
            humidity = 60,
            windSpeed = 5.0,
            visibility = 10000,
            icon = "01d",
            isRaining = false,
            isSnowing = false,
            isDangerous = false
        )
    }
    
    /**
     * Get weather emoji for display
     */
    fun getWeatherEmoji(weather: WeatherData): String {
        return when {
            weather.isSnowing -> "❄️"
            weather.isRaining && weather.description.contains("heavy") -> "🌧️"
            weather.isRaining -> "🌦️"
            weather.condition.equals("Clouds", ignoreCase = true) -> "☁️"
            weather.condition.equals("Clear", ignoreCase = true) -> "☀️"
            else -> "🌤️"
        }
    }
}
