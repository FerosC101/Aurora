package com.nextcs.aurora.ui

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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.nextcs.aurora.navigation.model.NavigationRoute
import com.nextcs.aurora.navigation.model.RouteType
import com.nextcs.aurora.navigation.model.TrafficLevel
// Removed desktop-specific imports: toComposeImageBitmap, skia

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
    Column(modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFFF5F7FA))
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1976D2))
                .padding(20.dp)
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
        
        // Routes list
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            routes.forEach { route ->
                ModernRouteCard(
                    route = route,
                    isSelected = route.type == selectedType,
                    onClick = { onRouteSelected(route.type) }
                )
            }
        }
        
        // Bottom button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(20.dp)
        ) {
            Button(
                onClick = onStartNavigation,
                enabled = selectedType != null,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1976D2),
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
                        Icons.Default.Place,
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
                    icon = Icons.Default.Star,
                    value = "${route.estimatedTime} min",
                    color = Color(0xFF6366F1)
                )
                RouteInfoPill(
                    icon = Icons.Default.Place,
                    value = "${String.format("%.1f", route.distance)} km",
                    color = Color(0xFF8B5CF6)
                )
                RouteInfoPill(
                    icon = Icons.Default.Lock,
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
            
            // Interactive Map Preview
            Spacer(modifier = Modifier.height(16.dp))
            InteractiveRouteMap(
                route = route,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
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

@Composable
fun InteractiveRouteMap(
    route: NavigationRoute,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var scale by remember { mutableStateOf(1f) }
    
    Box(
        modifier = modifier
            .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        offsetX += pan.x
                        offsetY += pan.y
                        scale = (scale * zoom).coerceIn(0.5f, 3f)
                    }
                }
        ) {
            val points = generateRoutePoints(route, size.width, size.height, offsetX, offsetY, scale)
            
            if (points.isNotEmpty()) {
                // Draw route path
                val path = Path()
                path.moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { point ->
                    path.lineTo(point.x, point.y)
                }
                
                // Draw route line
                drawPath(
                    path = path,
                    color = Color(0xFF1976D2),
                    style = Stroke(width = 4.dp.toPx() * scale)
                )
                
                // Draw start marker (green circle)
                drawCircle(
                    color = Color(0xFF10B981),
                    radius = 8.dp.toPx() * scale,
                    center = points.first()
                )
                
                // Draw end marker (red circle)
                drawCircle(
                    color = Color(0xFFEF4444),
                    radius = 8.dp.toPx() * scale,
                    center = points.last()
                )
                
                // Draw hazard markers along the route
                route.detectedHazards.forEach { hazard ->
                    // Use hazard.distance (distance from start in meters)
                    val hazardIndex = if (route.distance > 0) {
                        (points.size * hazard.distance / (route.distance * 1000)).toInt()
                            .coerceIn(0, points.size - 1)
                    } else {
                        points.size / 2
                    }
                    drawCircle(
                        color = Color(0xFFF59E0B),
                        radius = 6.dp.toPx() * scale,
                        center = points[hazardIndex]
                    )
                }
            }
        }
        
        // Controls overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
        ) {
            // Zoom in button
            IconButton(
                onClick = { scale = (scale * 1.2f).coerceAtMost(3f) },
                modifier = Modifier
                    .size(32.dp)
                    .background(Color.White, CircleShape)
                    .border(1.dp, Color(0xFFE5E7EB), CircleShape)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Zoom in",
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.size(18.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Zoom out button
            IconButton(
                onClick = { scale = (scale / 1.2f).coerceAtLeast(0.5f) },
                modifier = Modifier
                    .size(32.dp)
                    .background(Color.White, CircleShape)
                    .border(1.dp, Color(0xFFE5E7EB), CircleShape)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Zoom out",
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.size(18.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Reset button
            IconButton(
                onClick = { 
                    offsetX = 0f
                    offsetY = 0f
                    scale = 1f
                },
                modifier = Modifier
                    .size(32.dp)
                    .background(Color.White, CircleShape)
                    .border(1.dp, Color(0xFFE5E7EB), CircleShape)
            ) {
                Icon(
                    Icons.Default.Place,
                    contentDescription = "Reset view",
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        
        // Info overlay
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                "Drag to pan • Scroll to zoom",
                fontSize = 10.sp,
                color = Color(0xFF6B7280)
            )
        }
    }
}

private fun generateRoutePoints(
    route: NavigationRoute,
    width: Float,
    height: Float,
    offsetX: Float,
    offsetY: Float,
    scale: Float
): List<Offset> {
    val waypoints = route.waypoints
    
    // If we have real lat/lng waypoints, use those for more accurate visualization
    if (route.realLatLngWaypoints.isNotEmpty()) {
        val latValues = route.realLatLngWaypoints.map { it.first }
        val lngValues = route.realLatLngWaypoints.map { it.second }
        
        val latRange = (latValues.maxOrNull() ?: 0.0) - (latValues.minOrNull() ?: 0.0)
        val lngRange = (lngValues.maxOrNull() ?: 0.0) - (lngValues.minOrNull() ?: 0.0)
        val minLat = latValues.minOrNull() ?: 0.0
        val minLng = lngValues.minOrNull() ?: 0.0
        
        val mapWidth = width * 0.8f
        val mapHeight = height * 0.8f
        val mapLeft = (width - mapWidth) / 2
        val mapTop = (height - mapHeight) / 2
        
        return route.realLatLngWaypoints.map { (lat, lng) ->
            val normalizedLat = if (latRange > 0) (lat - minLat) / latRange else 0.5
            val normalizedLng = if (lngRange > 0) (lng - minLng) / lngRange else 0.5
            
            Offset(
                (mapLeft + normalizedLng.toFloat() * mapWidth) * scale + offsetX,
                (mapTop + (1 - normalizedLat.toFloat()) * mapHeight) * scale + offsetY
            )
        }
    }
    
    // Fallback: Generate simple path based on waypoints or simulated data
    if (waypoints.isEmpty()) {
        val centerX = width / 2
        val centerY = height / 2
        return listOf(
            Offset(centerX - 100f * scale + offsetX, centerY + offsetY),
            Offset(centerX - 50f * scale + offsetX, centerY - 30f * scale + offsetY),
            Offset(centerX + offsetX, centerY - 20f * scale + offsetY),
            Offset(centerX + 50f * scale + offsetX, centerY - 40f * scale + offsetY),
            Offset(centerX + 100f * scale + offsetX, centerY + offsetY)
        )
    }
    
    // Use Position objects (x, y coordinates) for visualization
    val xValues = waypoints.map { it.x }
    val yValues = waypoints.map { it.y }
    
    val xRange = (xValues.maxOrNull() ?: 0f) - (xValues.minOrNull() ?: 0f)
    val yRange = (yValues.maxOrNull() ?: 0f) - (yValues.minOrNull() ?: 0f)
    val minX = xValues.minOrNull() ?: 0f
    val minY = yValues.minOrNull() ?: 0f
    
    val mapWidth = width * 0.8f
    val mapHeight = height * 0.8f
    val mapLeft = (width - mapWidth) / 2
    val mapTop = (height - mapHeight) / 2
    
    return waypoints.map { waypoint ->
        val normalizedX = if (xRange > 0) (waypoint.x - minX) / xRange else 0.5f
        val normalizedY = if (yRange > 0) (waypoint.y - minY) / yRange else 0.5f
        
        Offset(
            (mapLeft + normalizedX * mapWidth) * scale + offsetX,
            (mapTop + (1 - normalizedY) * mapHeight) * scale + offsetY
        )
    }
}

// Removed NetworkImage and loadImageFromUrl functions for minimal design
