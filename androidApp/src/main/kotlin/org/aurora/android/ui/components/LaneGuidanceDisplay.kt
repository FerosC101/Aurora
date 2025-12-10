package org.aurora.android.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.aurora.android.models.Lane
import org.aurora.android.models.LaneGuidance
import org.aurora.android.models.LaneType

@Composable
fun LaneGuidanceDisplay(
    laneGuidance: LaneGuidance,
    modifier: Modifier = Modifier
) {
    // Pulse animation for recommended lanes
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Surface(
        modifier = modifier,
        color = Color(0xE6FFFFFF),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Distance to turn
            Text(
                text = "in ${laneGuidance.distance}m",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E88E5)
            )
            
            // Lane arrows
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                laneGuidance.lanes.forEach { lane ->
                    LaneArrow(
                        lane = lane,
                        alpha = if (lane.isRecommended) alpha else 1f
                    )
                }
            }
            
            // Instruction text
            Text(
                text = laneGuidance.instruction,
                fontSize = 12.sp,
                color = Color(0xFF757575)
            )
        }
    }
}

@Composable
fun LaneArrow(
    lane: Lane,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    val color = if (lane.isRecommended) {
        Color(0xFF4CAF50).copy(alpha = alpha)
    } else {
        Color(0xFFBDBDBD)
    }
    
    val strokeWidth = if (lane.isRecommended) 3.5f else 2.5f

    Canvas(
        modifier = modifier.size(40.dp)
    ) {
        val width = size.width
        val height = size.height
        val arrowPath = Path()

        when (lane.type) {
            LaneType.STRAIGHT -> {
                // Straight arrow
                arrowPath.moveTo(width / 2, height * 0.8f)
                arrowPath.lineTo(width / 2, height * 0.2f)
                // Arrow head
                arrowPath.moveTo(width / 2 - 8, height * 0.3f)
                arrowPath.lineTo(width / 2, height * 0.2f)
                arrowPath.lineTo(width / 2 + 8, height * 0.3f)
            }
            
            LaneType.LEFT -> {
                // Left turn arrow
                arrowPath.moveTo(width / 2, height * 0.8f)
                arrowPath.lineTo(width / 2, height * 0.5f)
                arrowPath.cubicTo(
                    width / 2, height * 0.3f,
                    width * 0.3f, height * 0.3f,
                    width * 0.2f, height * 0.3f
                )
                // Arrow head
                arrowPath.moveTo(width * 0.3f, height * 0.25f)
                arrowPath.lineTo(width * 0.2f, height * 0.3f)
                arrowPath.lineTo(width * 0.3f, height * 0.35f)
            }
            
            LaneType.RIGHT -> {
                // Right turn arrow
                arrowPath.moveTo(width / 2, height * 0.8f)
                arrowPath.lineTo(width / 2, height * 0.5f)
                arrowPath.cubicTo(
                    width / 2, height * 0.3f,
                    width * 0.7f, height * 0.3f,
                    width * 0.8f, height * 0.3f
                )
                // Arrow head
                arrowPath.moveTo(width * 0.7f, height * 0.25f)
                arrowPath.lineTo(width * 0.8f, height * 0.3f)
                arrowPath.lineTo(width * 0.7f, height * 0.35f)
            }
            
            LaneType.SLIGHT_LEFT -> {
                // Slight left arrow
                arrowPath.moveTo(width / 2, height * 0.8f)
                arrowPath.lineTo(width / 2, height * 0.5f)
                arrowPath.lineTo(width * 0.3f, height * 0.2f)
                // Arrow head
                arrowPath.moveTo(width * 0.35f, height * 0.25f)
                arrowPath.lineTo(width * 0.3f, height * 0.2f)
                arrowPath.lineTo(width * 0.35f, height * 0.15f)
            }
            
            LaneType.SLIGHT_RIGHT -> {
                // Slight right arrow
                arrowPath.moveTo(width / 2, height * 0.8f)
                arrowPath.lineTo(width / 2, height * 0.5f)
                arrowPath.lineTo(width * 0.7f, height * 0.2f)
                // Arrow head
                arrowPath.moveTo(width * 0.65f, height * 0.25f)
                arrowPath.lineTo(width * 0.7f, height * 0.2f)
                arrowPath.lineTo(width * 0.65f, height * 0.15f)
            }
            
            LaneType.SHARP_LEFT -> {
                // Sharp left U-turn style
                arrowPath.moveTo(width / 2, height * 0.8f)
                arrowPath.lineTo(width / 2, height * 0.5f)
                arrowPath.cubicTo(
                    width / 2, height * 0.25f,
                    width * 0.15f, height * 0.25f,
                    width * 0.15f, height * 0.4f
                )
                // Arrow head
                arrowPath.moveTo(width * 0.2f, height * 0.35f)
                arrowPath.lineTo(width * 0.15f, height * 0.4f)
                arrowPath.lineTo(width * 0.1f, height * 0.35f)
            }
            
            LaneType.SHARP_RIGHT -> {
                // Sharp right U-turn style
                arrowPath.moveTo(width / 2, height * 0.8f)
                arrowPath.lineTo(width / 2, height * 0.5f)
                arrowPath.cubicTo(
                    width / 2, height * 0.25f,
                    width * 0.85f, height * 0.25f,
                    width * 0.85f, height * 0.4f
                )
                // Arrow head
                arrowPath.moveTo(width * 0.8f, height * 0.35f)
                arrowPath.lineTo(width * 0.85f, height * 0.4f)
                arrowPath.lineTo(width * 0.9f, height * 0.35f)
            }
            
            LaneType.U_TURN -> {
                // U-turn arrow
                arrowPath.moveTo(width / 2, height * 0.8f)
                arrowPath.lineTo(width / 2, height * 0.5f)
                arrowPath.cubicTo(
                    width / 2, height * 0.2f,
                    width * 0.2f, height * 0.2f,
                    width * 0.2f, height * 0.5f
                )
                arrowPath.lineTo(width * 0.2f, height * 0.6f)
                // Arrow head
                arrowPath.moveTo(width * 0.15f, height * 0.55f)
                arrowPath.lineTo(width * 0.2f, height * 0.6f)
                arrowPath.lineTo(width * 0.25f, height * 0.55f)
            }
        }

        drawPath(
            path = arrowPath,
            color = color,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        
        // Draw lane divider lines
        if (lane.isRecommended) {
            // Highlight background
            drawRect(
                color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                size = Size(width, height)
            )
        }
    }
}

@Composable
fun CompactLaneGuidance(
    laneGuidance: LaneGuidance,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                color = Color(0xE6FFFFFF),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        laneGuidance.lanes.forEach { lane ->
            LaneArrow(
                lane = lane,
                alpha = if (lane.isRecommended) 1f else 0.5f,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
