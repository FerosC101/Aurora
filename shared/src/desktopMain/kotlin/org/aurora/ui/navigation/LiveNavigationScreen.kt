package org.aurora.ui.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.aurora.navigation.NavigationState
import org.aurora.navigation.model.NavigationRoute
import org.aurora.navigation.model.RouteType
import org.aurora.navigation.model.StoplightState
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LiveNavigationScreen(
    navState: NavigationState,
    onEndTrip: () -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
        // Left sidebar - Navigation info
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.32f)
                .fillMaxHeight(),
            color = Color(0xFF1E293B)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // Navigation header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFF3B82F6), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Navigation,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Column {
                        Text(
                            "Navigating to",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                        Text(
                            navState.selectedRoute?.name ?: "Destination",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // ETA and Speed
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "ETA",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                "${navState.eta}",
                                color = Color.White,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "min",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 16.sp
                            )
                        }
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "Speed",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                "${navState.currentSpeed.toInt()}",
                                color = Color(0xFF10B981),
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "km/h",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 16.sp
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Progress bar
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Progress",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                        Text(
                            "${(navState.progress * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = navState.progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = Color(0xFF3B82F6),
                        trackColor = Color(0xFF334155)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Stats cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Hazards",
                        value = "${navState.hazardsAvoided}",
                        color = Color(0xFFF59E0B)
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Safety",
                        value = "${navState.selectedRoute?.safetyScore ?: 0}%",
                        color = Color(0xFF10B981)
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Saved",
                        value = "${navState.timeSaved / 60}m",
                        color = Color(0xFF3B82F6)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Upcoming Stoplights
                Text(
                    "⏱️ Upcoming Stoplights",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                if (navState.upcomingStoplights.isEmpty()) {
                    Text(
                        "No stoplights ahead",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                } else {
                    navState.upcomingStoplights.take(3).forEach { stoplight ->
                        StoplightCard(stoplight)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Aurora SHIELD Hazard Alerts
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (navState.activeHazardAlerts.isNotEmpty()) Color(0xFF7F1D1D) else Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = if (navState.activeHazardAlerts.isNotEmpty()) Color(0xFFFEF3C7) else Color(0xFF10B981),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Aurora SHIELD",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (navState.activeHazardAlerts.isEmpty()) {
                            Text(
                                "• Clear road ahead - optimal conditions\n• No hazards detected\n• Route efficiency: ${(97 - navState.progress * 5).toInt()}%",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        } else {
                            navState.activeHazardAlerts.take(3).forEach { alert ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    val hazardIcon = when (alert.type) {
                                        org.aurora.navigation.model.HazardType.POTHOLE -> Icons.Default.Warning
                                        org.aurora.navigation.model.HazardType.FLOOD -> Icons.Default.WaterDrop
                                        org.aurora.navigation.model.HazardType.ACCIDENT -> Icons.Default.ErrorOutline
                                        org.aurora.navigation.model.HazardType.CONSTRUCTION -> Icons.Default.Construction
                                    }
                                    
                                    Icon(
                                        hazardIcon,
                                        contentDescription = null,
                                        tint = Color(0xFFFEF3C7),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            "${alert.type.name} - ${alert.location}",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            alert.description,
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // End Trip button
                OutlinedButton(
                    onClick = onEndTrip,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFEF4444)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("End Trip", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        
        // Right side - Map
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
        ) {
            NavigationMapCanvas(navState)
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF334155)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                color = color,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                label,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun StoplightCard(stoplight: org.aurora.navigation.model.Stoplight) {
    val stateColor = when (stoplight.state) {
        StoplightState.GREEN -> Color(0xFF10B981)
        StoplightState.YELLOW -> Color(0xFFFBBF24)
        StoplightState.RED -> Color(0xFFEF4444)
    }
    
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(stateColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stoplight.location,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${stoplight.distanceFromStart.toInt()}m",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${stoplight.remainingTime}s",
                    color = stateColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Turns ${stoplight.state.name.lowercase()}",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun NavigationMapCanvas(navState: NavigationState) {
    Canvas(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        
        val route = navState.selectedRoute ?: return@Canvas
        
        // Scale waypoints to canvas
        val waypoints = route.waypoints
        val scale = minOf(canvasWidth / 800f, canvasHeight / 400f)
        
        // Draw route path
        val pathColor = when (route.type) {
            RouteType.SMART -> Color(0xFF3B82F6)
            RouteType.CHILL -> Color(0xFFA855F7)
            RouteType.REGULAR -> Color(0xFF64748B)
        }
        
        val path = Path()
        waypoints.forEachIndexed { index, waypoint ->
            val x = waypoint.x * scale
            val y = waypoint.y * scale
            
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        
        drawPath(
            path = path,
            color = pathColor,
            style = Stroke(width = 8f)
        )
        
        // Draw waypoints
        waypoints.forEach { waypoint ->
            drawCircle(
                color = pathColor.copy(alpha = 0.3f),
                radius = 12f,
                center = Offset(waypoint.x * scale, waypoint.y * scale)
            )
        }
        
        // Draw start marker
        drawCircle(
            color = Color(0xFF10B981),
            radius = 16f,
            center = Offset(waypoints.first().x * scale, waypoints.first().y * scale)
        )
        
        // Draw end marker
        drawCircle(
            color = Color(0xFFEF4444),
            radius = 16f,
            center = Offset(waypoints.last().x * scale, waypoints.last().y * scale)
        )
        
        // Draw current position
        drawCircle(
            color = Color(0xFF3B82F6).copy(alpha = 0.3f),
            radius = 32f,
            center = Offset(navState.currentPosition.x * scale, navState.currentPosition.y * scale)
        )
        drawCircle(
            color = Color(0xFF3B82F6),
            radius = 12f,
            center = Offset(navState.currentPosition.x * scale, navState.currentPosition.y * scale)
        )
        
        // Draw stoplights
        navState.upcomingStoplights.forEach { stoplight ->
            val stateColor = when (stoplight.state) {
                StoplightState.GREEN -> Color(0xFF10B981)
                StoplightState.YELLOW -> Color(0xFFFBBF24)
                StoplightState.RED -> Color(0xFFEF4444)
            }
            
            drawCircle(
                color = stateColor.copy(alpha = 0.5f),
                radius = 20f,
                center = Offset(stoplight.position.x * scale, stoplight.position.y * scale)
            )
            drawCircle(
                color = stateColor,
                radius = 10f,
                center = Offset(stoplight.position.x * scale, stoplight.position.y * scale)
            )
        }
    }
}

@Composable
fun TripCompleteModal(
    route: NavigationRoute,
    hazardsAvoided: Int,
    timeSaved: Int,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(500.dp)
                .height(600.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E293B)
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(80.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    "Trip Complete!",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    "You've arrived safely at your destination",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Stats
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TripStat("Time Saved", "⏱️ ${timeSaved / 60} minutes", Color(0xFF3B82F6))
                    TripStat("Hazards Avoided", "🛡️ $hazardsAvoided", Color(0xFFF59E0B))
                    TripStat("Safety Score", "⭐ ${route.safetyScore}%", Color(0xFF10B981))
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Achievement badge
                if (route.type == RouteType.SMART) {
                    Surface(
                        color = Color(0xFF334155),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Smart Rider!",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "You saved time and stayed safe",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3B82F6)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        "Plan Another Trip",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun TripStat(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF334155), RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 16.sp
        )
        Text(
            value,
            color = color,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
