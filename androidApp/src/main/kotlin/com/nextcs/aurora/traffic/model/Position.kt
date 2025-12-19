package com.nextcs.aurora.traffic.model

import kotlin.math.sqrt

data class Position(
    val x: Float,
    val y: Float
) {
    fun distanceTo(other: Position): Float {
        val dx = other.x - x
        val dy = other.y - y
        return sqrt(dx * dx + dy * dy)
    }
    
    fun angleTo(other: Position): Float {
        return kotlin.math.atan2(other.y - y, other.x - x)
    }
    
    operator fun plus(other: Position) = Position(x + other.x, y + other.y)
    operator fun minus(other: Position) = Position(x - other.x, y - other.y)
    operator fun times(scalar: Float) = Position(x * scalar, y * scalar)
}
