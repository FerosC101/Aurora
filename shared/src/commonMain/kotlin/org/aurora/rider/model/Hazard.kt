package org.aurora.rider.model

/**
 * Types of hazards that riders face in real-world scenarios
 */
enum class HazardType {
    POTHOLE,              // Road damage
    FLOODED_AREA,         // Water accumulation
    ACCIDENT_PRONE_SPOT,  // Historical accident location
    STEEP_INCLINE,        // Dangerous for scooters/e-bikes
    POOR_LIGHTING,        // Unsafe at night
    NARROW_PASSAGE,       // Tight squeeze
    LOOSE_GRAVEL,         // Slippery surface
    CONSTRUCTION_ZONE,    // Road work
    HIGH_CRIME_AREA       // Safety concern
}

/**
 * Severity level of a hazard
 */
enum class HazardSeverity {
    LOW,      // Minor inconvenience
    MODERATE, // Noticeable risk
    HIGH,     // Serious danger
    CRITICAL  // Immediate threat
}

/**
 * Hazard that affects rider safety and route decisions
 */
data class Hazard(
    val id: String,
    val type: HazardType,
    val severity: HazardSeverity,
    val position: org.aurora.traffic.model.Position,
    val radius: Float = 20f,        // Affected area in meters
    val affectedRoad: String?,      // Road ID if hazard is on a road
    val description: String,
    val isActive: Boolean = true,   // Can be temporary (e.g., flooding)
    val reportedAt: Long = System.currentTimeMillis(),
    val verifiedReports: Int = 1    // Confidence level
) {
    /**
     * Calculate risk impact on a rider based on their profile
     */
    fun getRiskImpact(riderProfile: RiderProfile, riderType: RiderType): Float {
        val baseRisk = when (severity) {
            HazardSeverity.LOW -> 0.2f
            HazardSeverity.MODERATE -> 0.5f
            HazardSeverity.HIGH -> 0.8f
            HazardSeverity.CRITICAL -> 1.0f
        }
        
        // Type-specific multipliers
        val typeMultiplier = when {
            type == HazardType.STEEP_INCLINE && riderType in listOf(RiderType.SCOOTER, RiderType.E_BIKE) -> 1.5f
            type == HazardType.POOR_LIGHTING && !riderProfile.nightRiding -> 1.3f
            type == HazardType.FLOODED_AREA && riderType == RiderType.E_BIKE -> 1.4f
            else -> 1.0f
        }
        
        // Risk tolerance adjustment
        val riskAdjustment = 1.0f - (riderProfile.riskTolerance * 0.3f)
        
        return (baseRisk * typeMultiplier * riskAdjustment).coerceIn(0f, 1f)
    }
    
    /**
     * Check if hazard affects a specific position
     */
    fun affectsPosition(pos: org.aurora.traffic.model.Position): Boolean {
        return isActive && position.distanceTo(pos) <= radius
    }
    
    /**
     * Speed reduction factor when passing through hazard
     */
    fun getSpeedReduction(): Float {
        return when (severity) {
            HazardSeverity.LOW -> 0.9f      // 10% slower
            HazardSeverity.MODERATE -> 0.7f // 30% slower
            HazardSeverity.HIGH -> 0.5f     // 50% slower
            HazardSeverity.CRITICAL -> 0.2f // 80% slower
        }
    }
}

/**
 * Aurora SHIELD - Hazard detection and warning system
 */
class HazardDetector {
    private val hazards = mutableMapOf<String, Hazard>()
    
    fun addHazard(hazard: Hazard) {
        hazards[hazard.id] = hazard
    }
    
    fun removeHazard(hazardId: String) {
        hazards.remove(hazardId)
    }
    
    fun getActiveHazards(): List<Hazard> {
        return hazards.values.filter { it.isActive }
    }
    
    /**
     * Find hazards near a position
     */
    fun getHazardsNear(position: org.aurora.traffic.model.Position, searchRadius: Float = 100f): List<Hazard> {
        return getActiveHazards().filter { 
            it.position.distanceTo(position) <= searchRadius 
        }
    }
    
    /**
     * Find hazards along a road
     */
    fun getHazardsOnRoad(roadId: String): List<Hazard> {
        return getActiveHazards().filter { it.affectedRoad == roadId }
    }
    
    /**
     * Calculate total hazard risk for a path
     */
    fun calculatePathRisk(
        path: List<String>,
        roads: Map<String, org.aurora.traffic.model.Road>,
        riderProfile: RiderProfile,
        riderType: RiderType
    ): Float {
        var totalRisk = 0f
        var roadCount = 0
        
        path.forEach { roadId ->
            val roadHazards = getHazardsOnRoad(roadId)
            if (roadHazards.isNotEmpty()) {
                val roadRisk = roadHazards.sumOf { 
                    it.getRiskImpact(riderProfile, riderType).toDouble() 
                }.toFloat()
                totalRisk += roadRisk
                roadCount++
            }
        }
        
        return if (roadCount > 0) totalRisk / roadCount else 0f
    }
    
    /**
     * Generate warning message for nearby hazards
     */
    fun generateWarning(hazard: Hazard): String {
        val severityText = when (hazard.severity) {
            HazardSeverity.LOW -> "Caution"
            HazardSeverity.MODERATE -> "Warning"
            HazardSeverity.HIGH -> "Danger"
            HazardSeverity.CRITICAL -> "CRITICAL"
        }
        
        return "$severityText: ${hazard.description}"
    }
}
