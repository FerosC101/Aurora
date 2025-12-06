package org.aurora.ai

import org.aurora.traffic.model.CityMap
import org.aurora.traffic.model.Direction
import org.aurora.simulation.OrchestrationStrategy

/**
 * Aurora's Multi-Agent Orchestration Layer
 * Tests different strategies and optimizes traffic flow
 */
class TrafficOrchestrator(
    private val cityMap: CityMap,
    private val predictor: CongestionPredictor
) {
    private var rerouteCounter = 0
    
    fun orchestrate(strategy: OrchestrationStrategy) {
        when (strategy) {
            OrchestrationStrategy.DEFAULT -> {
                // Do nothing - lights use fixed timers
            }
            OrchestrationStrategy.PREDICTIVE -> {
                applyPredictiveAdjustments()
            }
            OrchestrationStrategy.AURORA -> {
                applyFullOptimization()
            }
        }
    }
    
    private fun applyPredictiveAdjustments() {
        val predictions = predictor.predict(cityMap)
        
        // Adjust traffic lights based on predictions
        cityMap.intersections.values.forEach { intersection ->
            val directionPredictions = mutableMapOf<Direction, Float>()
            
            // Get predictions for roads entering this intersection
            Direction.values().forEach { direction ->
                val incomingRoads = cityMap.roads.values.filter { 
                    it.endIntersection == intersection.id && it.direction == direction 
                }
                
                if (incomingRoads.isNotEmpty()) {
                    val avgPrediction = incomingRoads
                        .mapNotNull { predictions[it.id]?.predicted20s }
                        .average()
                        .toFloat()
                    directionPredictions[direction] = avgPrediction / 100f // Normalize to 0-1
                }
            }
            
            // Update traffic lights with predictions
            intersection.trafficLights.forEach { (direction, light) ->
                val predictedCongestion = directionPredictions[direction] ?: 0f
                val queueLength = intersection.getQueueLength(direction)
                
                // Adjust green duration
                if (predictedCongestion > 0.4f || queueLength > 3) {
                    light.greenDuration = (light.greenDuration * 1.3f).coerceIn(10f, 35f)
                } else if (predictedCongestion < 0.2f && queueLength < 2) {
                    light.greenDuration = (light.greenDuration * 0.9f).coerceIn(8f, 20f)
                }
            }
        }
    }
    
    private fun applyFullOptimization() {
        // First, apply predictive adjustments
        applyPredictiveAdjustments()
        
        // Then, apply advanced optimizations
        val predictions = predictor.predict(cityMap)
        
        // 1. Dynamic lane priority
        optimizeLanePriority(predictions)
        
        // 2. Intelligent rerouting
        if (rerouteCounter++ % 60 == 0) { // Every 60 frames (~1 second)
            performIntelligentRerouting(predictions)
        }
        
        // 3. Green wave coordination
        coordinateGreenWaves()
        
        // 4. Emergency congestion relief
        applyEmergencyCongestionRelief(predictions)
    }
    
    private fun optimizeLanePriority(predictions: Map<String, CongestionPredictor.Prediction>) {
        cityMap.intersections.values.forEach { intersection ->
            val queueLengths = Direction.values().associateWith { 
                intersection.getQueueLength(it) 
            }
            
            // Find most congested direction
            val maxQueue = queueLengths.maxByOrNull { it.value }
            if (maxQueue != null && maxQueue.value > 5) {
                // Give priority to most congested direction
                intersection.trafficLights[maxQueue.key]?.let { light ->
                    light.greenDuration = 30f
                    light.redDuration = 10f
                }
                
                // Reduce time for others
                queueLengths.filter { it.key != maxQueue.key }.forEach { (direction, _) ->
                    intersection.trafficLights[direction]?.let { light ->
                        light.greenDuration = 12f
                        light.redDuration = 25f
                    }
                }
            }
        }
    }
    
    private fun performIntelligentRerouting(predictions: Map<String, CongestionPredictor.Prediction>) {
        val congestedRoads = predictions.values
            .filter { it.predicted20s > 40f }
            .map { it.roadId }
            .toSet()
        
        if (congestedRoads.isEmpty()) return
        
        // Find vehicles heading toward congested roads
        cityMap.vehicles.values
            .filter { !it.hasReachedDestination && it.route.any { roadId -> roadId in congestedRoads } }
            .take(5) // Limit reroutes per cycle
            .forEach { vehicle ->
                // Calculate alternative route
                val currentIntersection = vehicle.currentRoad?.let { roadId ->
                    cityMap.roads[roadId]?.endIntersection
                } ?: return@forEach
                
                val alternativeRoute = cityMap.findShortestPath(currentIntersection, vehicle.destination)
                
                // Only reroute if alternative avoids congestion
                val hasCongestedRoads = alternativeRoute.any { it in congestedRoads }
                if (!hasCongestedRoads && alternativeRoute.isNotEmpty()) {
                    vehicle.updateRoute(alternativeRoute)
                }
            }
    }
    
    private fun coordinateGreenWaves() {
        // Create green wave on main corridors (horizontal roads in top row)
        val mainCorridorIntersections = listOf("I00", "I01", "I02")
        
        var offset = 0f
        mainCorridorIntersections.forEach { intersectionId ->
            val intersection = cityMap.intersections[intersectionId] ?: return@forEach
            
            // Synchronize east-west lights
            intersection.trafficLights[Direction.EAST]?.let { light ->
                light.remainingTime = (15f - offset).coerceAtLeast(1f)
            }
            intersection.trafficLights[Direction.WEST]?.let { light ->
                light.remainingTime = (15f - offset).coerceAtLeast(1f)
            }
            
            offset += 5f // 5 second offset between intersections
        }
    }
    
    private fun applyEmergencyCongestionRelief(predictions: Map<String, CongestionPredictor.Prediction>) {
        // Find gridlocked intersections
        val gridlockedIntersections = cityMap.intersections.values.filter { 
            it.getTotalQueueLength() > 15 
        }
        
        gridlockedIntersections.forEach { intersection ->
            // Implement "all clear" cycle - quickly clear one direction at a time
            Direction.values().forEach { direction ->
                val queueLength = intersection.getQueueLength(direction)
                if (queueLength > 5) {
                    intersection.trafficLights[direction]?.let { light ->
                        light.greenDuration = 40f // Extra long green
                        light.redDuration = 5f // Short red
                    }
                }
            }
        }
    }
    
    fun getOptimizationSummary(strategy: OrchestrationStrategy): String {
        return when (strategy) {
            OrchestrationStrategy.DEFAULT -> "🔴 Fixed timers • No optimization"
            OrchestrationStrategy.PREDICTIVE -> "🟡 Predictive adjustments • Light optimization"
            OrchestrationStrategy.AURORA -> "🟢 Full Aurora • Multi-agent optimization active"
        }
    }
}
