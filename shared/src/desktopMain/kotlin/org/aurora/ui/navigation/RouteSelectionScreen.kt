package org.aurora.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.aurora.navigation.model.NavigationRoute
import org.aurora.navigation.model.RouteType
import org.aurora.navigation.model.TrafficLevel
import org.jetbrains.skia.Image as SkiaImage
import java.net.URL

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
    Row(modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFFF5F7FA))
    ) {
        // Left sidebar
        Column(
            modifier = Modifier
                .fillMaxWidth(0.38f)
                .fillMaxHeight()
                .background(Color.White)
                .verticalScroll(rememberScrollState())
        ) {
            // Modern header with blue theme
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E88E5))
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Column {
                        Text(
                            "Best Routes",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "$origin → $destination",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                    }
                }
            }
            
            Column(modifier = Modifier.padding(20.dp)) {
                // Route cards
                routes.forEach { route ->
                    ModernRouteCard(
                        route = route,
                        isSelected = route.type == selectedType,
                        onClick = { onRouteSelected(route.type) }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Modern gradient button
                Button(
                    onClick = onStartNavigation,
                    enabled = selectedType != null,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6366F1),
                        disabledContainerColor = Color(0xFFE5E7EB)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 8.dp
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Navigation,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Start Navigation",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
        
        // Right side - Minimal map
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Map Preview",
                color = Color(0xFF9E9E9E),
                fontSize = 14.sp
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
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) Color(0xFFF5F5F5) else Color.White,
        border = BorderStroke(1.dp, if (isSelected) Color(0xFF212121) else Color(0xFFE0E0E0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                route.name,
                color = Color(0xFF212121),
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "${route.estimatedTime} min",
                    color = Color(0xFF757575),
                    fontSize = 13.sp
                )
                Text(
                    "${String.format("%.1f", route.distance)} km",
                    color = Color(0xFF757575),
                    fontSize = 13.sp
                )
                Text(
                    "${route.safetyScore}%",
                    color = Color(0xFF757575),
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun ModernRouteCard(
    route: NavigationRoute,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val (badgeColor, badgeText) = when (route.type) {
        RouteType.SMART -> Color(0xFF6366F1) to "Recommended"
        RouteType.CHILL -> Color(0xFF10B981) to "Scenic"
        RouteType.REGULAR -> Color(0xFF6B7280) to "Standard"
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFF0F4FF) else Color.White
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 6.dp else 2.dp
        ),
        border = if (isSelected) BorderStroke(2.dp, Color(0xFF6366F1)) else null
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(badgeColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            badgeText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }
                    
                    Text(
                        route.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                }
                
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0xFF6366F1), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                RouteInfoPill(
                    icon = Icons.Default.Timer,
                    value = "${route.estimatedTime} min",
                    color = Color(0xFF6366F1)
                )
                RouteInfoPill(
                    icon = Icons.Default.Route,
                    value = "${String.format("%.1f", route.distance)} km",
                    color = Color(0xFF8B5CF6)
                )
                RouteInfoPill(
                    icon = Icons.Default.Shield,
                    value = "${route.safetyScore}%",
                    color = Color(0xFF10B981)
                )
            }
            
            if (route.hazardCount > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "${route.hazardCount} hazards detected",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            }
        }
    }
}

@Composable
fun RouteInfoPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Text(
            value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1F2937)
        )
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

// Removed NetworkImage and loadImageFromUrl functions for minimal design
