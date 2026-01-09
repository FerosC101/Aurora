package com.nextcs.aurora.ui

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextcs.aurora.navigation.NavigationState
import com.nextcs.aurora.navigation.model.StoplightState

@Composable
fun LiveNavigationScreen(
    navState: NavigationState,
    onEndTrip: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F7FA))) {
        // Top section - Map Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
                .background(Color(0xFFE5E7EB))
                .padding(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0F172A)
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    EnhancedNavigationMapCanvas(navState)
                }
            }
        }
        
        // Bottom section - Navigation info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1976D2)
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Place,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Navigating to",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal
                            )
                            Text(
                                navState.selectedRoute?.name ?: "Smart Route",
                                color = Color.White,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "ETA",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "${navState.eta} min",
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "Speed",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "${navState.currentSpeed.toInt()} km/h",
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Progress",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "${(navState.progress * 100).toInt()}%",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = navState.progress,
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = Color.White,
                            trackColor = Color(0xFF1976D2)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // Stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatBox(
                            label = "Hazards",
                            value = "${navState.selectedRoute?.detectedHazards?.size ?: 0}",
                            color = Color(0xFF10B981)
                        )
                        StatBox(
                            label = "Safety",
                            value = "95%",
                            color = Color(0xFFFBBF24)
                        )
                        StatBox(
                            label = "Saved",
                            value = "4m",
                            color = Color(0xFF42A5F5)
                        )
                    }
                }
            }
            
            // Additional Metrics Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    icon = Icons.Default.Person,
                    label = "Distance",
                    value = "${((navState.selectedRoute?.distance?.toDouble() ?: 0.0) / 1000.0).toInt()} km",
                    color = Color(0xFF1976D2),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    icon = Icons.Default.LocationOn,
                    label = "CO₂ Saved",
                    value = "2.3 kg",
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    icon = Icons.Default.Warning,
                    label = "Weather",
                    value = "Clear",
                    color = Color(0xFFFBBF24),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    icon = Icons.Default.Star,
                    label = "Traffic",
                    value = "Light",
                    color = Color(0xFF42A5F5),
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Upcoming Stoplights
            if (navState.upcomingStoplights.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF8F9FA)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFF1976D2),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Upcoming Stoplights",
                                color = Color(0xFF1F2937),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        navState.upcomingStoplights.take(2).forEach { stoplight ->
                            StoplightCard(stoplight)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
            
            // Aurora SHIELD
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF10B981).copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Aurora SHIELD Active",
                            color = Color(0xFF1F2937),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "• Clear road ahead - optimal conditions",
                        color = Color(0xFF4B5563),
                        fontSize = 11.sp
                    )
                    Text(
                        "• Traffic prediction: Light for next 5 minutes",
                        color = Color(0xFF4B5563),
                        fontSize = 11.sp
                    )
                    Text(
                        "• Route efficiency: 97%",
                        color = Color(0xFF4B5563),
                        fontSize = 11.sp
                    )
                }
            }
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF1976D2)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reroute", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                
                Button(
                    onClick = onEndTrip,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1976D2)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("End Trip", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun StatBox(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            label,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            value,
            color = color,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun MetricCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
            Column {
                Text(
                    label,
                    color = Color(0xFF6B7280),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    value,
                    color = Color(0xFF1F2937),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun StoplightCard(stoplight: com.nextcs.aurora.navigation.model.Stoplight) {
    val stateColor = when (stoplight.state) {
        StoplightState.GREEN -> Color(0xFF10B981)
        StoplightState.YELLOW -> Color(0xFFFBBF24)
        StoplightState.RED -> Color(0xFFEF4444)
    }
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(stateColor, CircleShape)
                )
                Column {
                    Text(
                        stoplight.location,
                        color = Color(0xFF1F2937),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "${stoplight.distanceFromStart.toInt()}m away",
                        color = Color(0xFF6B7280),
                        fontSize = 10.sp
                    )
                }
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    "${stoplight.state.name.lowercase()}",
                    color = stateColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${stoplight.remainingTime}s",
                    color = Color(0xFF6B7280),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun TripCompleteModal(
    route: com.nextcs.aurora.navigation.model.NavigationRoute,
    hazardsAvoided: Int,
    timeSaved: Int,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
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
                if (route.type == com.nextcs.aurora.navigation.model.RouteType.SMART) {
                    Surface(
                        color = Color(0xFF334155),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Star,
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
