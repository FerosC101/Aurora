package com.nextcs.aurora.navigation.model

import com.nextcs.aurora.traffic.model.Position
import kotlin.random.Random

/**
 * Real-time hazard alert system - Aurora SHIELD
 */
data class HazardAlert(
    val id: String,
    val type: HazardType,
    val severity: HazardSeverity,
    val location: String,
    val position: Position,
    val distanceAhead: Float,  // meters
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

/**
 * Aurora SHIELD - Smart Hazard Intelligence for Enhanced Localized Detection
 */
class AuroraShieldSystem {
    private val activeAlerts = mutableListOf<HazardAlert>()
    
    /**
     * Scan for hazards along route
     */
    fun scanRoute(route: NavigationRoute, currentPosition: Position): List<HazardAlert> {
        // Clear old alerts
        activeAlerts.removeIf { System.currentTimeMillis() - it.timestamp > 60000 } // 1 minute
        
        // Generate hazards based on route type
        when (route.type) {
            RouteType.SMART -> {
                // Smart route: minimal hazards (already avoided)
                if (Random.nextFloat() < 0.1f) { // 10% chance
                    addMinorAlert(currentPosition)
                }
            }
            RouteType.CHILL -> {
                // Chill route: mostly safe with occasional warnings
                if (Random.nextFloat() < 0.15f) { // 15% chance
                    addSceniceRouteAlert(currentPosition)
                }
            }
            RouteType.REGULAR -> {
                // Regular route: more hazards
                if (Random.nextFloat() < 0.3f) { // 30% chance
                    addRegularRouteHazard(currentPosition)
                }
            }
        }
        
        return activeAlerts.sortedBy { it.distanceAhead }
    }
    
    private fun addMinorAlert(position: Position) {
        val alerts = listOf(
            HazardAlert(
                id = "H${Random.nextInt(1000)}",
                type = HazardType.CONSTRUCTION,
                severity = HazardSeverity.LOW,
                location = "200m ahead",
                position = position,
                distanceAhead = 200f,
                description = "Minor road work on right lane"
            ),
            HazardAlert(
                id = "H${Random.nextInt(1000)}",
                type = HazardType.POTHOLE,
                severity = HazardSeverity.LOW,
                location = "150m ahead",
                position = position,
                distanceAhead = 150f,
                description = "Small pothole detected"
            )
        )
        activeAlerts.add(alerts.random())
    }
    
    private fun addSceniceRouteAlert(position: Position) {
        val alerts = listOf(
            HazardAlert(
                id = "S${Random.nextInt(1000)}",
                type = HazardType.POTHOLE,
                severity = HazardSeverity.LOW,
                location = "250m ahead",
                position = position,
                distanceAhead = 250f,
                description = "Uneven road surface"
            )
        )
        activeAlerts.add(alerts.random())
    }
    
    private fun addRegularRouteHazard(position: Position) {
        val hazards = listOf(
            HazardAlert(
                id = "R${Random.nextInt(1000)}",
                type = HazardType.POTHOLE,
                severity = HazardSeverity.MODERATE,
                location = "100m ahead",
                position = position,
                distanceAhead = 100f,
                description = "Large pothole in center lane"
            ),
            HazardAlert(
                id = "R${Random.nextInt(1000)}",
                type = HazardType.FLOOD,
                severity = HazardSeverity.HIGH,
                location = "300m ahead",
                position = position,
                distanceAhead = 300f,
                description = "Flooded area - proceed with caution"
            ),
            HazardAlert(
                id = "R${Random.nextInt(1000)}",
                type = HazardType.ACCIDENT,
                severity = HazardSeverity.CRITICAL,
                location = "500m ahead",
                position = position,
                distanceAhead = 500f,
                description = "Traffic incident - expect delays"
            ),
            HazardAlert(
                id = "R${Random.nextInt(1000)}",
                type = HazardType.CONSTRUCTION,
                severity = HazardSeverity.MODERATE,
                location = "180m ahead",
                position = position,
                distanceAhead = 180f,
                description = "Road construction - lane merge"
            )
        )
        activeAlerts.add(hazards.random())
    }
    
    /**
     * Get critical alerts that need immediate attention
     */
    fun getCriticalAlerts(): List<HazardAlert> {
        return activeAlerts.filter { 
            it.severity == HazardSeverity.CRITICAL || 
            (it.severity == HazardSeverity.HIGH && it.distanceAhead < 200f)
        }
    }
    
    /**
     * Clear all alerts
     */
    fun clearAlerts() {
        activeAlerts.clear()
    }
}
