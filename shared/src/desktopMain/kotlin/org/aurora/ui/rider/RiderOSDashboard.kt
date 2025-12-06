package org.aurora.ui.rider

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.aurora.rider.model.*
import org.aurora.rider.simulation.*
import org.aurora.traffic.model.*

/**
 * Aurora 2.0 - RiderOS Competition Dashboard
 * Designed to WIN the 2025 Kotlin Multiplatform Competition
 */
@Composable
fun RiderOSDashboard(simulationEngine: RiderSimulationEngine, onLogout: () -> Unit) {
    val isRunning by simulationEngine.isRunning.collectAsState()
    val statistics by simulationEngine.statistics.collectAsState()
    val strategy by simulationEngine.strategy.collectAsState()
    val fps by simulationEngine.fps.collectAsState()
    val timeOfDay by simulationEngine.timeOfDay.collectAsState()
    val weather by simulationEngine.weather.collectAsState()
    
    // Update loop
    LaunchedEffect(Unit) {
        while (true) {
            simulationEngine.update()
            delay(16) // ~60 FPS
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Main simulation area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Top bar with controls
                RiderOSTopBar(
                    isRunning = isRunning,
                    fps = fps,
                    timeOfDay = timeOfDay,
                    weather = weather,
                    onStart = { simulationEngine.start() },
                    onPause = { simulationEngine.pause() },
                    onReset = { 
                        simulationEngine.reset()
                        simulationEngine.cityMap.spawnRiders(30)
                    },
                    onWeatherChange = { simulationEngine.setWeather(it) },
                    onLogout = onLogout
                )
                
                // Simulation canvas
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E293B)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    RiderSimulationCanvas(
                        cityMap = simulationEngine.cityMap,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            
            // Right sidebar with strategy selector and stats
            RiderOSSidebar(
                strategy = strategy,
                statistics = statistics,
                onStrategyChange = { simulationEngine.setStrategy(it) }
            )
        }
    }
}

