package org.aurora.navigation.model

import org.aurora.traffic.model.Position

/**
 * Route types for Aurora Rider personal navigation
 */
enum class RouteType {
    SMART,      // Recommended - fastest + safest
    CHILL,      // Scenic - relaxing with beautiful views
    REGULAR     // Baseline - standard GPS routing
}

/**
 * A navigation route from origin to destination
 */
data class NavigationRoute(
    val type: RouteType,
    val name: String,
    val description: String,
    val estimatedTime: Int,        // minutes
    val distance: Float,            // km
    val safetyScore: Int,           // 0-100
    val hazardCount: Int,
    val trafficLevel: TrafficLevel,
    val scenicPoints: Int = 0,
    val timeSavedVsBaseline: Int = 0,  // minutes (positive = saves time)
    val waypoints: List<Position>,
    val stoplights: List<Stoplight>
)

enum class TrafficLevel {
    VERY_LIGHT,
    LIGHT,
    MODERATE,
    HEAVY
}

/**
 * Traffic light with timing information
 */
data class Stoplight(
    val id: String,
    val location: String,           // "Central Ave & 5th St"
    val position: Position,
    val distanceFromStart: Float,   // meters
    var state: StoplightState = StoplightState.GREEN,
    var remainingTime: Int = 45     // seconds
) {
    fun update(deltaTime: Float) {
        remainingTime = (remainingTime - deltaTime.toInt()).coerceAtLeast(0)
        
        if (remainingTime <= 0) {
            // Cycle to next state
            state = when (state) {
                StoplightState.GREEN -> {
                    remainingTime = 5
                    StoplightState.YELLOW
                }
                StoplightState.YELLOW -> {
                    remainingTime = 60
                    StoplightState.RED
                }
                StoplightState.RED -> {
                    remainingTime = 45
                    StoplightState.GREEN
                }
            }
        }
    }
}

enum class StoplightState {
    GREEN,
    YELLOW,
    RED
}

/**
 * Hazard on the route
 */
data class RouteHazard(
    val type: HazardType,
    val location: String,
    val position: Position,
    val severity: HazardSeverity
)

enum class HazardType {
    POTHOLE,
    FLOOD,
    ACCIDENT,
    CONSTRUCTION
}

enum class HazardSeverity {
    LOW,
    MODERATE,
    HIGH,
    CRITICAL
}
