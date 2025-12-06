package org.aurora.rider.model

import org.aurora.traffic.model.*
import kotlin.random.Random

/**
 * RiderOS City Map - Enhanced for rider-specific navigation
 * Includes shortcuts, motorcycle lanes, hazards, and rider-safe routes
 */
class RiderCityMap {
    val intersections: MutableMap<String, Intersection> = mutableMapOf()
    val roads: MutableMap<String, Road> = mutableMapOf()
    val riders: MutableMap<String, Rider> = mutableMapOf()
    val hazardDetector = HazardDetector()
    
    // Rider-specific infrastructure
    val motorcycleLanes: MutableSet<String> = mutableSetOf()
    val shortcuts: MutableList<RiderShortcut> = mutableListOf()
    val unsafeAreas: MutableMap<String, Float> = mutableMapOf() // intersection ID -> danger level
    
    init {
        createRiderCity()
        spawnHazards()
    }
    
    /**
     * Create city infrastructure optimized for rider simulation
     */
    private fun createRiderCity() {
        val gridSpacing = 300f
        
        // Create intersections
        for (row in 0..1) {
            for (col in 0..2) {
                val id = "I${row}${col}"
                val x = col * gridSpacing + 100f
                val y = row * gridSpacing + 100f
                
                intersections[id] = Intersection(
                    id = id,
                    position = Position(x, y),
                    isSignalized = true
                )
            }
        }
        
        // Create main roads
        createMainRoads(gridSpacing)
        
        // Add rider-specific infrastructure
        createMotorcycleLanes()
        createShortcuts()
        markUnsafeAreas()
    }
    
    private fun createMainRoads(gridSpacing: Float) {
        // Horizontal roads
        for (row in 0..1) {
            for (col in 0..1) {
                val startId = "I${row}${col}"
                val endId = "I${row}${col + 1}"
                val start = intersections[startId]!!
                val end = intersections[endId]!!
                
                val roadEastId = "${startId}_${endId}_E"
                roads[roadEastId] = Road(
                    id = roadEastId,
                    startIntersection = startId,
                    endIntersection = endId,
                    lanes = 2,
                    length = gridSpacing,
                    direction = Direction.EAST,
                    positions = listOf(start.position, end.position)
                )
                
                val roadWestId = "${endId}_${startId}_W"
                roads[roadWestId] = Road(
                    id = roadWestId,
                    startIntersection = endId,
                    endIntersection = startId,
                    lanes = 2,
                    length = gridSpacing,
                    direction = Direction.WEST,
                    positions = listOf(end.position, start.position)
                )
                
                intersections[startId]!!.connectedRoads.add(roadEastId)
                intersections[endId]!!.connectedRoads.add(roadWestId)
            }
        }
        
        // Vertical roads
        for (col in 0..2) {
            for (row in 0..0) {
                val startId = "I${row}${col}"
                val endId = "I${row + 1}${col}"
                val start = intersections[startId]!!
                val end = intersections[endId]!!
                
                val roadSouthId = "${startId}_${endId}_S"
                roads[roadSouthId] = Road(
                    id = roadSouthId,
                    startIntersection = startId,
                    endIntersection = endId,
                    lanes = 2,
                    length = gridSpacing,
                    direction = Direction.SOUTH,
                    positions = listOf(start.position, end.position)
                )
                
                val roadNorthId = "${endId}_${startId}_N"
                roads[roadNorthId] = Road(
                    id = roadNorthId,
                    startIntersection = endId,
                    endIntersection = startId,
                    lanes = 2,
                    length = gridSpacing,
                    direction = Direction.NORTH,
                    positions = listOf(end.position, start.position)
                )
                
                intersections[startId]!!.connectedRoads.add(roadSouthId)
                intersections[endId]!!.connectedRoads.add(roadNorthId)
            }
        }
    }
    
    /**
     * Mark certain roads as motorcycle-only lanes (riders can pass faster)
     */
    private fun createMotorcycleLanes() {
        val motorcycleRoads = listOf(
            "I00_I01_E",
            "I01_I02_E",
            "I10_I11_E"
        )
        motorcycleLanes.addAll(motorcycleRoads)
    }
    
