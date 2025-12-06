package org.aurora.rider.simulation

import org.aurora.rider.model.*
import org.aurora.traffic.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Orchestration strategies for RiderOS
 */
enum class RiderStrategy {
    BASELINE,      // Standard routing (like Google Maps)
    RIDER_OS,      // Smart rider-aware routing
    AURORA_AI      // Full AI optimization with predictions
}

/**
 * Statistics for rider simulation
 */
data class RiderStatistics(
    val totalRiders: Int = 0,
    val activeRiders: Int = 0,
    val averageSpeed: Float = 0f,
    val averageWaitTime: Float = 0f,
    val totalHazardsAvoided: Int = 0,
    val totalNearMisses: Int = 0,
    val averageSafetyScore: Float = 100f,
    val averageStress: Float = 0f,
    val averageFatigue: Float = 0f,
    val timeSavedVsBaseline: Float = 0f,
    val riskReduction: Float = 0f,
    
    // By rider type
    val deliveryRiders: Int = 0,
    val commuters: Int = 0,
    val eBikes: Int = 0,
    val scooters: Int = 0,
    val personalMotorcycles: Int = 0
)

/**
 * RiderOS Simulation Engine
 */
class RiderSimulationEngine(val cityMap: RiderCityMap) {
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    
    private val _strategy = MutableStateFlow(RiderStrategy.BASELINE)
    val strategy: StateFlow<RiderStrategy> = _strategy.asStateFlow()
    
    private val _statistics = MutableStateFlow(RiderStatistics())
    val statistics: StateFlow<RiderStatistics> = _statistics.asStateFlow()
    
    private val _fps = MutableStateFlow(0)
    val fps: StateFlow<Int> = _fps.asStateFlow()
    
    private val _timeOfDay = MutableStateFlow(12f) // 0-24 hours
    val timeOfDay: StateFlow<Float> = _timeOfDay.asStateFlow()
    
    private val _weather = MutableStateFlow(Weather.CLEAR)
    val weather: StateFlow<Weather> = _weather.asStateFlow()
    
    private var frameCount = 0
    private var fpsTimer = 0f
    private var simulationTime = 0f
    
    fun start() {
        _isRunning.value = true
    }
    
    fun pause() {
        _isRunning.value = false
    }
    
    fun reset() {
        _isRunning.value = false
        simulationTime = 0f
        frameCount = 0
        cityMap.riders.clear()
        cityMap.spawnRiders(30)
        updateStatistics()
    }
    
    fun setStrategy(newStrategy: RiderStrategy) {
        _strategy.value = newStrategy
        // Re-route riders based on new strategy
        rerouteAllRiders(newStrategy)
    }
    
    fun setTimeOfDay(hours: Float) {
        _timeOfDay.value = hours.coerceIn(0f, 24f)
    }
    
    fun setWeather(newWeather: Weather) {
        _weather.value = newWeather
    }
    
    fun update(deltaTime: Float = 1f / 60f) {
        if (!_isRunning.value) return
        
        simulationTime += deltaTime
        
        // Update time of day (1 second = 1 minute simulation)
        _timeOfDay.value = (_timeOfDay.value + deltaTime / 60f) % 24f
        
        // Update FPS counter
        frameCount++
        fpsTimer += deltaTime
        if (fpsTimer >= 1f) {
            _fps.value = frameCount
            frameCount = 0
            fpsTimer = 0f
        }
        
        // Update traffic lights
        cityMap.intersections.values.forEach { intersection ->
            intersection.trafficLight?.update(deltaTime)
        }
        
        // Update all riders
        val nearbyRidersCache = mutableMapOf<String, List<Rider>>()
        
        cityMap.riders.values.forEach { rider ->
            if (!rider.hasReachedDestination) {
                // Get nearby riders for this rider
                val nearbyRiders = nearbyRidersCache.getOrPut(rider.id) {
                    cityMap.riders.values.filter { other ->
                        other.id != rider.id && 
                        !other.hasReachedDestination &&
                        rider.position.distanceTo(other.position) < 100f
                    }
                }
                
                rider.update(
                    deltaTime = deltaTime,
                    roads = cityMap.roads,
                    intersections = cityMap.intersections,
                    nearbyRiders = nearbyRiders,
                    hazardDetector = cityMap.hazardDetector,
                    timeOfDay = _timeOfDay.value
                )
            }
        }
        
        // Update congestion levels
        updateCongestion()
        
        // Update statistics
        updateStatistics()
        
        // Respawn riders that reached destination
        respawnCompletedRiders()
    }
    
    private fun updateCongestion() {
        cityMap.roads.values.forEach { road ->
            val ridersOnRoad = cityMap.riders.values.count { 
                it.currentRoad == road.id && !it.hasReachedDestination 
            }
            road.updateCongestion(ridersOnRoad)
        }
    }
    
