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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.aurora.database.TripRecord
import org.aurora.database.UserStatistics
import org.aurora.navigation.model.RouteType

@Composable
fun StatisticsScreen(
    userStats: UserStatistics,
    recentTrips: List<TripRecord>,
    routeTypeStats: Map<RouteType, Int>,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.ElectricBolt,
                    contentDescription = null,
                    tint = Color(0xFF3B82F6),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Your Statistics",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(48.dp))
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Overview Stats Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(
                    title = "Total Trips",
                    value = userStats.totalTrips.toString(),
                    icon = Icons.Default.DirectionsBike,
                    color = Color(0xFF3B82F6),
                    modifier = Modifier.weight(1f)
                )
                
                StatCard(
                    title = "Distance",
                    value = "${String.format("%.1f", userStats.totalDistance)} km",
                    icon = Icons.Default.Route,
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
                
                StatCard(
                    title = "Time Saved",
                    value = "${userStats.totalTimeSaved / 60} min",
                    icon = Icons.Default.Speed,
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(
                    title = "Hazards Avoided",
                    value = userStats.totalHazardsAvoided.toString(),
                    icon = Icons.Default.Shield,
                    color = Color(0xFFEF4444),
                    modifier = Modifier.weight(1f)
                )
                
                StatCard(
                    title = "Avg Safety",
                    value = "${userStats.avgSafetyScore}%",
                    icon = Icons.Default.Security,
                    color = Color(0xFFA855F7),
                    modifier = Modifier.weight(1f)
                )
                
                StatCard(
                    title = "Riding Time",
                    value = "${userStats.totalTime / 60}h ${userStats.totalTime % 60}m",
                    icon = Icons.Default.Timer,
                    color = Color(0xFF06B6D4),
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Route Preferences
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E293B)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        "Route Preferences",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val total = routeTypeStats.values.sum().toFloat()
                    
                    RouteTypeBar(
                        routeType = RouteType.SMART,
                        count = routeTypeStats[RouteType.SMART] ?: 0,
                        percentage = ((routeTypeStats[RouteType.SMART] ?: 0) / total * 100).toInt(),
                        color = Color(0xFF3B82F6)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    RouteTypeBar(
                        routeType = RouteType.CHILL,
                        count = routeTypeStats[RouteType.CHILL] ?: 0,
                        percentage = ((routeTypeStats[RouteType.CHILL] ?: 0) / total * 100).toInt(),
                        color = Color(0xFFA855F7)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    RouteTypeBar(
                        routeType = RouteType.REGULAR,
                        count = routeTypeStats[RouteType.REGULAR] ?: 0,
                        percentage = ((routeTypeStats[RouteType.REGULAR] ?: 0) / total * 100).toInt(),
                        color = Color(0xFF64748B)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Recent Trips
            if (recentTrips.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E293B)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            "Recent Trips",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        recentTrips.take(5).forEach { trip ->
                            TripHistoryItem(trip)
                            if (trip != recentTrips.last()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                title,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
            
            Text(
                value,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun RouteTypeBar(
    routeType: RouteType,
    count: Int,
    percentage: Int,
    color: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                when (routeType) {
                    RouteType.SMART -> "Smart Route"
                    RouteType.CHILL -> "Chill Route"
                    RouteType.REGULAR -> "Regular Route"
                },
                color = Color.White,
                fontSize = 14.sp
            )
            
            Text(
                "$count trips ($percentage%)",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Color(0xFF334155), RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percentage / 100f)
                    .fillMaxHeight()
                    .background(color, RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
fun TripHistoryItem(trip: TripRecord) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                "${trip.origin} → ${trip.destination}",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                val routeColor = when (trip.routeType) {
                    RouteType.SMART -> Color(0xFF3B82F6)
                    RouteType.CHILL -> Color(0xFFA855F7)
                    RouteType.REGULAR -> Color(0xFF64748B)
                }
                
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(routeColor, CircleShape)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    trip.routeName,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    "%.1f km".format(trip.distance),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${trip.actualTime} min",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            
            if (trip.timeSaved > 0) {
                Text(
                    "+${trip.timeSaved / 60}min saved",
                    color = Color(0xFF10B981),
                    fontSize = 12.sp
                )
            }
        }
    }
}