@Composable
fun RiderOSTopBar(
    isRunning: Boolean,
    fps: Int,
    timeOfDay: Float,
    weather: Weather,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onWeatherChange: (Weather) -> Unit,
    onLogout: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        color = Color(0xFF1E293B)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo and title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.DirectionsBike,
                    contentDescription = "RiderOS",
                    tint = Color(0xFF3B82F6),
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "AURORA 2.0",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "RiderOS - AI Rider Optimization",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
            }
            
            // Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time of day indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0xFF334155), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        if (timeOfDay in 6f..18f) Icons.Default.WbSunny else Icons.Default.NightsStay,
                        contentDescription = "Time",
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        String.format("%02d:%02d", timeOfDay.toInt(), ((timeOfDay % 1) * 60).toInt()),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Weather toggle
                IconButton(
                    onClick = {
                        val nextWeather = when (weather) {
                            Weather.CLEAR -> Weather.LIGHT_RAIN
                            Weather.LIGHT_RAIN -> Weather.HEAVY_RAIN
                            Weather.HEAVY_RAIN -> Weather.CLEAR
                            Weather.NIGHT -> Weather.CLEAR
                        }
                        onWeatherChange(nextWeather)
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF334155), CircleShape)
                ) {
                    Icon(
                        when (weather) {
                            Weather.CLEAR -> Icons.Default.WbSunny
                            Weather.LIGHT_RAIN, Weather.HEAVY_RAIN -> Icons.Default.WaterDrop
                            Weather.NIGHT -> Icons.Default.NightsStay
                        },
                        contentDescription = "Weather",
                        tint = Color.White
                    )
                }
                
                // Play/Pause
                IconButton(
                    onClick = { if (isRunning) onPause() else onStart() },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF3B82F6), CircleShape)
                ) {
                    Icon(
                        if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isRunning) "Pause" else "Play",
                        tint = Color.White
                    )
                }
                
                // Reset
                IconButton(
                    onClick = onReset,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF10B981), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Reset",
                        tint = Color.White
                    )
                }
                
                // FPS Counter
                Text(
                    "$fps FPS",
                    color = Color(0xFF10B981),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Color(0xFF334155), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
                
                // Logout
                IconButton(
                    onClick = onLogout,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFEF4444), CircleShape)
                ) {
                    Icon(
                        Icons.Default.ExitToApp,
                        contentDescription = "Logout",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun RiderOSSidebar(
    strategy: RiderStrategy,
    statistics: RiderStatistics,
    onStrategyChange: (RiderStrategy) -> Unit
) {
    Surface(
        modifier = Modifier
            .width(360.dp)
            .fillMaxHeight(),
        color = Color(0xFF1E293B)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Strategy Selector
            Text(
                "🎯 Strategy Comparison",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StrategyCard(
                    name = "Baseline (Google Maps)",
                    description = "Standard routing",
                    color = Color(0xFF94A3B8),
                    isSelected = strategy == RiderStrategy.BASELINE,
                    onClick = { onStrategyChange(RiderStrategy.BASELINE) }
                )
                
                StrategyCard(
                    name = "RiderOS Smart",
                    description = "Rider-aware routing",
                    color = Color(0xFF3B82F6),
                    isSelected = strategy == RiderStrategy.RIDER_OS,
                    onClick = { onStrategyChange(RiderStrategy.RIDER_OS) }
                )
                
                StrategyCard(
                    name = "Aurora AI",
                    description = "Full AI optimization",
                    color = Color(0xFF10B981),
                    isSelected = strategy == RiderStrategy.AURORA_AI,
                    onClick = { onStrategyChange(RiderStrategy.AURORA_AI) }
                )
            }
            
            Divider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)
            
            // Statistics
            Text(
                "📊 Live Statistics",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Active Riders", "${statistics.activeRiders}/${statistics.totalRiders}", Color(0xFF3B82F6))
                StatCard("Avg Speed", "${statistics.averageSpeed.toInt()} km/h", Color(0xFF10B981))
                StatCard("Hazards Avoided", "${statistics.totalHazardsAvoided}", Color(0xFFF59E0B))
                StatCard("Safety Score", "${statistics.averageSafetyScore.toInt()}/100", Color(0xFF10B981))
                StatCard("Near Misses", "${statistics.totalNearMisses}", Color(0xFFEF4444))
                StatCard("Avg Stress", "${(statistics.averageStress * 100).toInt()}%", Color(0xFFEF4444))
                StatCard("Time Saved", "${statistics.timeSavedVsBaseline.toInt()}s", Color(0xFF10B981))
            }
            
            Divider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)
            
            // Rider type breakdown
            Text(
                "🏍️ Rider Types",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RiderTypeRow("Delivery", statistics.deliveryRiders, Color(0xFFF97316))
                RiderTypeRow("Commuter", statistics.commuters, Color(0xFF3B82F6))
                RiderTypeRow("E-Bike", statistics.eBikes, Color(0xFF10B981))
                RiderTypeRow("Scooter", statistics.scooters, Color(0xFFA855F7))
                RiderTypeRow("Personal", statistics.personalMotorcycles, Color(0xFF06B6D4))
            }
        }
    }
}

@Composable
fun StrategyCard(
    name: String,
    description: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) color else Color(0xFF334155)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                name,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                description,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun StatCard(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF334155), RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
        Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun RiderTypeRow(label: String, count: Int, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
        }
        Text("$count", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun RiderSimulationCanvas(cityMap: RiderCityMap, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.padding(32.dp)) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        
        // Calculate scale to fit city
        val cityBounds = calculateCityBounds(cityMap)
        val scaleX = canvasWidth / cityBounds.width
        val scaleY = canvasHeight / cityBounds.height
        val scale = minOf(scaleX, scaleY) * 0.9f
        
        val offsetX = (canvasWidth - cityBounds.width * scale) / 2 - cityBounds.left * scale
        val offsetY = (canvasHeight - cityBounds.height * scale) / 2 - cityBounds.top * scale
        
        // Draw roads with congestion colors
        cityMap.roads.values.forEach { road ->
            drawRoad(road, scale, offsetX, offsetY)
        }
        
        // Draw hazards
        cityMap.hazardDetector.getActiveHazards().forEach { hazard ->
            drawHazard(hazard, scale, offsetX, offsetY)
        }
        
        // Draw intersections
        cityMap.intersections.values.forEach { intersection ->
            drawIntersection(intersection, scale, offsetX, offsetY)
        }
        
        // Draw riders
        cityMap.riders.values.filter { !it.hasReachedDestination }.forEach { rider ->
            drawRider(rider, scale, offsetX, offsetY)
        }
    }
}

