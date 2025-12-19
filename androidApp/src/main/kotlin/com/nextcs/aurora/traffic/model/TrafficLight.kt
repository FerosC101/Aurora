package com.nextcs.aurora.traffic.model

enum class LightState {
    RED,
    YELLOW,
    GREEN
}

data class TrafficLight(
    val id: String,
    val intersectionId: String,
    val direction: Direction, // Which direction this light controls
    var state: LightState = LightState.RED,
    var remainingTime: Float = 0f, // seconds
    var greenDuration: Float = 15f, // seconds
    var redDuration: Float = 15f, // seconds
    var yellowDuration: Float = 3f // seconds
) {
    private var cycleTime: Float = 0f
    
    fun update(deltaTime: Float, queueLength: Int = 0, predictedCongestion: Float = 0f, isAIControlled: Boolean = false) {
        remainingTime -= deltaTime
        
        if (remainingTime <= 0) {
            transitionState(queueLength, predictedCongestion, isAIControlled)
        }
    }
    
    private fun transitionState(queueLength: Int, predictedCongestion: Float, isAIControlled: Boolean) {
        when (state) {
            LightState.GREEN -> {
                state = LightState.YELLOW
                remainingTime = yellowDuration
            }
            LightState.YELLOW -> {
                state = LightState.RED
                remainingTime = redDuration
            }
            LightState.RED -> {
                state = LightState.GREEN
                // AI adjusts green duration based on queue and prediction
                if (isAIControlled) {
                    greenDuration = calculateOptimalGreenTime(queueLength, predictedCongestion)
                }
                remainingTime = greenDuration
            }
        }
    }
    
    private fun calculateOptimalGreenTime(queueLength: Int, predictedCongestion: Float): Float {
        val baseTime = 10f
        val queueFactor = (queueLength * 0.5f).coerceIn(0f, 20f)
        val congestionFactor = (predictedCongestion * 15f).coerceIn(0f, 15f)
        return (baseTime + queueFactor + congestionFactor).coerceIn(8f, 45f)
    }
    
    fun canVehiclePass(): Boolean = state == LightState.GREEN
}
