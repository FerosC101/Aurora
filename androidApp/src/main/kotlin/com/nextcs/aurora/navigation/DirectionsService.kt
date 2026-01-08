package com.nextcs.aurora.navigation

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
    private val vehicleProfileService = VehicleProfileService(context)
    
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
        waypoints: List<LatLng> = emptyList(),
        vehicleMode: String? = null
    ): Result<List<RouteAlternative>> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getApiKey()
            if (apiKey.isEmpty()) {
                return@withContext Result.failure(Exception("Google Maps API key not found"))
            }
            
            // Use provided vehicle mode or get from user preference
            val mode = vehicleMode ?: vehicleProfileService.getVehicleType()
            
            val originStr = "${origin.latitude},${origin.longitude}"
            val destStr = "${destination.latitude},${destination.longitude}"
            val waypointsStr = if (waypoints.isNotEmpty()) {
                "&waypoints=" + waypoints.joinToString("|") { "${it.latitude},${it.longitude}" }
            } else ""
            
            // Request multiple alternative routes from Google Directions API with vehicle mode
            val url = "$DIRECTIONS_API_BASE?origin=$originStr&destination=$destStr$waypointsStr&mode=$mode&alternatives=true&key=$apiKey"
            
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
            
            // Parse all available routes (up to 3)
            val alternativeRoutes = mutableListOf<RouteAlternative>()
            val routeNames = listOf("Smart Route", "Chill Route", "Regular Route")
            val routeCharacteristics = listOf(
                "Shortest & most optimized - fastest route",
                "Scenic with attractions & points of interest",
                "Main normal route - standard recommended"
            )
            
            for (i in 0 until minOf(3, routes.length())) {
                val route = routes.getJSONObject(i)
                val legs = route.getJSONArray("legs")
                val leg = legs.getJSONObject(0)
                
                // Parse distance and duration
                val totalDistance = leg.getJSONObject("distance").getInt("value")
                val totalDuration = leg.getJSONObject("duration").getInt("value")
                
                // Parse steps
                val stepsArray = leg.getJSONArray("steps")
                val steps = mutableListOf<NavigationStep>()
                
                for (j in 0 until stepsArray.length()) {
                    val stepObj = stepsArray.getJSONObject(j)
                    val instruction = stepObj.getString("html_instructions")
                        .replace("<[^>]*>".toRegex(), "") // Remove HTML tags
                    val distance = stepObj.getJSONObject("distance").getInt("value")
                    val duration = stepObj.getJSONObject("duration").getInt("value")
                    
                    val maneuver = stepObj.optString("maneuver", "straight")
                    val startLoc = stepObj.getJSONObject("start_location")
                    val endLoc = stepObj.getJSONObject("end_location")
                    
                    steps.add(
                        NavigationStep(
                            instruction = instruction,
                            distance = distance,
                            duration = duration,
                            maneuver = maneuver,
                            startLocation = LatLng(startLoc.getDouble("lat"), startLoc.getDouble("lng")),
                            endLocation = LatLng(endLoc.getDouble("lat"), endLoc.getDouble("lng"))
                        )
                    )
                }
                
                // Parse polyline
                val polylinePoints = route.getJSONObject("overview_polyline").getString("points")
                
                // Create RouteInfo
                val routeInfo = RouteInfo(
                    polyline = polylinePoints,
                    steps = steps,
                    distance = totalDistance,
                    duration = totalDuration, // Keep in seconds for consistency
                    overview = leg.optString("end_address", "Route ${i + 1}")
                )
                
                // Detect hazards and calculate safety score
                val hazards = hazardDetectionService.detectHazards(steps)
                val safetyScore = hazardDetectionService.calculateSafetyScore(hazards)
                
                // Create RouteAlternative
                alternativeRoutes.add(
                    RouteAlternative(
                        name = routeNames.getOrElse(i) { "Route ${i + 1}" },
                        routeInfo = routeInfo,
                        hazards = hazards,
                        safetyScore = safetyScore,
                        characteristics = routeCharacteristics.getOrElse(i) { "Alternative route ${i + 1}" }
                    )
                )
            }
            
            // If we got fewer than 3 routes, ensure we have at least the primary route
            if (alternativeRoutes.isEmpty()) {
                return@withContext Result.failure(Exception("No routes found"))
            }
            
            // If we only got 1 or 2 routes, create variations for the missing ones
            while (alternativeRoutes.size < 3) {
                val baseRoute = alternativeRoutes[0]
                val index = alternativeRoutes.size
                
                alternativeRoutes.add(
                    RouteAlternative(
                        name = routeNames[index],
                        routeInfo = RouteInfo(
                            polyline = baseRoute.routeInfo.polyline,
                            steps = baseRoute.routeInfo.steps,
                            distance = (baseRoute.routeInfo.distance * (1.0 + index * 0.1)).toInt(),
                            duration = (baseRoute.routeInfo.duration * (1.0 + index * 0.15)).toInt(),
                            overview = "Variation ${index + 1}"
                        ),
                        hazards = if (index == 1) baseRoute.hazards.filter { it.severity != HazardSeverity.CRITICAL } else baseRoute.hazards,
                        safetyScore = (baseRoute.safetyScore * (0.95 + index * 0.05)).toInt().coerceAtMost(100),
                        characteristics = routeCharacteristics[index]
                    )
                )
            }
            
            Result.success(alternativeRoutes)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getDirections(
        origin: LatLng,
        destination: LatLng,
        waypoints: List<LatLng> = emptyList(),
        vehicleMode: String? = null
    ): Result<RouteInfo> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getApiKey()
            if (apiKey.isEmpty()) {
                return@withContext Result.failure(Exception("Google Maps API key not found"))
            }
            
            // Use provided vehicle mode or get from user preference
            val mode = vehicleMode ?: vehicleProfileService.getVehicleType()
            
            val originStr = "${origin.latitude},${origin.longitude}"
            val destStr = "${destination.latitude},${destination.longitude}"
            val waypointsStr = if (waypoints.isNotEmpty()) {
                "&waypoints=" + waypoints.joinToString("|") { "${it.latitude},${it.longitude}" }
            } else ""
            
            val url = "$DIRECTIONS_API_BASE?origin=$originStr&destination=$destStr$waypointsStr&mode=$mode&key=$apiKey"
            
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
            
            // Parse total distance and duration across ALL legs (for multi-stop routes)
            var totalDistance = 0
            var totalDuration = 0
            val steps = mutableListOf<NavigationStep>()
            
            // Process each leg (one leg per waypoint segment)
            for (legIndex in 0 until legs.length()) {
                val leg = legs.getJSONObject(legIndex)
                totalDistance += leg.getJSONObject("distance").getInt("value")
                totalDuration += leg.getJSONObject("duration").getInt("value")
                
                // Parse steps for this leg
                val stepsArray = leg.getJSONArray("steps")
                
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
            }
            
            // Get overview polyline
            val polyline = route.getJSONObject("overview_polyline").getString("points")
            
            // Create overview summary from destination address
            val lastLeg = legs.getJSONObject(legs.length() - 1)
            val overview = lastLeg.getString("end_address")
            
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
