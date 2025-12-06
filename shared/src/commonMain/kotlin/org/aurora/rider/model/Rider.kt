package org.aurora.rider.model

import org.aurora.traffic.model.Position
import org.aurora.traffic.model.Road
import org.aurora.traffic.model.Intersection
import kotlin.random.Random

/**
 * Rider-specific colors for visual identification
 */
enum class RiderColor {
    DELIVERY_ORANGE,    // Delivery riders
    COMMUTER_BLUE,      // Regular commuters
    EBIKE_GREEN,        // E-bike riders
    SCOOTER_PURPLE,     // Scooter riders
    PERSONAL_CYAN       // Personal motorcycles
}

/**
 * Rider agent - the core actor in Aurora RiderOS
 * Replaces the generic Vehicle with rider-specific intelligence
 */
data class Rider(
    val id: String,
    val type: RiderType,
    val profile: RiderProfile,
    var state: RiderState = RiderState(),
    var position: Position,
    var currentRoad: String? = null,
    var targetIntersection: String? = null,
    val destination: String,
    var speed: Float = 0f,
    val color: RiderColor = RiderColor.COMMUTER_BLUE,
    
    // Routing
    var route: List<String> = emptyList(),
    var routeIndex: Int = 0,
    var alternativeRoutes: List<List<String>> = emptyList(),
    
    // Behavior state
    var isWaiting: Boolean = false,
    var waitTime: Float = 0f,
    var hasReachedDestination: Boolean = false,
    var isRerouted: Boolean = false,
    var lastDecision: RiderDecision? = null,
    
    // Statistics
    var totalDistance: Float = 0f,
    var totalTime: Float = 0f,
    var nearMissCount: Int = 0
) {
    private var distanceOnRoad: Float = 0f
    
    /**
     * Main update loop for rider agent
     */
    fun update(
        deltaTime: Float,
        roads: Map<String, Road>,
        intersections: Map<String, Intersection>,
        nearbyRiders: List<Rider>,
        hazardDetector: HazardDetector,
        timeOfDay: Float = 12f // 0-24 hours
    ) {
        if (hasReachedDestination) return
        
        totalTime += deltaTime
        updateFatigue(deltaTime)
        updateBattery(deltaTime)
        
        // Check if we need a new route segment
        if (currentRoad == null && route.isNotEmpty() && routeIndex < route.size) {
            currentRoad = route[routeIndex]
            distanceOnRoad = 0f
            isWaiting = false
        }
        
        val road = currentRoad?.let { roads[it] } ?: return
        
        // Rider-specific hazard awareness
        val nearbyHazards = hazardDetector.getHazardsNear(position, 50f)
        val roadHazards = hazardDetector.getHazardsOnRoad(road.id)
        
        // Check for riders ahead (different from car behavior)
        val riderAhead = findRiderAhead(nearbyRiders, road)
        val safeDistance = calculateSafeDistance()
        
        // Intersection logic
        val intersection = targetIntersection?.let { intersections[it] }
        val canPassIntersection = intersection?.canVehiclePass(road.direction) ?: true
        
        // RIDER MICRO-DECISIONS
        val decision = makeRiderDecision(
            road = road,
            riderAhead = riderAhead,
            intersection = intersection,
            canPassIntersection = canPassIntersection,
            nearbyHazards = nearbyHazards,
            timeOfDay = timeOfDay
        )
        
        lastDecision = decision
        applyDecision(decision, deltaTime, road, riderAhead, intersection)
        
        // Update position
        if (speed > 0f) {
            val distanceMoved = speed * deltaTime * (1000f / 3600f) // km/h to m/s
            distanceOnRoad += distanceMoved
            totalDistance += distanceMoved
            
            val progress = (distanceOnRoad / road.length).coerceIn(0f, 1f)
            val start = road.start
            val end = road.end
            
            position = Position(
                x = start.x + (end.x - start.x) * progress,
                y = start.y + (end.y - start.y) * progress
            )
        }
        
        // Check if reached end of road
        if (distanceOnRoad >= road.length) {
            routeIndex++
            currentRoad = null
            targetIntersection = null
            
            if (routeIndex >= route.size) {
                hasReachedDestination = true
            }
        }
        
        // Update stress based on conditions
        updateStress(nearbyHazards, riderAhead, canPassIntersection, deltaTime)
    }
    
    /**
     * Rider-specific decision making engine
     */
    private fun makeRiderDecision(
        road: Road,
        riderAhead: Rider?,
        intersection: Intersection?,
        canPassIntersection: Boolean,
        nearbyHazards: List<Hazard>,
        timeOfDay: Float
    ): RiderDecision {
        // Night riding caution
        val isNight = timeOfDay < 6f || timeOfDay > 20f
        val nightFactor = if (isNight && !profile.nightRiding) 0.7f else 1.0f
        
        // Hazard response
        if (nearbyHazards.isNotEmpty()) {
            val criticalHazard = nearbyHazards.firstOrNull { 
                it.severity == HazardSeverity.CRITICAL 
            }
            if (criticalHazard != null) {
                state.hazardsAvoided++
                return RiderDecision.AVOID_HAZARD
            }
        }
        
        // Intersection decisions
        if (!canPassIntersection && distanceOnRoad > road.length * 0.8f) {
            return RiderDecision.STOP_AT_LIGHT
        }
        
        // Following behavior
        if (riderAhead != null) {
            val distance = riderAhead.position.distanceTo(position)
            val safeDistance = calculateSafeDistance()
            
            when {
                distance < safeDistance * 0.5f -> return RiderDecision.EMERGENCY_BRAKE
                distance < safeDistance -> {
                    // Riders can overtake more easily than cars
                    val canOvertake = profile.riskTolerance > 0.6f && 
                                    road.lanes >= 2 &&
                                    state.currentFatigue < 0.7f
                    return if (canOvertake) RiderDecision.OVERTAKE else RiderDecision.FOLLOW
                }
            }
        }
        
        // Normal acceleration with rider preferences
        val targetSpeed = (profile.preferredSpeed * nightFactor).coerceAtMost(road.speedLimit * 1.1f)
        return if (speed < targetSpeed) {
            RiderDecision.ACCELERATE
        } else {
            RiderDecision.CRUISE
        }
    }
    
    /**
     * Apply the decision to rider state
     */
    private fun applyDecision(
        decision: RiderDecision,
        deltaTime: Float,
        road: Road,
        riderAhead: Rider?,
        intersection: Intersection?
    ) {
        when (decision) {
            RiderDecision.ACCELERATE -> {
                val acceleration = 30f * (1f - state.currentFatigue)
                speed = (speed + acceleration * deltaTime).coerceAtMost(profile.preferredSpeed)
                isWaiting = false
            }
            RiderDecision.CRUISE -> {
                // Maintain speed with slight variations (rider behavior)
                speed = profile.preferredSpeed * (0.95f + Random.nextFloat() * 0.1f)
            }
            RiderDecision.FOLLOW -> {
                riderAhead?.let {
                    speed = (it.speed * 0.9f).coerceAtMost(speed)
                    isWaiting = true
                    waitTime += deltaTime
                }
            }
            RiderDecision.OVERTAKE -> {
                val overtakeSpeed = profile.preferredSpeed * 1.2f
                speed = (speed + 40f * deltaTime).coerceAtMost(overtakeSpeed)
                state.riskyManeuvers++
                state.safetyScore -= 2f
            }
            RiderDecision.STOP_AT_LIGHT -> {
                speed = (speed - 25f * deltaTime).coerceAtLeast(0f)
                isWaiting = true
                waitTime += deltaTime
                intersection?.addToQueue(id, road.direction)
            }
            RiderDecision.AVOID_HAZARD -> {
                speed *= 0.6f // Slow down significantly
                state.stress += 0.05f
            }
            RiderDecision.EMERGENCY_BRAKE -> {
                speed = (speed - 50f * deltaTime).coerceAtLeast(0f)
                state.stress += 0.1f
                nearMissCount++
            }
            RiderDecision.TAKE_SHORTCUT -> {
                // Handled by routing system
                state.timeSaved += 0.5f
            }
        }
    }
    
    private fun findRiderAhead(nearbyRiders: List<Rider>, road: Road): Rider? {
        return nearbyRiders
            .filter { it.id != id && it.currentRoad == road.id }
            .filter {
                val ahead = road.isPositionAhead(position, it.position)
                ahead && position.distanceTo(it.position) < 50f
            }
            .minByOrNull { position.distanceTo(it.position) }
    }
    
    private fun calculateSafeDistance(): Float {
        val baseDistance = 10f // meters
        val speedFactor = speed / 50f
        val experienceFactor = 1f - (profile.experienceLevel * 0.3f)
        val fatigueFactor = 1f + state.currentFatigue
        
        return baseDistance * speedFactor * experienceFactor * fatigueFactor
    }
    
    private fun updateFatigue(deltaTime: Float) {
        state.currentFatigue = (state.currentFatigue + profile.fatigueRate * deltaTime)
            .coerceIn(0f, 1f)
        
        // High fatigue reduces speed
        if (state.currentFatigue > 0.8f) {
            speed *= 0.9f
        }
    }
    
    private fun updateBattery(deltaTime: Float) {
        if (type == RiderType.E_BIKE || type == RiderType.SCOOTER) {
            val consumption = speed * deltaTime * 0.001f
            state.batteryUsed = (state.batteryUsed + consumption).coerceAtMost(100f)
            
            // Low battery affects speed
            val batteryRemaining = profile.batteryLevel - state.batteryUsed
            if (batteryRemaining < 20f) {
                speed *= 0.7f
            }
        }
    }
    
    private fun updateStress(
        hazards: List<Hazard>,
        riderAhead: Rider?,
        canPass: Boolean,
        deltaTime: Float
    ) {
        var stressChange = -0.01f * deltaTime // Natural stress reduction
        
        if (hazards.isNotEmpty()) stressChange += 0.05f * hazards.size
        if (riderAhead != null && !canPass) stressChange += 0.02f
        if (isWaiting) stressChange += 0.01f
        
        state.stress = (state.stress + stressChange).coerceIn(0f, 1f)
    }
}

/**
 * Rider decision types
 */
enum class RiderDecision {
    ACCELERATE,
    CRUISE,
    FOLLOW,
    OVERTAKE,
    STOP_AT_LIGHT,
    AVOID_HAZARD,
    EMERGENCY_BRAKE,
    TAKE_SHORTCUT
}
