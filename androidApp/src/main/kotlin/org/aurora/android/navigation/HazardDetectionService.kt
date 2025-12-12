package org.aurora.android.navigation

import com.google.android.gms.maps.model.LatLng

enum class HazardSeverity {
    LOW,      // Yellow
    MODERATE, // Orange
    HIGH,     // Red
    CRITICAL  // Dark Red
}

data class DetectedHazard(
    val type: String,           // "construction", "pothole", "flooding", "accident"
    val description: String,
    val severity: HazardSeverity,
    val location: LatLng,
    val distanceFromStart: Int, // in meters
    val instruction: String     // the original turn instruction containing hazard
)

class HazardDetectionService {
    
    private val hazardKeywords = mapOf(
        "construction" to listOf(
            "construction", "construction zone", "road work", "under construction",
            "roadwork", "maintenance", "repair work", "construction area"
        ),
        "pothole" to listOf(
            "pothole", "potholes", "rough road", "damaged road", "road damage",
            "poor road condition", "uneven surface"
        ),
        "flooding" to listOf(
            "flood", "flooding", "flooded", "water hazard", "submerged", "water level",
            "heavy rain", "waterlogged", "inundated"
        ),
        "accident" to listOf(
            "accident", "collision", "crash", "incident", "traffic incident",
            "road incident", "vehicle incident", "debris"
        )
    )
    
    fun detectHazards(steps: List<NavigationStep>): List<DetectedHazard> {
        val detectedHazards = mutableListOf<DetectedHazard>()
        var cumulativeDistance = 0
        
        for (step in steps) {
            val instruction = step.instruction.lowercase()
            cumulativeDistance += step.distance
            
            // Check each hazard type
            for ((hazardType, keywords) in hazardKeywords) {
                for (keyword in keywords) {
                    if (instruction.contains(keyword, ignoreCase = true)) {
                        val severity = determineSeverity(instruction, hazardType)
                        detectedHazards.add(
                            DetectedHazard(
                                type = hazardType,
                                description = generateHazardDescription(hazardType, instruction),
                                severity = severity,
                                location = step.endLocation,
                                distanceFromStart = cumulativeDistance,
                                instruction = step.instruction
                            )
                        )
                        break // Only one hazard per step
                    }
                }
            }
        }
        
        return detectedHazards.distinctBy { "${it.type}${it.distanceFromStart}" }
    }
    
    private fun determineSeverity(instruction: String, hazardType: String): HazardSeverity {
        val lowerInstruction = instruction.lowercase()
        
        return when {
            // Critical indicators
            lowerInstruction.contains("severe") || 
            lowerInstruction.contains("major") ||
            lowerInstruction.contains("heavy") -> HazardSeverity.CRITICAL
            
            // High severity indicators
            lowerInstruction.contains("closed") ||
            lowerInstruction.contains("blocked") ||
            (hazardType == "accident" && lowerInstruction.contains("multiple")) -> HazardSeverity.HIGH
            
            // Moderate severity indicators
            lowerInstruction.contains("caution") ||
            lowerInstruction.contains("watch") ||
            lowerInstruction.contains("avoid") -> HazardSeverity.MODERATE
            
            // Default based on hazard type
            hazardType == "construction" -> HazardSeverity.MODERATE
            hazardType == "pothole" -> HazardSeverity.LOW
            hazardType == "flooding" -> HazardSeverity.HIGH
            hazardType == "accident" -> HazardSeverity.HIGH
            
            else -> HazardSeverity.LOW
        }
    }
    
    private fun generateHazardDescription(hazardType: String, instruction: String): String {
        return when (hazardType) {
            "construction" -> "Road construction zone - expect delays and lane changes"
            "pothole" -> "Road surface damage detected - drive carefully"
            "flooding" -> "Water hazard ahead - consider alternative route"
            "accident" -> "Traffic incident detected - proceed with caution"
            else -> "Hazard detected - proceed carefully"
        }
    }
    
    fun getHazardColor(severity: HazardSeverity): String {
        return when (severity) {
            HazardSeverity.LOW -> "#FCD34D"      // Yellow
            HazardSeverity.MODERATE -> "#FB923C" // Orange
            HazardSeverity.HIGH -> "#EF4444"     // Red
            HazardSeverity.CRITICAL -> "#DC2626" // Dark Red
        }
    }
    
    fun getHazardEmoji(hazardType: String): String {
        return when (hazardType) {
            "construction" -> "🚧"
            "pothole" -> "🕳️"
            "flooding" -> "🌊"
            "accident" -> "🚨"
            else -> "⚠️"
        }
    }
    
    fun calculateSafetyScore(hazards: List<DetectedHazard>): Int {
        // Base score of 100
        var score = 100
        
        for (hazard in hazards) {
            score -= when (hazard.severity) {
                HazardSeverity.LOW -> 5
                HazardSeverity.MODERATE -> 15
                HazardSeverity.HIGH -> 25
                HazardSeverity.CRITICAL -> 40
            }
        }
        
        return score.coerceIn(0, 100)
    }
}
