package org.aurora.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.aurora.navigation.model.DetectedHazard
import org.aurora.navigation.model.HazardType
import org.aurora.navigation.model.HazardSeverity

@Composable
fun HazardAlertSystem(
    hazards: List<DetectedHazard>,
    currentDistance: Float,
    modifier: Modifier = Modifier
) {
    // Find hazards within alerting range (500m)
    val upcomingHazards = remember(hazards, currentDistance) {
        hazards.filter { hazard ->
            val distanceToHazard = hazard.distance - currentDistance
            distanceToHazard in 0f..500f
        }.sortedBy { it.distance - currentDistance }
    }
    
    // Show alert for closest hazard
    upcomingHazards.firstOrNull()?.let { hazard ->
        HazardAlert(hazard = hazard, distanceToHazard = hazard.distance - currentDistance)
    }
    
    // Show hazard list
    if (upcomingHazards.isNotEmpty()) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "⚠️ AI Detected Hazards Ahead",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            
            upcomingHazards.take(3).forEach { hazard ->
                HazardListItem(hazard, hazard.distance - currentDistance)
            }
        }
    }
}

@Composable
fun HazardAlert(hazard: DetectedHazard, distanceToHazard: Float) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(hazard) {
        visible = true
        delay(5000) // Show for 5 seconds
        visible = false
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = when (hazard.severity) {
                    HazardSeverity.CRITICAL -> Color(0xFFDC2626)
                    HazardSeverity.HIGH -> Color(0xFFEF4444)
                    HazardSeverity.MODERATE -> Color(0xFFFB923C)
                    HazardSeverity.LOW -> Color(0xFFFCD34D)
                }
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    getHazardIcon(hazard.type),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "🤖 Aurora AI Alert",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        getHazardEmoji(hazard.type) + " " + hazard.type.name.replace("_", " "),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        hazard.description,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${distanceToHazard.toInt()}m",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "ahead",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun HazardListItem(hazard: DetectedHazard, distanceToHazard: Float) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF334155)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        when (hazard.severity) {
                            HazardSeverity.CRITICAL -> Color(0xFFDC2626)
                            HazardSeverity.HIGH -> Color(0xFFEF4444)
                            HazardSeverity.MODERATE -> Color(0xFFFB923C)
                            HazardSeverity.LOW -> Color(0xFFFCD34D)
                        },
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    getHazardEmoji(hazard.type),
                    fontSize = 20.sp
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    hazard.type.name.replace("_", " "),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    hazard.description,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${distanceToHazard.toInt()}m",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    hazard.severity.name,
                    color = when (hazard.severity) {
                        HazardSeverity.CRITICAL -> Color(0xFFDC2626)
                        HazardSeverity.HIGH -> Color(0xFFEF4444)
                        HazardSeverity.MODERATE -> Color(0xFFFB923C)
                        HazardSeverity.LOW -> Color(0xFFFCD34D)
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

fun getHazardIcon(type: HazardType): ImageVector {
    return when (type) {
        HazardType.CONSTRUCTION -> Icons.Default.Construction
        HazardType.POTHOLE -> Icons.Default.ErrorOutline
        HazardType.FLOOD -> Icons.Default.WaterDrop
        HazardType.ACCIDENT -> Icons.Default.LocalHospital
    }
}

fun getHazardEmoji(type: HazardType): String {
    return when (type) {
        HazardType.CONSTRUCTION -> "🚧"
        HazardType.POTHOLE -> "🕳️"
        HazardType.FLOOD -> "🌊"
        HazardType.ACCIDENT -> "🚨"
    }
}
