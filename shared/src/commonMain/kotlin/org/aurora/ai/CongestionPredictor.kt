package org.aurora.ai

import org.aurora.traffic.model.CityMap
import org.aurora.traffic.model.CongestionLevel
import org.aurora.traffic.model.Road

/**
 * Simplified ST-GNN inspired predictor
 * Uses rule-based system to simulate ML predictions
 */
class CongestionPredictor {
    private val historicalData = mutableMapOf<String, MutableList<Float>>()
    private val maxHistorySize = 20
    
    data class Prediction(
        val roadId: String,
        val currentCongestion: Float,
        val predicted10s: Float,
        val predicted20s: Float,
        val predicted30s: Float,
        val confidence: Float
    )
    
    fun predict(cityMap: CityMap): Map<String, Prediction> {
        val predictions = mutableMapOf<String, Prediction>()
        
        cityMap.roads.values.forEach { road ->
            // Update historical data
            if (historicalData[road.id] == null) {
                historicalData[road.id] = mutableListOf()
            }
            val history = historicalData[road.id]!!
            history.add(road.currentDensity)
            if (history.size > maxHistorySize) {
                history.removeFirst()
            }
            
            // Calculate predictions
            val current = road.currentDensity
            val trend = calculateTrend(history)
            val neighborInfluence = calculateNeighborInfluence(road, cityMap)
            val queueInfluence = calculateQueueInfluence(road, cityMap)
            
            // Predict future congestion
            val pred10s = (current + trend * 10 + neighborInfluence * 0.3f + queueInfluence * 0.2f)
                .coerceIn(0f, 100f)
            val pred20s = (current + trend * 20 + neighborInfluence * 0.5f + queueInfluence * 0.4f)
                .coerceIn(0f, 100f)
            val pred30s = (current + trend * 30 + neighborInfluence * 0.7f + queueInfluence * 0.6f)
                .coerceIn(0f, 100f)
            
            val confidence = calculateConfidence(history)
            
            predictions[road.id] = Prediction(
                roadId = road.id,
                currentCongestion = current,
                predicted10s = pred10s,
                predicted20s = pred20s,
                predicted30s = pred30s,
                confidence = confidence
            )
        }
        
        return predictions
    }
    
    private fun calculateTrend(history: List<Float>): Float {
        if (history.size < 3) return 0f
        
        // Simple linear regression slope
        val n = history.size
        val x = history.indices.map { it.toFloat() }
        val y = history
        
        val sumX = x.sum()
        val sumY = y.sum()
        val sumXY = x.zip(y).sumOf { (xi, yi) -> (xi * yi).toDouble() }.toFloat()
        val sumX2 = x.sumOf { (it * it).toDouble() }.toFloat()
        
        val slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX)
        return slope
    }
    
    private fun calculateNeighborInfluence(road: Road, cityMap: CityMap): Float {
        // Check connected roads at start and end intersections
        val startIntersection = cityMap.intersections[road.startIntersection]
        val endIntersection = cityMap.intersections[road.endIntersection]
        
        val neighborRoads = (startIntersection?.connectedRoads.orEmpty() + 
                            endIntersection?.connectedRoads.orEmpty())
            .mapNotNull { cityMap.roads[it] }
            .filter { it.id != road.id }
        
        if (neighborRoads.isEmpty()) return 0f
        
        return neighborRoads.map { it.currentDensity }.average().toFloat()
    }
    
    private fun calculateQueueInfluence(road: Road, cityMap: CityMap): Float {
        val endIntersection = cityMap.intersections[road.endIntersection] ?: return 0f
        val queueLength = endIntersection.getQueueLength(road.direction)
        
        // Queue adds to congestion
        return queueLength * 2f
    }
    
    private fun calculateConfidence(history: List<Float>): Float {
        if (history.size < 5) return 0.5f
        
        // Calculate variance - lower variance = higher confidence
        val mean = history.average().toFloat()
        val variance = history.map { (it - mean) * (it - mean) }.average().toFloat()
        
        // Normalize to 0-1 range
        return (1f / (1f + variance / 10f)).coerceIn(0.3f, 1f)
    }
    
    fun getSummary(predictions: Map<String, Prediction>): String {
        val highCongestionRoads = predictions.values.count { it.predicted20s > 40 }
        val moderateCongestionRoads = predictions.values.count { it.predicted20s in 20f..40f }
        
        return when {
            highCongestionRoads > predictions.size * 0.3 -> "⚠️ High congestion predicted in 20s"
            moderateCongestionRoads > predictions.size * 0.4 -> "⚡ Moderate congestion in 20s"
            else -> "✅ Traffic flow normal"
        }
    }
}
