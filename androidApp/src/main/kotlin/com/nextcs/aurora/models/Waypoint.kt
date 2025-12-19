package com.nextcs.aurora.models

import com.nextcs.aurora.traffic.model.Position

data class Waypoint(
    val id: String = java.util.UUID.randomUUID().toString(),
    val position: Position,
    val name: String,
    val address: String = "",
    val order: Int = 0,
    val stopDuration: Int = 0, // minutes
    val isOrigin: Boolean = false,
    val isDestination: Boolean = false
)

data class MultiStopRoute(
    val id: String = java.util.UUID.randomUUID().toString(),
    val waypoints: List<Waypoint>,
    val totalDistance: Double = 0.0, // km
    val totalDuration: Int = 0, // minutes
    val createdAt: Long = System.currentTimeMillis()
) {
    val origin: Waypoint?
        get() = waypoints.firstOrNull { it.isOrigin }
    
    val destination: Waypoint?
        get() = waypoints.firstOrNull { it.isDestination }
    
    val intermediateStops: List<Waypoint>
        get() = waypoints.filter { !it.isOrigin && !it.isDestination }
    
    fun optimized(): MultiStopRoute {
        // Simple optimization: keep origin and destination, sort intermediate stops
        val origin = this.origin ?: return this
        val destination = this.destination ?: return this
        val stops = intermediateStops
        
        if (stops.isEmpty()) return this
        
        // For now, just reorder by distance from origin (simple greedy algorithm)
        val sortedStops = stops.sortedBy { stop ->
            calculateDistance(origin.position, stop.position)
        }
        
        val optimizedWaypoints = listOf(origin) + 
            sortedStops.mapIndexed { index, stop -> stop.copy(order = index + 1) } + 
            destination.copy(order = sortedStops.size + 1)
        
        return copy(waypoints = optimizedWaypoints)
    }
    
    private fun calculateDistance(from: Position, to: Position): Double {
        // Simple distance using x,y coordinates
        val dx = (to.x - from.x).toDouble()
        val dy = (to.y - from.y).toDouble()
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}
