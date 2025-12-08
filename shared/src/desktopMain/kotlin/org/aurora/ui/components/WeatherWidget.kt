package org.aurora.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

data class WeatherData(
    val temperature: Int,
    val condition: WeatherCondition,
    val windSpeed: Int,
    val humidity: Int,
    val visibility: Int,
    val uvIndex: Int
)

enum class WeatherCondition {
    SUNNY, CLOUDY, RAINY, STORMY, FOGGY, SNOWY
}

@Composable
fun WeatherWidget(modifier: Modifier = Modifier) {
    var weather by remember { mutableStateOf(WeatherData(28, WeatherCondition.SUNNY, 12, 65, 10, 7)) }
    val infiniteTransition = rememberInfiniteTransition()
    
    // Animated sun rotation
    val sunRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Animated weather icon
            Box(
                modifier = Modifier.size(64.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    when (weather.condition) {
                        WeatherCondition.SUNNY -> {
                            rotate(sunRotation) {
                                // Sun rays
                                for (i in 0..7) {
                                    val angle = i * 45f
                                    rotate(angle) {
                                        drawLine(
                                            color = Color(0xFFFFA726),
                                            start = Offset(size.width / 2, size.height / 4),
                                            end = Offset(size.width / 2, size.height / 6),
                                            strokeWidth = 3f
                                        )
                                    }
                                }
                            }
                            // Sun circle
                            drawCircle(
                                color = Color(0xFFFFA726),
                                radius = size.minDimension / 4,
                                center = center
                            )
                        }
                        WeatherCondition.RAINY -> {
                            // Cloud
                            drawCircle(
                                color = Color(0xFF78909C),
                                radius = size.minDimension / 4,
                                center = Offset(size.width / 2, size.height / 3)
                            )
                            // Rain drops
                            for (i in 0..2) {
                                drawLine(
                                    color = Color(0xFF42A5F5),
                                    start = Offset(size.width / 3 + i * size.width / 6, size.height * 0.6f),
                                    end = Offset(size.width / 3 + i * size.width / 6, size.height * 0.8f),
                                    strokeWidth = 2f
                                )
                            }
                        }
                        else -> {
                            drawCircle(
                                color = Color.White,
                                radius = size.minDimension / 3,
                                center = center
                            )
                        }
                    }
                }
            }
            
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "${weather.temperature}°",
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        weather.condition.name,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    WeatherMetric("💨", "${weather.windSpeed}km/h")
                    WeatherMetric("💧", "${weather.humidity}%")
                    WeatherMetric("👁", "${weather.visibility}km")
                }
            }
        }
    }
}

@Composable
private fun WeatherMetric(icon: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(icon, fontSize = 12.sp)
        Text(
            value,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 11.sp
        )
    }
}
