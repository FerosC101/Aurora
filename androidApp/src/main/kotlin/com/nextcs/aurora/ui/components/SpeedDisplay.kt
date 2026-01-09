package com.nextcs.aurora.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SpeedDisplay(
    currentSpeed: Float,
    speedLimit: Int?,
    isExceeding: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isExceeding) Color(0xFFD32F2F) else Color.White,
        animationSpec = tween(300),
        label = "backgroundColor"
    )
    
    val textColor by animateColorAsState(
        targetValue = if (isExceeding) Color.White else Color(0xFF212121),
        animationSpec = tween(300),
        label = "textColor"
    )

    // Pulse animation when exceeding
    val scale by animateFloatAsState(
        targetValue = if (isExceeding) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Card(
        modifier = modifier
            .scale(if (isExceeding) scale else 1f)
            .width(120.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Current Speed
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = currentSpeed.toInt().toString(),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "km/h",
                    fontSize = 14.sp,
                    color = textColor.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // Speed Limit
            if (speedLimit != null) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Surface(
                    shape = CircleShape,
                    color = if (isExceeding) Color.White else Color(0xFFE53935),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = speedLimit.toString(),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isExceeding) Color(0xFFE53935) else Color.White
                            )
                            Text(
                                text = "LIMIT",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isExceeding) Color(0xFFE53935) else Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompactSpeedDisplay(
    currentSpeed: Float,
    speedLimit: Int?,
    isExceeding: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                color = if (isExceeding) Color(0xFFD32F2F) else Color.White,
                shape = RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Current Speed
        Text(
            text = "${currentSpeed.toInt()}",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = if (isExceeding) Color.White else Color(0xFF212121)
        )

        // Separator
        if (speedLimit != null) {
            Text(
                text = "/",
                fontSize = 16.sp,
                color = if (isExceeding) Color.White.copy(alpha = 0.7f) else Color(0xFF757575)
            )

            // Speed Limit
            Text(
                text = "$speedLimit",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = if (isExceeding) Color.White.copy(alpha = 0.9f) else Color(0xFF757575)
            )
        }

        Text(
            text = "km/h",
            fontSize = 12.sp,
            color = if (isExceeding) Color.White.copy(alpha = 0.8f) else Color(0xFF757575)
        )
    }
}
