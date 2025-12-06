package org.aurora.rider.model

/**
 * Different types of riders in Aurora RiderOS
 */
enum class RiderType {
    DELIVERY_RIDER,      // Grab, Foodpanda, Lalamove
    COMMUTER,            // Regular motorcycle commuter
    E_BIKE,              // Electric bike rider
    SCOOTER,             // Scooter rider
    PERSONAL_MOTORCYCLE  // Personal motorcycle
}

/**
 * Rider-specific attributes that influence behavior
 */
data class RiderProfile(
    val riskTolerance: Float = 0.5f,        // 0.0 (very cautious) to 1.0 (aggressive)
    val preferredSpeed: Float = 40f,         // km/h
    val batteryLevel: Float = 100f,          // 0-100% (for e-bikes/scooters)
    val avoidHighways: Boolean = false,
    val rainAvoidance: Boolean = true,
    val shortcutPreference: Float = 0.7f,    // 0.0 (main roads) to 1.0 (aggressive shortcuts)
    val fatigueRate: Float = 0.01f,          // How fast rider gets tired
    val nightRiding: Boolean = true,         // Comfortable riding at night
    val experienceLevel: Float = 0.8f        // 0.0 (novice) to 1.0 (expert)
)

/**
 * Dynamic rider state that changes during simulation
 */
data class RiderState(
    var currentFatigue: Float = 0f,          // 0.0 (fresh) to 1.0 (exhausted)
    var stress: Float = 0f,                  // 0.0 (calm) to 1.0 (very stressed)
    var batteryUsed: Float = 0f,             // Battery consumption for e-vehicles
    var timeSaved: Float = 0f,               // Time saved vs baseline route
    var hazardsAvoided: Int = 0,             // Number of hazards successfully avoided
    var riskyManeuvers: Int = 0,             // Count of risky decisions made
    var safetyScore: Float = 100f            // 0-100 safety rating
)
