package com.nextcs.aurora.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextcs.aurora.navigation.DetectedHazard
import com.nextcs.aurora.navigation.HazardSeverity
import com.nextcs.aurora.navigation.RouteAlternative

@Composable
fun RouteComparisonScreen(
    routes: List<RouteAlternative>,
    onRouteSelected: (RouteAlternative) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    aiRecommendation: String? = null
) {
    var selectedRoute by remember { mutableStateOf<RouteAlternative?>(
        routes.find { it.name == aiRecommendation } ?: routes.firstOrNull()
    ) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // Header
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            color = Color.White,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Choose Route",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121)
                )
                Spacer(modifier = Modifier.width(40.dp))
            }
        }
        
        // Route Comparison Cards
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            routes.forEach { route ->
                RouteComparisonCard(
                    route = route,
                    isSelected = selectedRoute == route,
                    onSelect = { selectedRoute = it },
                    isAiRecommended = route.name == aiRecommendation
                )
            }
            
            // Hazard Details Card
            selectedRoute?.let { route ->
                if (route.hazards.isNotEmpty()) {
                    HazardListCard(hazards = route.hazards)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Selection Button
        Button(
            onClick = { selectedRoute?.let { onRouteSelected(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(48.dp),
            enabled = selectedRoute != null,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1E88E5),
                disabledContainerColor = Color(0xFF9E9E9E)
            )
        ) {
            Text(
                text = "Continue with ${selectedRoute?.name}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun RouteComparisonCard(
    route: RouteAlternative,
    isSelected: Boolean,
    onSelect: (RouteAlternative) -> Unit,
    isAiRecommended: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(route) }
            .then(
                if (isSelected) {
                    Modifier.border(
                        2.dp,
                        Color(0xFF1E88E5),
                        RoundedCornerShape(12.dp)
                    )
                } else if (isAiRecommended) {
                    Modifier.border(
                        2.dp,
                        Color(0xFF7C3AED),
                        RoundedCornerShape(12.dp)
                    )
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> Color(0xFFE3F2FD)
                isAiRecommended -> Color(0xFFF3E5F5)
                else -> Color.White
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // AI Recommendation Badge
            if (isAiRecommended) {
                Surface(
                    color = Color(0xFF7C3AED),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "🤖 AI Recommended",
                        modifier = Modifier.padding(6.dp, 3.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            
            // Route Name & Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = route.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )
                    Text(
                        text = route.characteristics,
                        fontSize = 13.sp,
                        color = Color(0xFF757575)
                    )
                }
                
                // Badge
                Surface(
                    color = getRouteColor(route.name),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = getRouteBadge(route.name),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color(0xFFE0E0E0))
            Spacer(modifier = Modifier.height(12.dp))
            
            // Stats Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = Icons.Default.Info,
                    label = "ETA",
                    value = formatDuration(route.routeInfo.duration)
                )
                StatItem(
                    icon = Icons.Default.LocationOn,
                    label = "Distance",
                    value = formatDistance(route.routeInfo.distance)
                )
                StatItem(
                    icon = Icons.Default.Info,
                    label = "Safety",
                    value = "${route.safetyScore}%"
                )
                StatItem(
                    icon = Icons.Default.Warning,
                    label = "Hazards",
                    value = route.hazards.size.toString()
                )
            }
        }
    }
}

@Composable
fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFF1E88E5),
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF212121)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF9E9E9E)
        )
    }
}

@Composable
fun HazardListCard(hazards: List<DetectedHazard>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Hazards on This Route (${hazards.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            hazards.take(5).forEach { hazard ->
                HazardItem(hazard = hazard)
                if (hazard != hazards.take(5).last()) {
                    Divider(color = Color(0xFFE0E0E0), modifier = Modifier.padding(vertical = 8.dp))
                }
            }
            
            if (hazards.size > 5) {
                Text(
                    text = "and ${hazards.size - 5} more...",
                    fontSize = 12.sp,
                    color = Color(0xFF9E9E9E),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun HazardItem(hazard: DetectedHazard) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Use icon instead of emoji
        val iconColor = parseColorString(getHazardColor(hazard.severity))
        Icon(
            imageVector = when (hazard.type) {
                "construction" -> Icons.Default.Build
                "accident" -> Icons.Default.Warning
                else -> Icons.Default.LocationOn
            },
            contentDescription = hazard.type,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = hazard.type.replaceFirstChar { it.uppercase() },
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF212121)
            )
            Text(
                text = hazard.description,
                fontSize = 11.sp,
                color = Color(0xFF757575)
            )
            Text(
                text = "In ${formatDistance(hazard.distanceFromStart)}",
                fontSize = 10.sp,
                color = Color(0xFF9E9E9E)
            )
        }
        Surface(
            color = parseColorString(getHazardColor(hazard.severity)).copy(alpha = 0.2f),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = hazard.severity.name,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = parseColorString(getHazardColor(hazard.severity)),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

private fun parseColorString(hexColor: String): Color {
    val hex = hexColor.removePrefix("#")
    return when (hex.length) {
        6 -> Color(hex.toLong(16) or 0xFFFFFFFFUL.toLong())
        8 -> Color(hex.toLong(16))
        else -> Color.White
    }
}

private fun getRouteColor(routeName: String): Color {
    return when (routeName) {
        "Smart Route" -> Color(0xFF1E88E5)
        "Chill Route" -> Color(0xFF4CAF50)
        "Regular Route" -> Color(0xFF9E9E9E)
        else -> Color(0xFF1E88E5)
    }
}

private fun getRouteBadge(routeName: String): String {
    return when (routeName) {
        "Smart Route" -> "FASTEST"
        "Chill Route" -> "SCENIC"
        "Regular Route" -> "STANDARD"
        else -> ""
    }
}

private fun getHazardColor(severity: HazardSeverity): String {
    return when (severity) {
        HazardSeverity.LOW -> "#FCD34D"
        HazardSeverity.MODERATE -> "#FB923C"
        HazardSeverity.HIGH -> "#EF4444"
        HazardSeverity.CRITICAL -> "#DC2626"
    }
}

private fun formatDistance(meters: Int): String {
    return if (meters < 1000) {
        "$meters m"
    } else {
        String.format("%.1f km", meters / 1000.0)
    }
}

private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    return if (minutes < 60) {
        "$minutes min"
    } else {
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        if (remainingMinutes == 0) "${hours}h" else "${hours}h ${remainingMinutes}m"
    }
}
