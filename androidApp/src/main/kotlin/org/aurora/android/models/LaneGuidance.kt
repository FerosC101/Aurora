package org.aurora.android.models

enum class LaneType {
    STRAIGHT,
    SLIGHT_LEFT,
    LEFT,
    SHARP_LEFT,
    SLIGHT_RIGHT,
    RIGHT,
    SHARP_RIGHT,
    U_TURN
}

data class LaneGuidance(
    val lanes: List<Lane>,
    val distance: Int, // meters to turn
    val instruction: String
)

data class Lane(
    val type: LaneType,
    val isRecommended: Boolean
)

// Predefined lane configurations for common scenarios
object LaneConfigurations {
    fun straightOnly() = LaneGuidance(
        lanes = listOf(Lane(LaneType.STRAIGHT, true)),
        distance = 0,
        instruction = "Continue straight"
    )
    
    fun leftTurnFromLeft() = LaneGuidance(
        lanes = listOf(
            Lane(LaneType.LEFT, true),
            Lane(LaneType.STRAIGHT, false),
            Lane(LaneType.STRAIGHT, false)
        ),
        distance = 200,
        instruction = "Turn left from left lane"
    )
    
    fun rightTurnFromRight() = LaneGuidance(
        lanes = listOf(
            Lane(LaneType.STRAIGHT, false),
            Lane(LaneType.STRAIGHT, false),
            Lane(LaneType.RIGHT, true)
        ),
        distance = 150,
        instruction = "Turn right from right lane"
    )
    
    fun leftOrStraight() = LaneGuidance(
        lanes = listOf(
            Lane(LaneType.LEFT, true),
            Lane(LaneType.STRAIGHT, true),
            Lane(LaneType.STRAIGHT, false)
        ),
        distance = 300,
        instruction = "Use left or middle lane"
    )
    
    fun complexIntersection() = LaneGuidance(
        lanes = listOf(
            Lane(LaneType.LEFT, false),
            Lane(LaneType.SLIGHT_LEFT, false),
            Lane(LaneType.STRAIGHT, true),
            Lane(LaneType.STRAIGHT, true),
            Lane(LaneType.RIGHT, false)
        ),
        distance = 250,
        instruction = "Stay in middle lanes"
    )
}
