package org.aurora.android.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.aurora.android.navigation.RouteAlternative
import org.aurora.android.navigation.RouteInfo
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

/**
 * Service for leveraging Gemini AI to provide intelligent route recommendations
 * Uses Google's Generative AI API to analyze routes and suggest the best option
 */
class GeminiAIService(private val context: Context) {
    companion object {
        private const val GEMINI_API_KEY = "AIzaSyCeHoCQg6tr_aepWbAofy8AWYeKq4gd-e0"
        private const val GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent"
    }

    data class RouteAnalysis(
        val recommendedRoute: String,  // Smart, Chill, or Regular
        val reasoning: String,
        val safetyScore: Int,
        val efficiencyScore: Int,
        val comfortScore: Int,
        val timeEstimate: String,
        val warning: String? = null
    )

    /**
     * Analyze routes using Gemini AI to provide intelligent recommendations
     */
    suspend fun analyzeRoutes(
        routes: List<RouteAlternative>,
        currentTime: String,
        trafficConditions: String = "normal"
    ): RouteAnalysis = withContext(Dispatchers.IO) {
        try {
            // Prepare route data for analysis
            val routeData = routes.mapIndexed { index, route ->
                """
                Route ${index + 1} (${route.name}):
                - Distance: ${formatDistance(route.routeInfo.distance)} km
                - Duration: ${formatDuration(route.routeInfo.duration)} minutes
                - Safety Score: ${route.safetyScore}%
                - Hazards: ${route.hazards.joinToString(", ") { "${it.type} (${it.severity})" }}
                - Characteristics: ${route.characteristics}
                """.trimIndent()
            }.joinToString("\n\n")

            val prompt = """
            Analyze these routes and provide the BEST recommendation for a driver:
            
            Current Time: $currentTime
            Traffic Conditions: $trafficConditions
            
            $routeData
            
            Provide a JSON response with this structure:
            {
                "recommendedRoute": "Smart|Chill|Regular",
                "reasoning": "Why this route is best for current conditions",
                "safetyScore": 85,
                "efficiencyScore": 90,
                "comfortScore": 75,
                "timeEstimate": "estimated time for this route",
                "warning": "any safety warnings or null"
            }
            
            Consider:
            1. Current traffic conditions
            2. Road safety (hazards on route)
            3. Time efficiency
            4. Driver comfort and experience
            5. Current time of day (rush hour, night driving, etc.)
            """.trimIndent()

            val response = callGeminiAPI(prompt)
            parseGeminiResponse(response)
        } catch (e: Exception) {
            // Fallback to safe recommendation if API fails
            RouteAnalysis(
                recommendedRoute = "Chill",
                reasoning = "Default recommendation: Safest route chosen due to API unavailability",
                safetyScore = 90,
                efficiencyScore = 60,
                comfortScore = 85,
                timeEstimate = "Unknown",
                warning = "Using default recommendation"
            )
        }
    }

    /**
     * Get personalized route recommendation based on user profile
     */
    suspend fun getPersonalizedRecommendation(
        routes: List<RouteAlternative>,
        userPreferences: UserPreferences
    ): RouteAnalysis = withContext(Dispatchers.IO) {
        try {
            val routeData = routes.mapIndexed { index, route ->
                """
                Route ${index + 1} (${route.name}):
                - Distance: ${formatDistance(route.routeInfo.distance)} km
                - Duration: ${formatDuration(route.routeInfo.duration)} minutes
                - Safety Score: ${route.safetyScore}%
                - Hazards: ${route.hazards.size} detected
                - Characteristics: ${route.characteristics}
                """.trimIndent()
            }.joinToString("\n\n")

            val prompt = """
            Provide a personalized route recommendation based on driver preferences:
            
            Driver Profile:
            - Priority: ${userPreferences.priority} (speed, safety, comfort, or scenic)
            - Experience Level: ${userPreferences.experienceLevel}
            - Preferred Time: ${userPreferences.preferredTime}
            - RideShare Driver: ${userPreferences.isRideShareDriver}
            - Previous Route Rating: ${userPreferences.lastRouteRating}/5
            
            Available Routes:
            $routeData
            
            Return JSON:
            {
                "recommendedRoute": "Smart|Chill|Regular",
                "reasoning": "Explanation tailored to user preferences",
                "safetyScore": 0-100,
                "efficiencyScore": 0-100,
                "comfortScore": 0-100,
                "timeEstimate": "estimated time",
                "warning": "any alerts or null"
            }
            """.trimIndent()

            val response = callGeminiAPI(prompt)
            parseGeminiResponse(response)
        } catch (e: Exception) {
            // Safe fallback
            RouteAnalysis(
                recommendedRoute = userPreferences.priority.let {
                    when (it) {
                        "speed" -> "Smart"
                        "safety" -> "Chill"
                        "comfort" -> "Chill"
                        else -> "Regular"
                    }
                },
                reasoning = "Recommendation based on preferences",
                safetyScore = 85,
                efficiencyScore = 75,
                comfortScore = 80,
                timeEstimate = "Calculating...",
                warning = null
            )
        }
    }

