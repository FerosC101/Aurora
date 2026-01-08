package com.nextcs.aurora.analytics

import android.location.Location
import kotlin.math.abs

data class DrivingBehavior(
    val harshBrakingCount: Int = 0,
    val rapidAccelerationCount: Int = 0,
    val speedingIncidents: Int = 0,
    val smoothDrivingScore: Int = 100
)

class DrivingBehaviorAnalyzer {
    
    private var previousSpeed: Float = 0f
    private var previousLocation: Location? = null
    private var harshBrakingCount = 0
    private var rapidAccelerationCount = 0
    private var speedingIncidents = 0
    private val speedHistory = mutableListOf<Float>()
    
    companion object {
        private const val HARSH_BRAKING_THRESHOLD = 5f // m/s² deceleration
        private const val RAPID_ACCELERATION_THRESHOLD = 4f // m/s² acceleration
        private const val SPEEDING_THRESHOLD = 10 // km/h over limit
    }
    
    /**
     * Analyze driving behavior based on location and speed changes
     */
    fun analyzeBehavior(
        currentLocation: Location,
        currentSpeed: Float, // km/h
        speedLimit: Int? = null
    ) {
        val speedMs = currentSpeed / 3.6f // Convert to m/s
        
        // Check for speeding
        speedLimit?.let { limit ->
            if (currentSpeed > limit + SPEEDING_THRESHOLD) {
                speedingIncidents++
            }
        }
        
        // Check for harsh braking or rapid acceleration
        if (previousSpeed > 0) {
            previousLocation?.let { prevLoc ->
                val timeDiff = (currentLocation.time - prevLoc.time) / 1000f // seconds
                if (timeDiff > 0 && timeDiff < 5) { // Only analyze if time diff is reasonable
                    val speedDiff = speedMs - (previousSpeed / 3.6f)
                    val acceleration = speedDiff / timeDiff
                    
                    when {
                        acceleration < -HARSH_BRAKING_THRESHOLD -> {
                            harshBrakingCount++
                        }
                        acceleration > RAPID_ACCELERATION_THRESHOLD -> {
                            rapidAccelerationCount++
                        }
                    }
                }
            }
        }
        
        // Track speed for smoothness calculation
        speedHistory.add(currentSpeed)
        if (speedHistory.size > 100) {
            speedHistory.removeAt(0)
        }
        
        previousSpeed = currentSpeed
        previousLocation = currentLocation
    }
    
    /**
     * Calculate smooth driving score based on collected data
     */
    fun calculateSmoothDrivingScore(): Int {
        if (speedHistory.isEmpty()) return 100
        
        // Base score
        var score = 100
        
        // Deduct points for incidents
        score -= harshBrakingCount * 5
        score -= rapidAccelerationCount * 3
        score -= speedingIncidents * 2
        
        // Deduct points for speed variance (jerky driving)
        if (speedHistory.size > 10) {
            val average = speedHistory.average()
            val variance = speedHistory.map { (it - average) * (it - average) }.average()
            val stdDev = kotlin.math.sqrt(variance)
            
            // Higher standard deviation means less smooth driving
            score -= (stdDev / 2).toInt().coerceAtMost(20)
        }
        
        return score.coerceIn(0, 100)
    }
    
    /**
     * Get current driving behavior metrics
     */
    fun getBehavior(): DrivingBehavior {
        return DrivingBehavior(
            harshBrakingCount = harshBrakingCount,
            rapidAccelerationCount = rapidAccelerationCount,
            speedingIncidents = speedingIncidents,
            smoothDrivingScore = calculateSmoothDrivingScore()
        )
    }
    
    /**
     * Reset behavior tracking for new trip
     */
    fun reset() {
        harshBrakingCount = 0
        rapidAccelerationCount = 0
        speedingIncidents = 0
        speedHistory.clear()
        previousSpeed = 0f
        previousLocation = null
    }
}
