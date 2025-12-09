package org.aurora.android.navigation

import org.aurora.android.navigation.model.*
import org.aurora.android.traffic.model.Position
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Navigation state for active trip
 */
data class NavigationState(
    val isNavigating: Boolean = false,
    val selectedRoute: NavigationRoute? = null,
    val currentPosition: Position = Position(0f, 0f),
    val progress: Float = 0f,              // 0.0 to 1.0
    val distanceTraveled: Float = 0f,      // meters traveled from start
    val currentSpeed: Float = 0f,          // km/h
    val eta: Int = 0,                      // minutes remaining
    val hazardsAvoided: Int = 0,
    val timeSaved: Int = 0,                // seconds
    val upcomingStoplights: List<Stoplight> = emptyList(),
    val activeHazardAlerts: List<HazardAlert> = emptyList(),
    val tripStartTime: Long = 0            // timestamp when navigation started
)

/**
 * Personal Navigation Engine for Aurora Rider
 */
class PersonalNavigationEngine {
    private val _navigationState = MutableStateFlow(NavigationState())
    val navigationState: StateFlow<NavigationState> = _navigationState.asStateFlow()
    
    private val auroraShield = AuroraShieldSystem()
    private var simulationTime = 0f
    
    /**
     * Generate three route options for comparison
     */
    fun generateRoutes(origin: String, destination: String): List<NavigationRoute> {
        val baseWaypoints = listOf(
            Position(100f, 100f),   // Start
            Position(250f, 150f),
            Position(400f, 200f),
            Position(550f, 250f),
            Position(700f, 300f)    // End
        )
        
        val smartWaypoints = listOf(
            Position(100f, 100f),
            Position(200f, 120f),
            Position(350f, 180f),
            Position(500f, 220f),
            Position(650f, 280f),
            Position(700f, 300f)
        )
        
        val chillWaypoints = listOf(
            Position(100f, 100f),
            Position(180f, 180f),
            Position(300f, 250f),
            Position(450f, 200f),
            Position(600f, 270f),
            Position(700f, 300f)
        )
        
        return listOf(
            // Smart Route (Recommended)
            NavigationRoute(
                type = RouteType.SMART,
                name = "Smart Route",
                description = "Optimized for speed & safety",
                estimatedTime = 14,
                distance = 8.2f,
                safetyScore = 95,
                hazardCount = 0,
                trafficLevel = TrafficLevel.LIGHT,
                timeSavedVsBaseline = 4,
                waypoints = smartWaypoints,
                stoplights = listOf(
                    Stoplight("SL1", "Tech Blvd & River Dr", Position(200f, 120f), 180f, StoplightState.GREEN, 15),
                    Stoplight("SL2", "Main St & Park Rd", Position(500f, 220f), 320f, StoplightState.RED, 28)
                )
            ),
            
            // Chill Route (Scenic)
            NavigationRoute(
                type = RouteType.CHILL,
                name = "Chill Route",
                description = "Scenic route with beautiful views",
                estimatedTime = 22,
                distance = 9.8f,
                safetyScore = 98,
                hazardCount = 0,
                trafficLevel = TrafficLevel.VERY_LIGHT,
                scenicPoints = 3,
                timeSavedVsBaseline = -4,
                waypoints = chillWaypoints,
                stoplights = listOf(
                    Stoplight("SL3", "Central Ave & 5th St", Position(450f, 200f), 450f, StoplightState.GREEN, 43)
                )
            ),
            
            // Regular Route (Baseline)
            NavigationRoute(
                type = RouteType.REGULAR,
                name = "Regular Route",
                description = "Direct route via main roads",
                estimatedTime = 18,
                distance = 8.5f,
                safetyScore = 65,
                hazardCount = 4,
                trafficLevel = TrafficLevel.HEAVY,
                timeSavedVsBaseline = 0,
                waypoints = baseWaypoints,
                stoplights = listOf(
                    Stoplight("SL4", "Highway 101 & Oak St", Position(400f, 200f), 250f, StoplightState.RED, 38)
                )
            )
        )
    }
    
    /**
     * Start navigation with selected route
     */
    fun startNavigation(route: NavigationRoute) {
        auroraShield.clearAlerts()
        _navigationState.value = NavigationState(
            isNavigating = true,
            selectedRoute = route,
            currentPosition = route.waypoints.first(),
            progress = 0f,
            currentSpeed = 0f,
            eta = route.estimatedTime,
            upcomingStoplights = route.stoplights,
            tripStartTime = System.currentTimeMillis()
        )
        simulationTime = 0f
    }
    
    /**
     * Update navigation simulation
     */
    fun update(deltaTime: Float) {
        val state = _navigationState.value
        if (!state.isNavigating || state.selectedRoute == null) return
        
        simulationTime += deltaTime
        
        // Update progress (complete in ~3 minutes for demo)
        val newProgress = (state.progress + deltaTime * 0.005f).coerceAtMost(1f)
        
        // Simulate speed variation (25-55 km/h)
        val baseSpeed = when (state.selectedRoute.type) {
            RouteType.SMART -> 45f
            RouteType.CHILL -> 35f
            RouteType.REGULAR -> 40f
        }
        val speedVariation = (Math.random() * 10 - 5).toFloat()
        val newSpeed = (baseSpeed + speedVariation).coerceIn(25f, 55f)
        
        // Update position along route
        val waypoints = state.selectedRoute.waypoints
        val segmentIndex = (newProgress * (waypoints.size - 1)).toInt()
        val segmentProgress = (newProgress * (waypoints.size - 1)) - segmentIndex
        
        val newPosition = if (segmentIndex < waypoints.size - 1) {
            val start = waypoints[segmentIndex]
            val end = waypoints[segmentIndex + 1]
            Position(
                x = start.x + (end.x - start.x) * segmentProgress,
                y = start.y + (end.y - start.y) * segmentProgress
            )
        } else {
            waypoints.last()
        }
        
        // Update stoplights
        val updatedStoplights = state.upcomingStoplights.map { light ->
            light.copy().also { it.update(deltaTime) }
        }
        
        // Calculate ETA
        val remainingTime = (state.selectedRoute.estimatedTime * (1f - newProgress)).toInt()
        
        // Scan for hazards with Aurora SHIELD
        val hazardAlerts = auroraShield.scanRoute(state.selectedRoute, newPosition)
        
        // Update hazards avoided (for Smart/Chill routes)
        val hazardsAvoided = when (state.selectedRoute.type) {
            RouteType.SMART, RouteType.CHILL -> {
                // Increment when passing hazard zones
                if (newProgress > 0.3f && state.hazardsAvoided == 0) 1
                else state.hazardsAvoided
            }
            RouteType.REGULAR -> 0
        }
        
        _navigationState.value = state.copy(
            progress = newProgress,
            currentSpeed = newSpeed,
            currentPosition = newPosition,
            eta = remainingTime,
            hazardsAvoided = hazardsAvoided,
            timeSaved = (state.selectedRoute.timeSavedVsBaseline * 60 * newProgress).toInt(),
            upcomingStoplights = updatedStoplights,
            activeHazardAlerts = hazardAlerts
        )
        
        // Check if navigation complete
        if (newProgress >= 1f) {
            completeNavigation()
        }
    }
    
    /**
     * End navigation
     */
    fun endNavigation() {
        auroraShield.clearAlerts()
        _navigationState.value = NavigationState()
        simulationTime = 0f
    }
    
    private fun completeNavigation() {
        val state = _navigationState.value
        _navigationState.value = state.copy(
            isNavigating = false,
            progress = 1f,
            eta = 0
        )
    }
}