private fun calculateCityBounds(cityMap: RiderCityMap): Bounds {
    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = Float.MIN_VALUE
    var maxY = Float.MIN_VALUE
    
    cityMap.intersections.values.forEach { intersection ->
        minX = minOf(minX, intersection.position.x)
        minY = minOf(minY, intersection.position.y)
        maxX = maxOf(maxX, intersection.position.x)
        maxY = maxOf(maxY, intersection.position.y)
    }
    
    return Bounds(minX, minY, maxX - minX, maxY - minY)
}

private fun DrawScope.drawRoad(road: Road, scale: Float, offsetX: Float, offsetY: Float) {
    val start = Offset(
        road.start.x * scale + offsetX,
        road.start.y * scale + offsetY
    )
    val end = Offset(
        road.end.x * scale + offsetX,
        road.end.y * scale + offsetY
    )
    
    val color = when (road.congestionLevel) {
        CongestionLevel.FREE -> Color(0xFF10B981)
        CongestionLevel.MODERATE -> Color(0xFFF59E0B)
        CongestionLevel.HEAVY -> Color(0xFFF97316)
        CongestionLevel.GRIDLOCK -> Color(0xFFEF4444)
    }
    
    drawLine(
        color = color,
        start = start,
        end = end,
        strokeWidth = 6f
    )
}

private fun DrawScope.drawHazard(hazard: Hazard, scale: Float, offsetX: Float, offsetY: Float) {
    val center = Offset(
        hazard.position.x * scale + offsetX,
        hazard.position.y * scale + offsetY
    )
    
    val color = when (hazard.severity) {
        HazardSeverity.LOW -> Color(0xFFFBBF24)
        HazardSeverity.MODERATE -> Color(0xFFF97316)
        HazardSeverity.HIGH -> Color(0xFFEF4444)
        HazardSeverity.CRITICAL -> Color(0xFFDC2626)
    }
    
    drawCircle(
        color = color.copy(alpha = 0.3f),
        radius = hazard.radius * scale * 0.5f,
        center = center
    )
    
    drawCircle(
        color = color,
        radius = 8f,
        center = center
    )
}

private fun DrawScope.drawIntersection(intersection: Intersection, scale: Float, offsetX: Float, offsetY: Float) {
    val center = Offset(
        intersection.position.x * scale + offsetX,
        intersection.position.y * scale + offsetY
    )
    
    drawCircle(
        color = Color(0xFF1E293B),
        radius = 12f,
        center = center
    )
    
    // Traffic light indicator
    intersection.trafficLight?.let { light ->
        val lightColor = when (light.state) {
            LightState.RED -> Color(0xFFEF4444)
            LightState.YELLOW -> Color(0xFFFBBF24)
            LightState.GREEN -> Color(0xFF10B981)
        }
        
        drawCircle(
            color = lightColor,
            radius = 6f,
            center = center
        )
    }
}

private fun DrawScope.drawRider(rider: Rider, scale: Float, offsetX: Float, offsetY: Float) {
    val center = Offset(
        rider.position.x * scale + offsetX,
        rider.position.y * scale + offsetY
    )
    
    val color = when (rider.color) {
        RiderColor.DELIVERY_ORANGE -> Color(0xFFF97316)
        RiderColor.COMMUTER_BLUE -> Color(0xFF3B82F6)
        RiderColor.EBIKE_GREEN -> Color(0xFF10B981)
        RiderColor.SCOOTER_PURPLE -> Color(0xFFA855F7)
        RiderColor.PERSONAL_CYAN -> Color(0xFF06B6D4)
    }
    
    // Rerouted indicator
    if (rider.isRerouted) {
        drawCircle(
            color = color.copy(alpha = 0.3f),
            radius = 12f,
            center = center
        )
    }
    
    drawCircle(
        color = color,
        radius = 6f,
        center = center
    )
}

data class Bounds(val left: Float, val top: Float, val width: Float, val height: Float)
