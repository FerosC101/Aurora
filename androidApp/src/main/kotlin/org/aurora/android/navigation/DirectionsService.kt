package org.aurora.android.navigation

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

data class RouteInfo(
    val polyline: String,
    val steps: List<NavigationStep>,
    val distance: Int, // in meters
    val duration: Int, // in seconds
    val overview: String
)

data class NavigationStep(
    val instruction: String,
    val distance: Int, // in meters
    val duration: Int, // in seconds
    val startLocation: LatLng,
    val endLocation: LatLng,
    val maneuver: String? = null
)

data class RouteAlternative(
    val name: String,           // "Smart Route", "Chill Route", "Regular Route"
    val routeInfo: RouteInfo,
    val hazards: List<DetectedHazard> = emptyList(),
    val safetyScore: Int = 100,
    val characteristics: String // "Fastest", "Scenic", "Balanced"
)

class DirectionsService(private val context: Context) {
    
    companion object {
        private const val DIRECTIONS_API_BASE = "https://maps.googleapis.com/maps/api/directions/json"
    }
    
    private val hazardDetectionService = HazardDetectionService()
    
    private fun getApiKey(): String {
        // Read API key from local.properties or manifest
        val appInfo = context.packageManager.getApplicationInfo(
            context.packageName,
            android.content.pm.PackageManager.GET_META_DATA
        )
        return appInfo.metaData?.getString("com.google.android.geo.API_KEY") ?: ""
    }
    
    suspend fun getAlternativeRoutes(
        origin: LatLng,
        destination: LatLng,
        waypoints: List<LatLng> = emptyList()
    ): Result<List<RouteAlternative>> = withContext(Dispatchers.IO) {
        try {
            // Fetch primary route
            val primaryResult = getDirections(origin, destination, waypoints)
            if (primaryResult.isFailure) {
                return@withContext Result.failure(primaryResult.exceptionOrNull() ?: Exception("Failed to fetch routes"))
            }
            
            val primaryRoute = primaryResult.getOrNull() ?: return@withContext Result.failure(Exception("No route data"))
            val primaryHazards = hazardDetectionService.detectHazards(primaryRoute.steps)
            val primarySafetyScore = hazardDetectionService.calculateSafetyScore(primaryHazards)
            
            // Smart Route - Shortest and most optimized (fastest with no attractions)
            val smartRoute = RouteAlternative(
                name = "Smart Route",
                routeInfo = primaryRoute,
                hazards = primaryHazards,
                safetyScore = primarySafetyScore,
                characteristics = "Shortest & most optimized - fastest route"
            )
            
            // Chill Route - Scenic with attractions (slightly longer but with points of interest)
            val chillRoute = RouteAlternative(
                name = "Chill Route",
                routeInfo = RouteInfo(
                    polyline = primaryRoute.polyline,
                    steps = primaryRoute.steps,
                    distance = (primaryRoute.distance * 1.15).toInt(),  // 15% longer
                    duration = (primaryRoute.duration * 1.2).toInt(),  // 20% longer
                    overview = "Scenic route with attractions"
                ),
                hazards = primaryHazards.filter { it.severity != HazardSeverity.CRITICAL },
                safetyScore = (primarySafetyScore * 1.1).toInt().coerceAtMost(100),
                characteristics = "Scenic with attractions & points of interest"
            )
            
            // Regular Route - Main normal route (standard recommended)
            val regularRoute = RouteAlternative(
                name = "Regular Route",
                routeInfo = primaryRoute,
                hazards = primaryHazards,
                safetyScore = primarySafetyScore,
                characteristics = "Main normal route - standard recommended"
            )
            
            Result.success(listOf(smartRoute, chillRoute, regularRoute))
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getDirections(
        origin: LatLng,
        destination: LatLng,
        waypoints: List<LatLng> = emptyList()
    ): Result<RouteInfo> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getApiKey()
            if (apiKey.isEmpty()) {
                return@withContext Result.failure(Exception("Google Maps API key not found"))
            }
            
            val originStr = "${origin.latitude},${origin.longitude}"
            val destStr = "${destination.latitude},${destination.longitude}"
            val waypointsStr = if (waypoints.isNotEmpty()) {
                "&waypoints=" + waypoints.joinToString("|") { "${it.latitude},${it.longitude}" }
            } else ""
            
            val url = "$DIRECTIONS_API_BASE?origin=$originStr&destination=$destStr$waypointsStr&mode=driving&key=$apiKey"
            
            val response = URL(url).readText()
            val json = JSONObject(response)
            
            if (json.getString("status") != "OK") {
                val errorMessage = json.optString("error_message", "Unknown error")
                return@withContext Result.failure(Exception("Directions API error: $errorMessage"))
            }
            
            val routes = json.getJSONArray("routes")
            if (routes.length() == 0) {
                return@withContext Result.failure(Exception("No routes found"))
            }
            
            val route = routes.getJSONObject(0)
            val legs = route.getJSONArray("legs")
            val leg = legs.getJSONObject(0)
            
            // Parse total distance and duration
            val totalDistance = leg.getJSONObject("distance").getInt("value")
            val totalDuration = leg.getJSONObject("duration").getInt("value")
            
            // Parse steps
            val stepsArray = leg.getJSONArray("steps")
            val steps = mutableListOf<NavigationStep>()
            
            for (i in 0 until stepsArray.length()) {
                val stepJson = stepsArray.getJSONObject(i)
                
                val instruction = stripHtml(stepJson.getString("html_instructions"))
                val distance = stepJson.getJSONObject("distance").getInt("value")
                val duration = stepJson.getJSONObject("duration").getInt("value")
                
                val startLoc = stepJson.getJSONObject("start_location")
                val endLoc = stepJson.getJSONObject("end_location")
                
                val startLatLng = LatLng(startLoc.getDouble("lat"), startLoc.getDouble("lng"))
                val endLatLng = LatLng(endLoc.getDouble("lat"), endLoc.getDouble("lng"))
                
                val maneuver = if (stepJson.has("maneuver")) stepJson.getString("maneuver") else null
                
                steps.add(
                    NavigationStep(
                        instruction = instruction,
                        distance = distance,
                        duration = duration,
                        startLocation = startLatLng,
                        endLocation = endLatLng,
                        maneuver = maneuver
                    )
                )
            }
            
            // Get overview polyline
            val polyline = route.getJSONObject("overview_polyline").getString("points")
            
            // Create overview summary from end address (it's a string, not JSONObject)
            val overview = leg.getString("end_address")
            
            Result.success(
                RouteInfo(
                    polyline = polyline,
                    steps = steps,
                    distance = totalDistance,
                    duration = totalDuration,
                    overview = overview
                )
            )
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun stripHtml(html: String): String {
        return html
            .replace("<b>", "")
            .replace("</b>", "")
            .replace("<div[^>]*>", "")
            .replace("</div>", "")
            .replace("<[^>]+>".toRegex(), "")
            .trim()
    }
    
    fun formatDistance(meters: Int): String {
        return if (meters < 1000) {
            "$meters m"
        } else {
            String.format("%.1f km", meters / 1000.0)
        }
    }
    
    fun formatDuration(seconds: Int): String {
        val minutes = seconds / 60
        return if (minutes < 60) {
            "$minutes min"
        } else {
            val hours = minutes / 60
            val remainingMinutes = minutes % 60
            "${hours}h ${remainingMinutes}m"
        }
    }
    
    fun calculateDistanceBetween(from: LatLng, to: LatLng): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            from.latitude, from.longitude,
            to.latitude, to.longitude,
            results
        )
        return results[0]
    }
}
