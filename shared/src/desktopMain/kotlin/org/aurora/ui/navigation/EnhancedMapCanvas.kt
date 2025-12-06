package org.aurora.ui.navigation

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import org.aurora.navigation.NavigationState
import org.aurora.navigation.model.*
import org.aurora.traffic.model.Position
import kotlin.math.*

@Composable
fun EnhancedNavigationMapCanvas(navState: NavigationState) {
    // Animated pulse effect for current position
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    // Animated progress for smooth route drawing
    val animatedProgress by animateFloatAsState(
        targetValue = navState.progress,
        animationSpec = tween(300, easing = EaseOutCubic)
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val route = navState.selectedRoute ?: return@Canvas
        val waypoints = route.waypoints
        if (waypoints.isEmpty()) return@Canvas
        
        // Clean dark background
        drawRect(
            color = Color(0xFF1A1F2E),
            size = size
        )
        
        // Subtle grid for streets
        val gridColor = Color(0xFF2D3748).copy(alpha = 0.4f)
        val spacing = 80f
        
        // Vertical lines
        var x = 0f
        while (x < size.width) {
            drawLine(
                color = gridColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f
            )
            x += spacing
        }
        
        // Horizontal lines
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
            y += spacing
        }
        
        // Draw the route path
        val path = Path()
        val scaleX = size.width / 1000f
        val scaleY = size.height / 600f
        
        waypoints.forEachIndexed { index, waypoint ->
            val wx = waypoint.x * scaleX
            val wy = waypoint.y * scaleY
            
            if (index == 0) {
                path.moveTo(wx, wy)
            } else {
                path.lineTo(wx, wy)
            }
        }
        
        // Draw outer stroke for depth
        drawPath(
            path = path,
            color = Color(0xFF1E3A8A),
            style = Stroke(
                width = 20f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        
        // Draw main route line
        drawPath(
            path = path,
            color = Color(0xFF3B82F6),
            style = Stroke(
                width = 12f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        
        // Draw traveled portion in different color with smooth animation
        val traveledPath = Path()
        val progressWaypoints = waypoints.take(max(2, (waypoints.size * animatedProgress).toInt()))
        
        progressWaypoints.forEachIndexed { index, waypoint ->
            val wx = waypoint.x * scaleX
            val wy = waypoint.y * scaleY
            
            if (index == 0) {
                traveledPath.moveTo(wx, wy)
            } else {
                traveledPath.lineTo(wx, wy)
            }
        }
        
        drawPath(
            path = traveledPath,
            color = Color(0xFF8B5CF6),
            style = Stroke(
                width = 12f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        
        // Draw hazard markers (if route has them)
        route.detectedHazards?.forEach { hazard ->
            val hx = hazard.position.x * size.width / 1000f
            val hy = hazard.position.y * size.height / 600f
            
            val hcolor = when (hazard.severity) {
                HazardSeverity.CRITICAL -> Color(0xFFDC2626)
                HazardSeverity.HIGH -> Color(0xFFEF4444)
                HazardSeverity.MODERATE -> Color(0xFFF59E0B)
                HazardSeverity.LOW -> Color(0xFFFBBF24)
            }
            
            // Pulsing background
            drawCircle(
                color = hcolor.copy(alpha = 0.3f),
                radius = 32f,
                center = Offset(hx, hy)
            )
            
            // Main marker
            drawCircle(
                color = hcolor,
                radius = 20f,
                center = Offset(hx, hy)
            )
            
            // Icon based on hazard type
            when (hazard.type) {
                HazardType.CONSTRUCTION -> {
                    // Triangle
                    val iconPath = Path().apply {
                        moveTo(hx, hy - 10f)
                        lineTo(hx - 8f, hy + 8f)
                        lineTo(hx + 8f, hy + 8f)
                        close()
                    }
                    drawPath(
                        path = iconPath,
                        color = Color.White,
                        style = Stroke(width = 2f)
                    )
                }
                HazardType.POTHOLE -> {
                    // Circle with exclamation
                    drawCircle(
                        color = Color.White,
                        radius = 3f,
                        center = Offset(hx, hy + 4f)
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(hx, hy - 6f),
                        end = Offset(hx, hy),
                        strokeWidth = 2f,
                        cap = StrokeCap.Round
                    )
                }
                HazardType.FLOOD -> {
                    // Wave pattern
                    drawLine(
                        color = Color.White,
                        start = Offset(hx - 6f, hy),
                        end = Offset(hx + 6f, hy),
                        strokeWidth = 2f,
                        cap = StrokeCap.Round
                    )
                }
                HazardType.ACCIDENT -> {
                    // X mark
                    drawLine(
                        color = Color.White,
                        start = Offset(hx - 6f, hy - 6f),
                        end = Offset(hx + 6f, hy + 6f),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(hx + 6f, hy - 6f),
                        end = Offset(hx - 6f, hy + 6f),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
        
        // Draw destination marker
        val lastPoint = waypoints.last()
        val destX = lastPoint.x * size.width / 1000f
        val destY = lastPoint.y * size.height / 600f
        
        drawCircle(
            color = Color(0xFFEF4444),
            radius = 28f,
            center = Offset(destX, destY)
        )
        drawCircle(
            color = Color.White,
            radius = 12f,
            center = Offset(destX, destY)
        )
        
        // Draw current position marker
        val currentX = navState.currentPosition.x * size.width / 1000f
        val currentY = navState.currentPosition.y * size.height / 600f
        
        // Outer glow
        drawCircle(
            color = Color(0xFF3B82F6).copy(alpha = 0.3f),
            radius = 40f,
            center = Offset(currentX, currentY)
        )
        
        // Main position circle
        drawCircle(
            color = Color(0xFF3B82F6),
            radius = 24f,
            center = Offset(currentX, currentY)
        )
        
        // White center dot
        drawCircle(
            color = Color.White,
            radius = 8f,
            center = Offset(currentX, currentY)
        )
        
        // Direction indicator arrow
        val angle = atan2(
            navState.currentPosition.y - (waypoints.getOrNull(1)?.y ?: waypoints[0].y),
            navState.currentPosition.x - (waypoints.getOrNull(1)?.x ?: waypoints[0].x)
        )
        
        val arrowPath = Path().apply {
            moveTo(currentX, currentY - 12f)
            lineTo(currentX - 6f, currentY - 4f)
            lineTo(currentX, currentY - 8f)
            lineTo(currentX + 6f, currentY - 4f)
            close()
        }
        
        drawPath(
            path = arrowPath,
            color = Color.White
        )
    }
}
