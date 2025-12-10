package org.aurora.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.aurora.android.models.RouteGenerator
import org.aurora.android.models.RouteOption
import org.aurora.android.models.TrafficLevel

@Composable
fun AlternativeRoutesScreen(
    origin: String,
    destination: String,
    onRouteSelected: (RouteOption) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val routes = remember { RouteGenerator.generateAlternatives(origin, destination) }
    var selectedRoute by remember { mutableStateOf<RouteOption?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // Top Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Choose Route",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )
                    Text(
                        text = "$origin → $destination",
                        fontSize = 12.sp,
                        color = Color(0xFF757575)
                    )
                }
            }
        }
        
        // Route Options List
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(routes) { route ->
                RouteOptionCard(
                    route = route,
                    isSelected = selectedRoute?.id == route.id,
                    onClick = {
                        selectedRoute = route
                        onRouteSelected(route)
                    }
                )
            }
        }
    }
}

@Composable
fun RouteOptionCard(
    route: RouteOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFE3F2FD) else Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF1E88E5))
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = when (route.type) {
                            org.aurora.android.models.RouteType.FASTEST -> Color(0xFF1E88E5)
                            org.aurora.android.models.RouteType.SHORTEST -> Color(0xFF4CAF50)
                            org.aurora.android.models.RouteType.ECO_FRIENDLY -> Color(0xFF8BC34A)
                            org.aurora.android.models.RouteType.AVOID_TOLLS -> Color(0xFFFF9800)
                            org.aurora.android.models.RouteType.AVOID_HIGHWAYS -> Color(0xFF9C27B0)
                            org.aurora.android.models.RouteType.SCENIC -> Color(0xFF00BCD4)
                        },
                        modifier = Modifier.size(8.dp)
                    ) {}
                    
                    Text(
                        text = route.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )
                }
                
                if (isSelected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = Color(0xFF1E88E5),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Main Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                RouteStatItem(
                    icon = Icons.Default.DateRange,
                    value = "${route.duration} min",
                    label = "Time"
                )
                RouteStatItem(
                    icon = Icons.Default.Star,
                    value = "${route.distance} km",
                    label = "Distance"
                )
                if (route.tollCost > 0) {
                    RouteStatItem(
                        icon = Icons.Default.ShoppingCart,
                        value = "₱${route.tollCost.toInt()}",
                        label = "Toll"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Traffic Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = getTrafficColor(route.trafficLevel),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Traffic: ${route.trafficLevel.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    fontSize = 13.sp,
                    color = getTrafficColor(route.trafficLevel),
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Highlights
            if (route.highlights.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                
                route.highlights.forEach { highlight ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.size(6.dp)
                        ) {}
                        Text(
                            text = highlight,
                            fontSize = 12.sp,
                            color = Color(0xFF757575)
                        )
                    }
                }
            }
            
            // Warnings
            if (route.warnings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                
                route.warnings.forEach { warning ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = warning,
                            fontSize = 12.sp,
                            color = Color(0xFFFF9800),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // CO2 Savings
            if (route.co2Saved > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFE8F5E9)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Saves ${route.co2Saved}kg CO₂",
                            fontSize = 11.sp,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RouteStatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
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
            fontSize = 15.sp,
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

fun getTrafficColor(level: TrafficLevel): Color {
    return when (level) {
        TrafficLevel.LIGHT -> Color(0xFF4CAF50)
        TrafficLevel.MODERATE -> Color(0xFFFF9800)
        TrafficLevel.HEAVY -> Color(0xFFE91E63)
        TrafficLevel.SEVERE -> Color(0xFFD32F2F)
    }
}
