package org.aurora.traffic.model

enum class Direction {
    NORTH, SOUTH, EAST, WEST;
    
    fun opposite(): Direction = when (this) {
        NORTH -> SOUTH
        SOUTH -> NORTH
        EAST -> WEST
        WEST -> EAST
    }
    
    fun toAngle(): Float = when (this) {
        NORTH -> -90f
        SOUTH -> 90f
        EAST -> 0f
        WEST -> 180f
    }
}

data class Road(
    val id: String,
    val startIntersection: String,
    val endIntersection: String,
    val lanes: Int = 2,
    val length: Float,
    val direction: Direction,
    val maxSpeed: Float = 50f, // km/h
    val positions: List<Position> // waypoints along the road
) {
    var currentDensity: Float = 0f // vehicles per km
    var currentSpeed: Float = maxSpeed
    var congestionLevel: CongestionLevel = CongestionLevel.FREE
    
    val start: Position
        get() = positions.first()
    
    val end: Position
        get() = positions.last()
    
    val speedLimit: Float
        get() = maxSpeed
    
    fun updateCongestion(vehicleCount: Int) {
        updateCongestionLevel(vehicleCount)
    }
    
    fun updateCongestionLevel(vehicleCount: Int) {
        val density = vehicleCount.toFloat() / (length / 1000f) / lanes
        currentDensity = density
        
        congestionLevel = when {
            density < 10 -> CongestionLevel.FREE
            density < 25 -> CongestionLevel.MODERATE
            density < 45 -> CongestionLevel.HEAVY
            else -> CongestionLevel.GRIDLOCK
        }
        
        currentSpeed = when (congestionLevel) {
            CongestionLevel.FREE -> maxSpeed
            CongestionLevel.MODERATE -> maxSpeed * 0.7f
            CongestionLevel.HEAVY -> maxSpeed * 0.4f
            CongestionLevel.GRIDLOCK -> maxSpeed * 0.1f
        }
    }
    
    fun isPositionAhead(pos1: Position, pos2: Position): Boolean {
        val dir = end.x - start.x + end.y - start.y
        val delta = (pos2.x - pos1.x) + (pos2.y - pos1.y)
        return (dir > 0 && delta > 0) || (dir < 0 && delta < 0)
    }
}

enum class CongestionLevel {
    FREE,       // Green
    MODERATE,   // Yellow
    HEAVY,      // Orange
    GRIDLOCK    // Red
}