    /**
     * Create rider shortcuts (alleys, narrow passages)
     */
    private fun createShortcuts() {
        // Diagonal shortcut from I00 to I11
        shortcuts.add(
            RiderShortcut(
                id = "SC_DIAGONAL_1",
                startIntersection = "I00",
                endIntersection = "I11",
                distance = 250f,
                timeSaving = 15f,
                riskLevel = 0.3f,
                requiredExperience = 0.5f
            )
        )
        
        // Alley shortcut
        shortcuts.add(
            RiderShortcut(
                id = "SC_ALLEY_1",
                startIntersection = "I01",
                endIntersection = "I12",
                distance = 200f,
                timeSaving = 10f,
                riskLevel = 0.2f,
                requiredExperience = 0.3f
            )
        )
    }
    
    /**
     * Mark unsafe areas (high crime, poor lighting, etc.)
     */
    private fun markUnsafeAreas() {
        unsafeAreas["I02"] = 0.7f // High crime area
        unsafeAreas["I10"] = 0.4f // Poor lighting
    }
    
    /**
     * Spawn hazards across the city
     */
    private fun spawnHazards() {
        // Pothole on a main road
        hazardDetector.addHazard(
            Hazard(
                id = "H_POTHOLE_1",
                type = HazardType.POTHOLE,
                severity = HazardSeverity.MODERATE,
                position = Position(250f, 100f),
                affectedRoad = "I00_I01_E",
                description = "Large pothole on eastbound lane"
            )
        )
        
        // Flooded area
        hazardDetector.addHazard(
            Hazard(
                id = "H_FLOOD_1",
                type = HazardType.FLOODED_AREA,
                severity = HazardSeverity.HIGH,
                position = Position(700f, 250f),
                affectedRoad = "I02_I12_S",
                description = "Flooded underpass"
            )
        )
        
        // Accident-prone spot
        hazardDetector.addHazard(
            Hazard(
                id = "H_ACCIDENT_1",
                type = HazardType.ACCIDENT_PRONE_SPOT,
                severity = HazardSeverity.MODERATE,
                position = Position(400f, 400f),
                affectedRoad = "I11_I10_W",
                description = "Blind curve - frequent accidents"
            )
        )
        
        // Steep incline
        hazardDetector.addHazard(
            Hazard(
                id = "H_STEEP_1",
                type = HazardType.STEEP_INCLINE,
                severity = HazardSeverity.LOW,
                position = Position(100f, 250f),
                affectedRoad = "I00_I10_S",
                description = "Steep hill - dangerous for scooters"
            )
        )
        
        // Poor lighting
        hazardDetector.addHazard(
            Hazard(
                id = "H_DARK_1",
                type = HazardType.POOR_LIGHTING,
                severity = HazardSeverity.LOW,
                position = Position(250f, 400f),
                affectedRoad = "I10_I11_E",
                description = "Dark street - unsafe at night"
            )
        )
    }
    
    /**
     * Spawn riders with different profiles
     */
    fun spawnRiders(count: Int = 30) {
        riders.clear()
        
        repeat(count) { i ->
            val riderType = RiderType.values().random()
            val profile = generateRiderProfile(riderType)
            val color = when (riderType) {
                RiderType.DELIVERY_RIDER -> RiderColor.DELIVERY_ORANGE
                RiderType.COMMUTER -> RiderColor.COMMUTER_BLUE
                RiderType.E_BIKE -> RiderColor.EBIKE_GREEN
                RiderType.SCOOTER -> RiderColor.SCOOTER_PURPLE
                RiderType.PERSONAL_MOTORCYCLE -> RiderColor.PERSONAL_CYAN
            }
            
            val startIntersection = intersections.keys.random()
            var endIntersection = intersections.keys.random()
            while (endIntersection == startIntersection) {
                endIntersection = intersections.keys.random()
            }
            
            val route = findPath(startIntersection, endIntersection)
            val startPos = intersections[startIntersection]!!.position.copy()
            
            val rider = Rider(
                id = "R_$i",
                type = riderType,
                profile = profile,
                position = startPos.copy(),
                destination = endIntersection,
                route = route,
                color = color
            )
            
            riders[rider.id] = rider
        }
    }
    
