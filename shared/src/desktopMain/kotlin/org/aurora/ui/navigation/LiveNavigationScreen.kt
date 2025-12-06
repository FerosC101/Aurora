package org.aurora.ui.navigation

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
import org.aurora.navigation.NavigationState
import org.aurora.navigation.model.StoplightState

@Composable
fun LiveNavigationScreen(
    navState: NavigationState,
    onEndTrip: () -> Unit
) {
    Row(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0E1A))) {
        // Left sidebar
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(400.dp)
                .background(Color(0xFF1E293B))
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF3B82F6)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Navigation,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Navigating to",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 13.sp
                            )
                            Text(
                                navState.selectedRoute?.name ?: "Destination",
                                color = Color.White,
                                fontSize = 18.sp,
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
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
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
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                            Text(
                                "${navState.currentSpeed.toInt()} km/h",
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Progress",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                            Text(
                                "${(navState.progress * 100).toInt()}%",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = navState.progress,
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = Color(0xFF8B5CF6),
                            trackColor = Color(0xFF1E293B)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
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
                            color = Color(0xFF8B5CF6)
                        )
                        StatBox(
                            label = "Saved",
                            value = "4m",
                            color = Color(0xFF3B82F6)
                        )
                    }
                }
            }
            
            // Upcoming Stoplights
            if (navState.upcomingStoplights.isNotEmpty()) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Traffic,
                            contentDescription = null,
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Upcoming Stoplights",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    navState.upcomingStoplights.take(3).forEach { stoplight ->
                        StoplightCard(stoplight)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
            
            // Aurora SHIELD
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF334155)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Aurora SHIELD Active",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "• Clear road ahead - optimal conditions",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                    Text(
                        "• Traffic prediction: Light for next 5 minutes",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                    Text(
                        "• Route efficiency: 97%",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // End Trip button
            Button(
                onClick = onEndTrip,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3B82F6)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("End Trip", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            
            OutlinedButton(
                onClick = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF3B82F6)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Reroute", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        
        // Right side - Map
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
        ) {
            EnhancedNavigationMapCanvas(navState)
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
            .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp
        )
        Text(
            value,
            color = color,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
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
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stoplight.location,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${stoplight.distanceFromStart.toInt()}m",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
            
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    "Turns ${stoplight.state.name.lowercase()} in",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 10.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = stoplight.remainingTime / 60f,
                        modifier = Modifier.width(60.dp).height(4.dp),
                        color = stateColor,
                        trackColor = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "${stoplight.remainingTime}s",
                        color = stateColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun TripCompleteModal(
    route: org.aurora.navigation.model.NavigationRoute,
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
                if (route.type == org.aurora.navigation.model.RouteType.SMART) {
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
