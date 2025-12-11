package org.aurora.android.ui.screens

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.LatLng
import org.aurora.android.location.LocationService
import org.aurora.android.models.Waypoint
import org.aurora.android.traffic.model.Position

@Composable
fun MultiStopPlanningScreen(
    onStartNavigation: (List<Waypoint>) -> Unit,
    onBack: () -> Unit,
    onMapPicker: (String, LatLng?, (LatLng, String) -> Unit) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val locationService = remember { LocationService(context) }
    
    var waypoints by remember { mutableStateOf(listOf<Waypoint>()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var origin by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var originLocation by remember { mutableStateOf<LatLng?>(null) }
    var destinationLocation by remember { mutableStateOf<LatLng?>(null) }
    var newStopName by remember { mutableStateOf("") }
    var stopDuration by remember { mutableStateOf(5) }
    
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    
                    Text(
                        text = "Multi-Stop Route",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121),
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (waypoints.size >= 2) {
                        TextButton(
                            onClick = {
                                // Optimize route
                                waypoints = optimizeWaypoints(waypoints)
                            }
                        ) {
                            Icon(
                                Icons.Default.Build,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Optimize")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Origin
                OutlinedTextField(
                    value = origin,
                    onValueChange = { 
                        origin = it
                        originLocation = null
                    },
                    placeholder = { Text("Starting point") },
                    trailingIcon = {
                        Row {
                            // Use Current Location
                            IconButton(
                                onClick = {
                                    val currentLoc = locationService.getLastKnownLocation()
                                    if (currentLoc != null) {
                                        originLocation = currentLoc
                                        origin = locationService.formatLocationToAddress(currentLoc)
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = "Use Current Location",
                                    tint = Color(0xFF4CAF50)
                                )
                            }
                            // Pick on Map
                            IconButton(
                                onClick = {
                                    onMapPicker("Select Starting Point", originLocation) { location, address ->
                                        originLocation = location
                                        origin = address
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Place,
                                    contentDescription = "Pick on Map",
                                    tint = Color(0xFF1E88E5)
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Destination
                OutlinedTextField(
                    value = destination,
                    onValueChange = { 
                        destination = it
                        destinationLocation = null
                    },
                    placeholder = { Text("Final destination") },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                onMapPicker("Select Final Destination", destinationLocation) { location, address ->
                                    destinationLocation = location
                                    destination = address
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Place,
                                contentDescription = "Pick on Map",
                                tint = Color(0xFF1E88E5)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        }
        
        // Waypoints List
        if (waypoints.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Stops (${waypoints.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF757575)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                itemsIndexed(waypoints) { index, waypoint ->
                    WaypointCard(
                        waypoint = waypoint,
                        index = index,
                        onRemove = {
                            waypoints = waypoints.filter { it.id != waypoint.id }
                        },
                        onMoveUp = {
                            if (index > 0) {
                                val mutable = waypoints.toMutableList()
                                mutable[index] = mutable[index - 1].also {
                                    mutable[index - 1] = mutable[index]
                                }
                                waypoints = mutable
                            }
                        },
                        onMoveDown = {
                            if (index < waypoints.size - 1) {
                                val mutable = waypoints.toMutableList()
                                mutable[index] = mutable[index + 1].also {
                                    mutable[index + 1] = mutable[index]
                                }
                                waypoints = mutable
                            }
                        }
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color(0xFFBDBDBD)
                    )
                    Text(
                        text = "No stops added yet",
                        fontSize = 16.sp,
                        color = Color(0xFF757575)
                    )
                    Text(
                        text = "Add stops between origin and destination",
                        fontSize = 14.sp,
                        color = Color(0xFF9E9E9E)
                    )
                }
            }
        }
        
        // Bottom Actions
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E88E5)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Stop")
                }
                
                if (origin.isNotEmpty() && destination.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = {
                            // Create waypoints list
                            val allWaypoints = mutableListOf<Waypoint>()
                            
                            // Origin
                            allWaypoints.add(
                                Waypoint(
                                    position = Position(0f, 0f), // Mock position
                                    name = origin,
                                    address = origin,
                                    order = 0,
                                    isOrigin = true
                                )
                            )
                            
                            // Intermediate stops
                            allWaypoints.addAll(waypoints.mapIndexed { index, wp ->
                                wp.copy(order = index + 1)
                            })
                            
                            // Destination
                            allWaypoints.add(
                                Waypoint(
                                    position = Position(0f, 0f), // Mock position
                                    name = destination,
                                    address = destination,
                                    order = waypoints.size + 1,
                                    isDestination = true
                                )
                            )
                            
                            onStartNavigation(allWaypoints)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Place, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Navigation")
                    }
                }
            }
        }
    }
    
    // Add Stop Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Stop") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newStopName,
                        onValueChange = { newStopName = it },
                        label = { Text("Location name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Stop duration: $stopDuration minutes", fontSize = 14.sp)
                    
                    Slider(
                        value = stopDuration.toFloat(),
                        onValueChange = { stopDuration = it.toInt() },
                        valueRange = 0f..60f,
                        steps = 11
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newStopName.isNotEmpty()) {
                            waypoints = waypoints + Waypoint(
                                position = Position(0f, 0f), // Mock position
                                name = newStopName,
                                address = newStopName,
                                order = waypoints.size,
                                stopDuration = stopDuration
                            )
                            newStopName = ""
                            stopDuration = 5
                        }
                        showAddDialog = false
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun WaypointCard(
    waypoint: Waypoint,
    index: Int,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Order badge
            Surface(
                shape = CircleShape,
                color = Color(0xFF1E88E5),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "${index + 1}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Waypoint info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = waypoint.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF212121)
                )
                if (waypoint.stopDuration > 0) {
                    Text(
                        text = "Stop for ${waypoint.stopDuration} min",
                        fontSize = 12.sp,
                        color = Color(0xFF757575)
                    )
                }
            }
            
            // Controls
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onMoveUp, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = "Move up",
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFF757575)
                    )
                }
                
                IconButton(onClick = onMoveDown, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Move down",
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFF757575)
                    )
                }
                
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove",
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFFE53935)
                    )
                }
            }
        }
    }
}

fun optimizeWaypoints(waypoints: List<Waypoint>): List<Waypoint> {
    if (waypoints.size <= 1) return waypoints
    
    // Simple nearest neighbor algorithm
    val remaining = waypoints.toMutableList()
    val optimized = mutableListOf<Waypoint>()
    
    // Start with first waypoint
    var current = remaining.removeAt(0)
    optimized.add(current.copy(order = 0))
    
    // Find nearest waypoint iteratively
    while (remaining.isNotEmpty()) {
        val nearest = remaining.minByOrNull { wp ->
            calculateDistance(
                current.position.x,
                current.position.y,
                wp.position.x,
                wp.position.y
            )
        }
        
        if (nearest != null) {
            remaining.remove(nearest)
            optimized.add(nearest.copy(order = optimized.size))
            current = nearest
        }
    }
    
    return optimized
}

fun calculateDistance(x1: Float, y1: Float, x2: Float, y2: Float): Double {
    val dx = (x2 - x1).toDouble()
    val dy = (y2 - y1).toDouble()
    return kotlin.math.sqrt(dx * dx + dy * dy)
}
