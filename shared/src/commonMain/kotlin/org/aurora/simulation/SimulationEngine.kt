package org.aurora.simulation

import org.aurora.traffic.model.CityMap
import org.aurora.traffic.model.Vehicle
import org.aurora.ai.CongestionPredictor
import org.aurora.ai.TrafficOrchestrator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class OrchestrationStrategy {
    DEFAULT,      // Fixed timers, no intelligence
    PREDICTIVE,   // Predictions adjust lights
    AURORA        // Full multi-agent optimization
}

class SimulationEngine(val cityMap: CityMap) {
    private val predictor = CongestionPredictor()
    private val orchestrator = TrafficOrchestrator(cityMap, predictor)
    
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    
    private val _simulationTime = MutableStateFlow(0f)
    val simulationTime: StateFlow<Float> = _simulationTime.asStateFlow()
    
    private val _strategy = MutableStateFlow(OrchestrationStrategy.DEFAULT)
    val strategy: StateFlow<OrchestrationStrategy> = _strategy.asStateFlow()
    
    private val _fps = MutableStateFlow(0)
    val fps: StateFlow<Int> = _fps.asStateFlow()
    
    private val _statistics = MutableStateFlow(SimulationStatistics())
    val statistics: StateFlow<SimulationStatistics> = _statistics.asStateFlow()
    
    private var lastUpdateTime = 0L
    private var frameCount = 0
    private var fpsTimer = 0f
    
    fun start() {
        _isRunning.value = true
        lastUpdateTime = System.currentTimeMillis()
    }
    
    fun pause() {
        _isRunning.value = false
    }
    
    fun reset() {
        _isRunning.value = false
        _simulationTime.value = 0f
        cityMap.vehicles.clear()
        cityMap.vehicles.clear()
        cityMap.updateCongestionLevels()
    }
    
    fun setStrategy(newStrategy: OrchestrationStrategy) {
        _strategy.value = newStrategy
    }
    
    fun update() {
        if (!_isRunning.value) return
        
        val currentTime = System.currentTimeMillis()
        val deltaTime = ((currentTime - lastUpdateTime) / 1000f).coerceAtMost(0.1f) // Max 100ms to prevent huge jumps
        lastUpdateTime = currentTime
        
        _simulationTime.value += deltaTime
        
        // Update FPS counter
        frameCount++
        fpsTimer += deltaTime
        if (fpsTimer >= 1f) {
            _fps.value = frameCount
            frameCount = 0
            fpsTimer = 0f
        }
        
        // Update all vehicles
        val vehicleList = cityMap.vehicles.values.toList()
        vehicleList.forEach { vehicle ->
            if (!vehicle.hasReachedDestination) {
                val nearbyVehicles = cityMap.getVehiclesNear(vehicle.position, 50f)
                vehicle.update(deltaTime, cityMap.roads, cityMap.intersections, nearbyVehicles)
            }
        }
        
        // Remove vehicles that reached destination and spawn new ones
        val reachedDestination = cityMap.vehicles.values.count { it.hasReachedDestination }
        if (reachedDestination > 0) {
            cityMap.vehicles.values.removeIf { it.hasReachedDestination }
            cityMap.spawnVehicles(reachedDestination)
        }
        
        // Update traffic lights based on strategy
        orchestrator.orchestrate(_strategy.value)
        val isAIControlled = _strategy.value != OrchestrationStrategy.DEFAULT
        cityMap.intersections.values.forEach { intersection ->
            intersection.updateTrafficLights(deltaTime, isAIControlled)
        }
        
        // Update congestion levels
        cityMap.updateCongestionLevels()
        
        // Update statistics
        updateStatistics()
    }
    
    private fun updateStatistics() {
        val activeVehicles = cityMap.vehicles.values.filter { !it.hasReachedDestination }
        
        val avgSpeed = if (activeVehicles.isNotEmpty()) {
            activeVehicles.sumOf { it.speed.toDouble() }.toFloat() / activeVehicles.size
        } else 0f
        
        val totalWaitTime = activeVehicles.sumOf { it.waitTime.toDouble() }.toFloat()
        val waitingVehicles = activeVehicles.count { it.isWaiting }
        
        val totalQueueLength = cityMap.intersections.values.sumOf { it.getTotalQueueLength() }
        
        val congestionLevels = cityMap.roads.values.groupingBy { it.congestionLevel }.eachCount()
        
        _statistics.value = SimulationStatistics(
            totalVehicles = cityMap.vehicles.size,
            activeVehicles = activeVehicles.size,
            averageSpeed = avgSpeed,
            totalWaitTime = totalWaitTime,
            waitingVehicles = waitingVehicles,
            totalQueueLength = totalQueueLength,
            freeRoads = congestionLevels[org.aurora.traffic.model.CongestionLevel.FREE] ?: 0,
            moderateRoads = congestionLevels[org.aurora.traffic.model.CongestionLevel.MODERATE] ?: 0,
            heavyRoads = congestionLevels[org.aurora.traffic.model.CongestionLevel.HEAVY] ?: 0,
            gridlockedRoads = congestionLevels[org.aurora.traffic.model.CongestionLevel.GRIDLOCK] ?: 0
        )
    }
}

data class SimulationStatistics(
    val totalVehicles: Int = 0,
    val activeVehicles: Int = 0,
    val averageSpeed: Float = 0f,
    val totalWaitTime: Float = 0f,
    val waitingVehicles: Int = 0,
    val totalQueueLength: Int = 0,
    val freeRoads: Int = 0,
    val moderateRoads: Int = 0,
    val heavyRoads: Int = 0,
    val gridlockedRoads: Int = 0
)
