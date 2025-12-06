package org.aurora.traffic.model

import kotlin.random.Random

data class Vehicle(
    val id: String,
    var position: Position,
    var currentRoad: String? = null,
    var targetIntersection: String? = null,
    val destination: String, // Final destination intersection ID
    var speed: Float = 0f, // current speed in km/h
    val maxSpeed: Float = 50f,
    var route: List<String> = emptyList(), // List of road IDs to follow
    var routeIndex: Int = 0,
    var isWaiting: Boolean = false,
    var waitTime: Float = 0f,
    var hasReachedDestination: Boolean = false,
    var isRerouted: Boolean = false, // Visual indicator
    val color: VehicleColor = VehicleColor.BLUE
) {
    private var distanceOnRoad: Float = 0f
    
    fun update(
        deltaTime: Float,
        roads: Map<String, Road>,
        intersections: Map<String, Intersection>,
        nearbyVehicles: List<Vehicle>
    ) {
        if (hasReachedDestination) return
        
        // Check if we need a new route segment
        if (currentRoad == null && route.isNotEmpty() && routeIndex < route.size) {
            currentRoad = route[routeIndex]
            distanceOnRoad = 0f
            isWaiting = false
        }
        
        val road = currentRoad?.let { roads[it] } ?: return
        
        // Check for vehicles ahead
        val vehicleAhead = findVehicleAhead(nearbyVehicles, road)
        val safeDistance = 15f // meters
        
        // Check intersection
        val intersection = targetIntersection?.let { intersections[it] }
        val canPassIntersection = intersection?.canVehiclePass(road.direction) ?: true
        
        // Adjust speed
        when {
            !canPassIntersection && distanceOnRoad > road.length * 0.8f -> {
                // Approaching red light - slow down
                speed = (speed - 20f * deltaTime).coerceAtLeast(0f)
                isWaiting = true
                waitTime += deltaTime
                intersection?.addToQueue(id, road.direction)
            }
            vehicleAhead != null && vehicleAhead.position.distanceTo(position) < safeDistance -> {
                // Vehicle ahead - match its speed or slower
                speed = (vehicleAhead.speed * 0.8f).coerceAtMost(speed)
                isWaiting = true
            }
            else -> {
                // Accelerate to road speed
                val targetSpeed = road.currentSpeed.coerceAtMost(maxSpeed)
                speed = (speed + 15f * deltaTime).coerceAtMost(targetSpeed)
                isWaiting = false
                
                if (waitTime > 0) {
                    intersection?.removeFromQueue(id, road.direction)
                    waitTime = 0f
                }
            }
        }
        
        // Move forward
        val distanceTraveled = (speed / 3.6f) * deltaTime // Convert km/h to m/s
        distanceOnRoad += distanceTraveled
        
        // Update position along road waypoints
        updatePositionOnRoad(road)
        
        // Check if reached end of road
        if (distanceOnRoad >= road.length) {
            routeIndex++
            if (routeIndex >= route.size) {
                // Reached destination
                hasReachedDestination = true
            } else {
                currentRoad = null
                targetIntersection = road.endIntersection
            }
        }
    }
    
    private fun updatePositionOnRoad(road: Road) {
        if (road.positions.size < 2) return
        
        val progress = (distanceOnRoad / road.length).coerceIn(0f, 1f)
        val totalSegments = road.positions.size - 1
        val segmentIndex = (progress * totalSegments).toInt().coerceIn(0, totalSegments - 1)
        val segmentProgress = (progress * totalSegments) - segmentIndex
        
        val start = road.positions[segmentIndex]
        val end = road.positions.getOrNull(segmentIndex + 1) ?: road.positions.last()
        
        position = Position(
            start.x + (end.x - start.x) * segmentProgress,
            start.y + (end.y - start.y) * segmentProgress
        )
    }
    
    private fun findVehicleAhead(nearbyVehicles: List<Vehicle>, road: Road): Vehicle? {
        return nearbyVehicles
            .filter { it.id != id && it.currentRoad == road.id && !it.hasReachedDestination }
            .minByOrNull { it.position.distanceTo(position) }
            ?.takeIf { it.position.distanceTo(position) < 30f }
    }
    
    fun updateRoute(newRoute: List<String>) {
        if (newRoute.isNotEmpty() && newRoute != route) {
            route = newRoute
            routeIndex = 0
            currentRoad = null
            isRerouted = true
        }
    }
    
    fun resetReroutedFlag() {
        isRerouted = false
    }
}

enum class VehicleColor {
    BLUE,
    RED,
    GREEN,
    YELLOW,
    PURPLE
}