    /**
     * Generate realistic rider profile based on type
     */
    private fun generateRiderProfile(type: RiderType): RiderProfile {
        return when (type) {
            RiderType.DELIVERY_RIDER -> RiderProfile(
                riskTolerance = Random.nextFloat() * 0.3f + 0.6f, // 0.6-0.9 (aggressive)
                preferredSpeed = Random.nextFloat() * 15f + 50f,   // 50-65 km/h
                batteryLevel = 100f,
                avoidHighways = false,
                rainAvoidance = false, // Need to deliver
                shortcutPreference = 0.9f,
                fatigueRate = 0.015f,
                nightRiding = true,
                experienceLevel = 0.7f
            )
            RiderType.COMMUTER -> RiderProfile(
                riskTolerance = Random.nextFloat() * 0.3f + 0.3f, // 0.3-0.6 (moderate)
                preferredSpeed = Random.nextFloat() * 10f + 40f,   // 40-50 km/h
                batteryLevel = 100f,
                avoidHighways = false,
                rainAvoidance = true,
                shortcutPreference = 0.5f,
                fatigueRate = 0.01f,
                nightRiding = true,
                experienceLevel = 0.6f
            )
            RiderType.E_BIKE -> RiderProfile(
                riskTolerance = Random.nextFloat() * 0.2f + 0.1f, // 0.1-0.3 (cautious)
                preferredSpeed = Random.nextFloat() * 5f + 25f,    // 25-30 km/h
                batteryLevel = Random.nextFloat() * 40f + 60f,     // 60-100%
                avoidHighways = true,
                rainAvoidance = true,
                shortcutPreference = 0.3f,
                fatigueRate = 0.02f,
                nightRiding = false,
                experienceLevel = 0.4f
            )
            RiderType.SCOOTER -> RiderProfile(
                riskTolerance = Random.nextFloat() * 0.2f + 0.2f, // 0.2-0.4 (cautious)
                preferredSpeed = Random.nextFloat() * 5f + 30f,    // 30-35 km/h
                batteryLevel = Random.nextFloat() * 30f + 70f,     // 70-100%
                avoidHighways = true,
                rainAvoidance = true,
                shortcutPreference = 0.4f,
                fatigueRate = 0.015f,
                nightRiding = false,
                experienceLevel = 0.5f
            )
            RiderType.PERSONAL_MOTORCYCLE -> RiderProfile(
                riskTolerance = Random.nextFloat() * 0.4f + 0.3f, // 0.3-0.7 (moderate-high)
                preferredSpeed = Random.nextFloat() * 20f + 45f,   // 45-65 km/h
                batteryLevel = 100f,
                avoidHighways = false,
                rainAvoidance = true,
                shortcutPreference = 0.6f,
                fatigueRate = 0.008f,
                nightRiding = true,
                experienceLevel = 0.7f
            )
        }
    }
    
    /**
     * Find path using BFS (basic pathfinding)
     */
    fun findPath(start: String, end: String): List<String> {
        if (start == end) return emptyList()
        
        val queue = mutableListOf<Pair<String, List<String>>>()
        val visited = mutableSetOf<String>()
        queue.add(start to emptyList())
        visited.add(start)
        
        while (queue.isNotEmpty()) {
            val (current, path) = queue.removeAt(0)
            
            if (current == end) return path
            
            val intersection = intersections[current] ?: continue
            
            for (roadId in intersection.connectedRoads) {
                val road = roads[roadId] ?: continue
                val next = road.endIntersection
                
                if (next !in visited) {
                    visited.add(next)
                    queue.add(next to path + roadId)
                }
            }
        }
        
        return emptyList()
    }
    
    /**
     * Find rider-optimized path considering shortcuts and hazards
     */
    fun findRiderPath(
        start: String,
        end: String,
        riderProfile: RiderProfile,
        riderType: RiderType
    ): List<String> {
        val basePath = findPath(start, end)
        
        // TODO: Implement advanced routing with:
        // - Hazard avoidance
        // - Shortcut consideration
        // - Safety preferences
        // - Time optimization
        
        return basePath
    }
}

/**
 * Rider-specific shortcut data
 */
data class RiderShortcut(
    val id: String,
    val startIntersection: String,
    val endIntersection: String,
    val distance: Float,
    val timeSaving: Float,  // seconds saved
    val riskLevel: Float,   // 0.0-1.0
    val requiredExperience: Float // minimum experience needed
)
