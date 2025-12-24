package com.nextcs.aurora.navigation

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class LiveTrafficLevel {
    LIGHT, MODERATE, HEAVY, SEVERE
}

data class TrafficCondition(
    val level: LiveTrafficLevel,
    val delayMinutes: Int,
    val description: String
)

data class ReroutingState(
    val isOffRoute: Boolean = false,
    val distanceFromRoute: Double = 0.0, // meters
    val isRerouting: Boolean = false,
    val rerouteAttempts: Int = 0
)

class TrafficAwareNavigationService(private val context: Context) {
    
    private val directionsService = DirectionsService(context)
    
    private val _trafficCondition = MutableStateFlow(TrafficCondition(LiveTrafficLevel.LIGHT, 0, "No delays"))
    val trafficCondition: StateFlow<TrafficCondition> = _trafficCondition.asStateFlow()
    
    private val _reroutingState = MutableStateFlow(ReroutingState())
    val reroutingState: StateFlow<ReroutingState> = _reroutingState.asStateFlow()
    
    private var currentRoute: RouteInfo? = null
    private var lastRerouteTime = 0L
    private val REROUTE_COOLDOWN_MS = 10000L // 10 seconds between reroutes
    private val OFF_ROUTE_THRESHOLD = 50.0 // meters
    private val MAX_REROUTE_ATTEMPTS = 3
    
    /**
     * Check if user is off-route and trigger rerouting if needed
     */
    fun checkOffRoute(
        currentLocation: LatLng,
        route: RouteInfo
    ): Boolean {
        currentRoute = route
        
        // Find closest point on route
        val closestDistance = findClosestDistanceToRoute(currentLocation, route)
        
        val isOffRoute = closestDistance > OFF_ROUTE_THRESHOLD
        
        _reroutingState.value = _reroutingState.value.copy(
            isOffRoute = isOffRoute,
            distanceFromRoute = closestDistance
        )
        
        return isOffRoute
    }
    
    /**
     * Automatically reroute if conditions are met
     */
    suspend fun autoReroute(
        currentLocation: LatLng,
        destination: LatLng,
        waypoints: List<LatLng> = emptyList()
    ): Result<RouteInfo> {
        val now = System.currentTimeMillis()
        val state = _reroutingState.value
        
        // Check cooldown and max attempts
        if (now - lastRerouteTime < REROUTE_COOLDOWN_MS) {
            return Result.failure(Exception("Reroute cooldown active"))
        }
        
        if (state.rerouteAttempts >= MAX_REROUTE_ATTEMPTS) {
            return Result.failure(Exception("Max reroute attempts reached"))
        }
        
        _reroutingState.value = state.copy(
            isRerouting = true,
            rerouteAttempts = state.rerouteAttempts + 1
        )
        
        lastRerouteTime = now
        
        // Fetch new route with traffic
        val result = fetchRouteWithTraffic(currentLocation, destination, waypoints)
        
        _reroutingState.value = _reroutingState.value.copy(
            isRerouting = false,
            isOffRoute = false
        )
        
        return result
    }
    
    /**
     * Reset reroute state (call when reaching destination or starting new route)
     */
    fun resetRerouteState() {
        _reroutingState.value = ReroutingState()
        lastRerouteTime = 0L
    }
    
    /**
     * Fetch route with traffic consideration
     */
    suspend fun fetchRouteWithTraffic(
        origin: LatLng,
        destination: LatLng,
        waypoints: List<LatLng> = emptyList()
    ): Result<RouteInfo> = withContext(Dispatchers.IO) {
        try {
            // Get route with traffic model
            val result = directionsService.getDirections(origin, destination, waypoints)
            
            result.onSuccess { route ->
                // Analyze traffic from duration_in_traffic
                analyzeTraffic(route)
            }
            
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Analyze traffic conditions from route data
     */
    private fun analyzeTraffic(route: RouteInfo) {
        // Estimate traffic based on distance vs duration ratio
        val avgSpeedKmh = (route.distance / 1000.0) / (route.duration / 3600.0)
        
        val (level, delayMin, description) = when {
            avgSpeedKmh >= 50 -> Triple(LiveTrafficLevel.LIGHT, 0, "Traffic is light, smooth sailing!")
            avgSpeedKmh >= 35 -> Triple(LiveTrafficLevel.MODERATE, 5, "Moderate traffic ahead")
            avgSpeedKmh >= 20 -> Triple(LiveTrafficLevel.HEAVY, 15, "Heavy traffic, expect delays")
            else -> Triple(LiveTrafficLevel.SEVERE, 30, "Severe traffic congestion")
        }
        
        _trafficCondition.value = TrafficCondition(level, delayMin, description)
    }
    
    /**
     * Find closest distance from current location to route polyline
     */
    private fun findClosestDistanceToRoute(location: LatLng, route: RouteInfo): Double {
        var minDistance = Double.MAX_VALUE
        
        // Check distance to each step in route
        route.steps.forEach { step ->
            val distance = calculateDistance(location, step.startLocation)
            if (distance < minDistance) {
                minDistance = distance
            }
        }
        
        return minDistance
    }
    
    /**
     * Calculate distance between two points in meters
     */
    private fun calculateDistance(point1: LatLng, point2: LatLng): Double {
        val earthRadius = 6371000.0 // meters
        
        val lat1Rad = Math.toRadians(point1.latitude)
        val lat2Rad = Math.toRadians(point2.latitude)
        val deltaLat = Math.toRadians(point2.latitude - point1.latitude)
        val deltaLng = Math.toRadians(point2.longitude - point1.longitude)
        
        val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
                cos(lat1Rad) * cos(lat2Rad) *
                sin(deltaLng / 2) * sin(deltaLng / 2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return earthRadius * c
    }
    
    fun getTrafficColor(): androidx.compose.ui.graphics.Color {
        return when (_trafficCondition.value.level) {
            LiveTrafficLevel.LIGHT -> androidx.compose.ui.graphics.Color(0xFF4CAF50) // Green
            LiveTrafficLevel.MODERATE -> androidx.compose.ui.graphics.Color(0xFFFFC107) // Yellow
            LiveTrafficLevel.HEAVY -> androidx.compose.ui.graphics.Color(0xFFFF9800) // Orange
            LiveTrafficLevel.SEVERE -> androidx.compose.ui.graphics.Color(0xFFF44336) // Red
        }
    }
}