    /**
     * Call Gemini API with the prompt
     */
    private suspend fun callGeminiAPI(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            val url = "$GEMINI_API_URL?key=$GEMINI_API_KEY"
            val connection = URL(url).openConnection()
            
            connection.apply {
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }

            val requestBody = JSONObject().apply {
                put("contents", JSONObject().apply {
                    put("parts", arrayOf(
                        JSONObject().apply {
                            put("text", prompt)
                        }
                    ))
                })
            }.toString()

            connection.outputStream.use { os ->
                os.write(requestBody.toByteArray())
                os.flush()
            }

            val responseCode = (connection as java.net.HttpURLConnection).responseCode
            return@withContext if (responseCode == 200) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                throw Exception("API returned code $responseCode")
            }
        } catch (e: Exception) {
            throw Exception("Failed to call Gemini API: ${e.message}")
        }
    }

    /**
     * Parse Gemini API response and extract route analysis
     */
    private fun parseGeminiResponse(response: String): RouteAnalysis {
        return try {
            val jsonResponse = JSONObject(response)
            val content = jsonResponse
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")

            // Extract JSON from response (Gemini might wrap it in markdown)
            val jsonStart = content.indexOf("{")
            val jsonEnd = content.lastIndexOf("}") + 1
            val jsonString = if (jsonStart != -1 && jsonEnd > jsonStart) {
                content.substring(jsonStart, jsonEnd)
            } else {
                content
            }

            val analysis = JSONObject(jsonString)
            RouteAnalysis(
                recommendedRoute = analysis.optString("recommendedRoute", "Smart"),
                reasoning = analysis.optString("reasoning", "No reasoning provided"),
                safetyScore = analysis.optInt("safetyScore", 80),
                efficiencyScore = analysis.optInt("efficiencyScore", 75),
                comfortScore = analysis.optInt("comfortScore", 80),
                timeEstimate = analysis.optString("timeEstimate", "Unknown"),
                warning = analysis.optString("warning", null)
            )
        } catch (e: Exception) {
            // Fallback parsing
            RouteAnalysis(
                recommendedRoute = "Regular",
                reasoning = "AI analysis completed with default interpretation",
                safetyScore = 80,
                efficiencyScore = 75,
                comfortScore = 80,
                timeEstimate = "Unknown",
                warning = "Unable to fully parse AI response"
            )
        }
    }

    private fun formatDistance(meters: Int): String {
        return String.format("%.1f", meters / 1000.0)
    }

    private fun formatDuration(seconds: Int): String {
        return (seconds / 60).toString()
    }
}

/**
 * User preferences for personalized route recommendations
 */
data class UserPreferences(
    val priority: String = "speed",  // speed, safety, comfort, scenic
    val experienceLevel: String = "intermediate",  // beginner, intermediate, advanced
    val preferredTime: String = "any",  // morning, afternoon, evening, night
    val isRideShareDriver: Boolean = false,
    val lastRouteRating: Int = 5
)
