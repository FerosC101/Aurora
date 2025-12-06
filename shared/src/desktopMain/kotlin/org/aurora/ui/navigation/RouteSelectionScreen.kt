package org.aurora.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import org.aurora.navigation.model.NavigationRoute
import org.aurora.navigation.model.RouteType
import org.aurora.navigation.model.TrafficLevel

@Composable
fun RouteSelectionScreen(
    origin: String,
    destination: String,
    routes: List<NavigationRoute>,
    selectedType: RouteType?,
    onRouteSelected: (RouteType) -> Unit,
    onStartNavigation: () -> Unit,
    onBack: () -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
        // Left sidebar with route cards
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.35f)
                .fillMaxHeight(),
            color = Color(0xFF1E293B)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
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
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.ElectricBolt,
                                contentDescription = null,
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Aurora Rider",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            "Smart & safe routes for riders",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Change destination link
                TextButton(onClick = onBack) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Change destination",
                        color = Color(0xFF3B82F6),
                        fontSize = 14.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Route cards
                routes.forEach { route ->
                    RouteCard(
                        route = route,
                        isSelected = route.type == selectedType,
                        onClick = { onRouteSelected(route.type) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Start Navigation button
                Button(
                    onClick = onStartNavigation,
                    enabled = selectedType != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3B82F6),
                        disabledContainerColor = Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        "Start Navigation",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Smart Route Benefits (if selected)
                if (selectedType == RouteType.SMART) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF334155)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Smart Route Benefits:",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "• Clear road ahead - optimal conditions\n• Traffic prediction: Light for next 5 minutes\n• Route efficiency: 97%",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
        
        // Right side - Map preview
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Map Preview\n\n${origin} → ${destination}",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Light
            )
        }
    }
}

@Composable
fun RouteCard(
    route: NavigationRoute,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when (route.type) {
        RouteType.SMART -> if (isSelected) Color(0xFF3B82F6) else Color(0xFF1E3A5F)
        RouteType.CHILL -> if (isSelected) Color(0xFFA855F7) else Color(0xFF4C1D95)
        RouteType.REGULAR -> if (isSelected) Color(0xFF64748B) else Color(0xFF334155)
    }
    
    val borderColor = when (route.type) {
        RouteType.SMART -> Color(0xFF3B82F6)
        RouteType.CHILL -> Color(0xFFA855F7)
        RouteType.REGULAR -> Color(0xFF64748B)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(
                if (isSelected) Modifier.border(2.dp, borderColor, RoundedCornerShape(16.dp))
                else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header with icon and badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        when (route.type) {
                            RouteType.SMART -> Icons.Default.AutoAwesome
                            RouteType.CHILL -> Icons.Default.Park
                            RouteType.REGULAR -> Icons.Default.Route
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        route.name,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (route.type == RouteType.SMART) {
                    Surface(
                        color = Color(0xFF10B981),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "Recommended",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                route.description,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 13.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "${route.estimatedTime}",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "min",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        "ETA",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
                
                if (route.timeSavedVsBaseline != 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (route.timeSavedVsBaseline > 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = if (route.timeSavedVsBaseline > 0) Color(0xFF10B981) else Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                "${if (route.timeSavedVsBaseline > 0) "+" else ""}${route.timeSavedVsBaseline}",
                                color = if (route.timeSavedVsBaseline > 0) Color(0xFF10B981) else Color(0xFFEF4444),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            "Min saved",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Details grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RouteDetail("Safety", "${route.safetyScore}%")
                RouteDetail("Traffic", route.trafficLevel.name.replace("_", " "))
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RouteDetail("Hazards", "${route.hazardCount}")
                if (route.scenicPoints > 0) {
                    RouteDetail("View points", "${route.scenicPoints}")
                }
            }
        }
    }
}

@Composable
fun RouteDetail(label: String, value: String) {
    Column {
        Text(
            label,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp
        )
        Text(
            value,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
