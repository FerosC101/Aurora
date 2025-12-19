package com.nextcs.aurora.traffic.model

data class Intersection(
    val id: String,
    val position: Position,
    val connectedRoads: MutableList<String> = mutableListOf(), // Road IDs
    val trafficLights: MutableMap<Direction, TrafficLight> = mutableMapOf(),
    val isSignalized: Boolean = true // true = has lights, false = stop sign
) {
    private val vehicleQueue: MutableMap<Direction, MutableList<String>> = mutableMapOf()
    
    // Single traffic light for simplified access
    var trafficLight: TrafficLight? = null
        get() = trafficLights.values.firstOrNull()
    
    init {
        // Initialize queues for all directions
        Direction.values().forEach { dir ->
            vehicleQueue[dir] = mutableListOf()
        }
    }
    
    fun addToQueue(vehicleId: String, fromDirection: Direction) {
        vehicleQueue[fromDirection]?.add(vehicleId)
    }
    
    fun removeFromQueue(vehicleId: String, fromDirection: Direction) {
        vehicleQueue[fromDirection]?.remove(vehicleId)
    }
    
    fun getQueueLength(direction: Direction): Int {
        return vehicleQueue[direction]?.size ?: 0
    }
    
    fun getTotalQueueLength(): Int {
        return vehicleQueue.values.sumOf { it.size }
    }
    
    fun canVehiclePass(fromDirection: Direction): Boolean {
        if (!isSignalized) return true // Stop sign - always can eventually pass
        
        val light = trafficLights[fromDirection]
        return light?.canVehiclePass() ?: true
    }
    
    fun updateTrafficLights(deltaTime: Float, isAIControlled: Boolean = false, predictions: Map<Direction, Float> = emptyMap()) {
        trafficLights.forEach { (direction, light) ->
            val queueLength = getQueueLength(direction)
            val predictedCongestion = predictions[direction] ?: 0f
            light.update(deltaTime, queueLength, predictedCongestion, isAIControlled)
        }
    }
    
    fun getAverageWaitTime(): Float {
        val totalVehicles = getTotalQueueLength()
        if (totalVehicles == 0) return 0f
        
        // Estimate based on queue length and light cycles
        return totalVehicles * 2.5f // rough estimate: 2.5 seconds per vehicle
    }
}