    private fun updateStatistics() {
        val activeRiders = cityMap.riders.values.filter { !it.hasReachedDestination }
        
        if (activeRiders.isEmpty()) {
            _statistics.value = RiderStatistics()
            return
        }
        
        val avgSpeed = activeRiders.map { it.speed }.average().toFloat()
        val avgWaitTime = activeRiders.map { it.waitTime }.average().toFloat()
        val totalHazards = activeRiders.sumOf { it.state.hazardsAvoided }
        val totalNearMisses = activeRiders.sumOf { it.nearMissCount }
        val avgSafety = activeRiders.map { it.state.safetyScore }.average().toFloat()
        val avgStress = activeRiders.map { it.state.stress }.average().toFloat()
        val avgFatigue = activeRiders.map { it.state.currentFatigue }.average().toFloat()
        val totalTimeSaved = activeRiders.sumOf { it.state.timeSaved.toDouble() }.toFloat()
        
        // Count by type
        val byType = cityMap.riders.values.groupBy { it.type }
        
        _statistics.value = RiderStatistics(
            totalRiders = cityMap.riders.size,
            activeRiders = activeRiders.size,
            averageSpeed = avgSpeed,
            averageWaitTime = avgWaitTime,
            totalHazardsAvoided = totalHazards,
            totalNearMisses = totalNearMisses,
            averageSafetyScore = avgSafety,
            averageStress = avgStress,
            averageFatigue = avgFatigue,
            timeSavedVsBaseline = totalTimeSaved,
            riskReduction = calculateRiskReduction(),
            deliveryRiders = byType[RiderType.DELIVERY_RIDER]?.size ?: 0,
            commuters = byType[RiderType.COMMUTER]?.size ?: 0,
            eBikes = byType[RiderType.E_BIKE]?.size ?: 0,
            scooters = byType[RiderType.SCOOTER]?.size ?: 0,
            personalMotorcycles = byType[RiderType.PERSONAL_MOTORCYCLE]?.size ?: 0
        )
    }
    
    private fun calculateRiskReduction(): Float {
        // Simplified calculation: based on hazards avoided and safety scores
        val riders = cityMap.riders.values.filter { !it.hasReachedDestination }
        if (riders.isEmpty()) return 0f
        
        val hazardAvoidanceRate = riders.sumOf { it.state.hazardsAvoided } / riders.size.toFloat()
        val avgSafety = riders.map { it.state.safetyScore }.average().toFloat()
        
        return (hazardAvoidanceRate * 10f + avgSafety / 10f).coerceIn(0f, 100f)
    }
    
    private fun respawnCompletedRiders() {
        val completed = cityMap.riders.values.filter { it.hasReachedDestination }
        
        completed.forEach { oldRider ->
            val startIntersection = cityMap.intersections.keys.random()
            var endIntersection = cityMap.intersections.keys.random()
            while (endIntersection == startIntersection) {
                endIntersection = cityMap.intersections.keys.random()
            }
            
            val route = when (_strategy.value) {
                RiderStrategy.BASELINE -> cityMap.findPath(startIntersection, endIntersection)
                RiderStrategy.RIDER_OS -> cityMap.findRiderPath(
                    startIntersection, 
                    endIntersection, 
                    oldRider.profile, 
                    oldRider.type
                )
                RiderStrategy.AURORA_AI -> cityMap.findRiderPath(
                    startIntersection,
                    endIntersection,
                    oldRider.profile,
                    oldRider.type
                )
            }
            
            val startPos = cityMap.intersections[startIntersection]!!.position.copy()
            
            val newRider = oldRider.copy(
                position = startPos,
                destination = endIntersection,
                route = route,
                routeIndex = 0,
                speed = 0f,
                currentRoad = null,
                targetIntersection = null,
                isWaiting = false,
                waitTime = 0f,
                hasReachedDestination = false,
                isRerouted = false,
                state = RiderState(),
                totalDistance = 0f,
                totalTime = 0f,
                nearMissCount = 0
            )
            
            cityMap.riders[newRider.id] = newRider
        }
    }
    
    private fun rerouteAllRiders(strategy: RiderStrategy) {
        cityMap.riders.values.forEach { rider ->
            if (!rider.hasReachedDestination && rider.route.isNotEmpty()) {
                val currentIntersection = rider.targetIntersection ?: rider.route.firstOrNull()?.let { roadId ->
                    cityMap.roads[roadId]?.startIntersection
                } ?: return@forEach
                
                val newRoute = when (strategy) {
                    RiderStrategy.BASELINE -> cityMap.findPath(currentIntersection, rider.destination)
                    RiderStrategy.RIDER_OS, RiderStrategy.AURORA_AI -> cityMap.findRiderPath(
                        currentIntersection,
                        rider.destination,
                        rider.profile,
                        rider.type
                    )
                }
                
                if (newRoute.isNotEmpty()) {
                    rider.route = newRoute
                    rider.routeIndex = 0
                    rider.isRerouted = true
                }
            }
        }
    }
}

/**
 * Weather conditions affecting riders
 */
enum class Weather {
    CLEAR,
    LIGHT_RAIN,
    HEAVY_RAIN,
    NIGHT
}
