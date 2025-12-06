package org.aurora.ui.simulation

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
import org.aurora.simulation.OrchestrationStrategy
import org.aurora.simulation.SimulationEngine
import org.aurora.traffic.model.*

@Composable
fun SimulationDashboard(simulationEngine: SimulationEngine, onLogout: () -> Unit) {
    val isRunning by simulationEngine.isRunning.collectAsState()
    val statistics by simulationEngine.statistics.collectAsState()
    val strategy by simulationEngine.strategy.collectAsState()
    val fps by simulationEngine.fps.collectAsState()
    
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
            // Main simulation canvas
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Top bar
                TopBar(
                    isRunning = isRunning,
                    fps = fps,
                    onStart = { simulationEngine.start() },
                    onPause = { simulationEngine.pause() },
                    onReset = { 
                        simulationEngine.reset()
                        simulationEngine.cityMap.spawnVehicles(40)
                    },
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
                    SimulationCanvas(
                        cityMap = simulationEngine.cityMap,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            
            // Right sidebar with controls
            RightSidebar(
                strategy = strategy,
                statistics = statistics,
                onStrategyChange = { simulationEngine.setStrategy(it) }
            )
        }
    }
}

@Composable
fun TopBar(
    isRunning: Boolean,
    fps: Int,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onLogout: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = Color(0xFF06B6D4),
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    "Aurora Traffic Simulator",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // FPS Counter
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF06B6D4).copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "$fps FPS",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = Color(0xFF06B6D4),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                // Play/Pause
                IconButton(
                    onClick = { if (isRunning) onPause() else onStart() },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF06B6D4), CircleShape)
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
                        .background(Color(0xFFEF4444), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Reset",
                        tint = Color.White
                    )
                }
                
                // Logout
                IconButton(
                    onClick = onLogout,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF64748B), CircleShape)
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
fun SimulationCanvas(cityMap: CityMap, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.padding(32.dp)) {
        val scale = 1.2f
        
        // Draw roads
        cityMap.roads.values.forEach { road ->
            drawRoad(road, scale)
        }
        
        // Draw intersections
        cityMap.intersections.values.forEach { intersection ->
            drawIntersection(intersection, scale)
        }
        
        // Draw vehicles
        cityMap.vehicles.values.forEach { vehicle ->
            if (!vehicle.hasReachedDestination) {
                drawVehicle(vehicle, scale)
            }
        }
    }
}

private fun DrawScope.drawRoad(road: Road, scale: Float) {
    val color = when (road.congestionLevel) {
        CongestionLevel.FREE -> Color(0xFF10B981)
        CongestionLevel.MODERATE -> Color(0xFFFBBF24)
        CongestionLevel.HEAVY -> Color(0xFFF97316)
        CongestionLevel.GRIDLOCK -> Color(0xFFEF4444)
    }
    
    road.positions.zipWithNext().forEach { (start, end) ->
        drawLine(
            color = color,
            start = Offset(start.x * scale, start.y * scale),
            end = Offset(end.x * scale, end.y * scale),
            strokeWidth = 8f
        )
    }
}

private fun DrawScope.drawIntersection(intersection: Intersection, scale: Float) {
    val pos = intersection.position
    
    // Draw intersection box
    drawCircle(
        color = Color(0xFF475569),
        radius = 12f,
        center = Offset(pos.x * scale, pos.y * scale)
    )
    
    // Draw traffic light indicators
    intersection.trafficLights.values.firstOrNull()?.let { light ->
        val lightColor = when (light.state) {
            LightState.RED -> Color(0xFFEF4444)
            LightState.YELLOW -> Color(0xFFFBBF24)
            LightState.GREEN -> Color(0xFF10B981)
        }
        
        drawCircle(
            color = lightColor,
            radius = 6f,
            center = Offset(pos.x * scale, pos.y * scale)
        )
    }
}

private fun DrawScope.drawVehicle(vehicle: Vehicle, scale: Float) {
    val color = if (vehicle.isRerouted) {
        Color(0xFF06B6D4) // Cyan for rerouted
    } else when (vehicle.color) {
        VehicleColor.BLUE -> Color(0xFF3B82F6)
        VehicleColor.RED -> Color(0xFFEF4444)
        VehicleColor.GREEN -> Color(0xFF10B981)
        VehicleColor.YELLOW -> Color(0xFFFBBF24)
        VehicleColor.PURPLE -> Color(0xFFA855F7)
    }
    
    drawCircle(
        color = color,
        radius = if (vehicle.isWaiting) 5f else 4f,
        center = Offset(vehicle.position.x * scale, vehicle.position.y * scale)
    )
}

@Composable
fun RightSidebar(
    strategy: OrchestrationStrategy,
    statistics: org.aurora.simulation.SimulationStatistics,
    onStrategyChange: (OrchestrationStrategy) -> Unit
) {
    Card(
        modifier = Modifier
            .width(350.dp)
            .fillMaxHeight()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Strategy Selector
            Text(
                "Orchestration Strategy",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            StrategyButton(
                title = "Default",
                description = "Fixed timers",
                icon = Icons.Default.Circle,
                isSelected = strategy == OrchestrationStrategy.DEFAULT,
                color = Color(0xFFEF4444),
                onClick = { onStrategyChange(OrchestrationStrategy.DEFAULT) }
            )
            
            StrategyButton(
                title = "Predictive",
                description = "AI-adjusted lights",
                icon = Icons.Default.ShowChart,
                isSelected = strategy == OrchestrationStrategy.PREDICTIVE,
                color = Color(0xFFFBBF24),
                onClick = { onStrategyChange(OrchestrationStrategy.PREDICTIVE) }
            )
            
            StrategyButton(
                title = "Aurora",
                description = "Full optimization",
                icon = Icons.Default.AutoAwesome,
                isSelected = strategy == OrchestrationStrategy.AURORA,
                color = Color(0xFF06B6D4),
                onClick = { onStrategyChange(OrchestrationStrategy.AURORA) }
            )
            
            Divider(color = Color.White.copy(alpha = 0.2f))
            
            // Statistics
            Text(
                "Live Statistics",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            StatCard("Active Vehicles", "${statistics.activeVehicles}", Icons.Default.DirectionsCar)
            StatCard("Avg Speed", "${statistics.averageSpeed.toInt()} km/h", Icons.Default.Speed)
            StatCard("Waiting Vehicles", "${statistics.waitingVehicles}", Icons.Default.Timer)
            StatCard("Queue Length", "${statistics.totalQueueLength}", Icons.Default.ViewList)
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Congestion Summary
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF06B6D4).copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Road Congestion",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        CongestionIndicator("Free", statistics.freeRoads, Color(0xFF10B981))
                        CongestionIndicator("Mod", statistics.moderateRoads, Color(0xFFFBBF24))
                        CongestionIndicator("Heavy", statistics.heavyRoads, Color(0xFFF97316))
                    }
                }
            }
        }
    }
}

@Composable
fun StrategyButton(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) color else Color(0xFF0F172A),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Column {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(description, fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F172A)
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color(0xFF06B6D4),
                    modifier = Modifier.size(20.dp)
                )
                Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
            }
            Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RowScope.CongestionIndicator(label: String, count: Int, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.weight(1f)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$count",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            label,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 10.sp
        )
    }
}
