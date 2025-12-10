package org.aurora.android.ui.screens

import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.aurora.android.audio.VoiceNavigationService
import org.aurora.android.database.AppDatabase
import org.aurora.android.database.WaypointData
import org.aurora.android.models.LaneConfigurations
import org.aurora.android.models.LaneGuidance
import org.aurora.android.repository.SavedRoutesRepository
import org.aurora.android.sensors.SpeedMonitor
import org.aurora.android.ui.components.CompactLaneGuidance
import org.aurora.android.ui.components.LaneGuidanceDisplay
import org.aurora.android.ui.components.SpeedDisplay

@Composable
fun RealNavigationScreen(
    origin: String,
    destination: String,
    onBack: () -> Unit,
    onViewAlternativeRoutes: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val voiceService = remember { VoiceNavigationService(context) }
    val speedMonitor = remember { SpeedMonitor(context) }
    val repository = remember {
        SavedRoutesRepository(AppDatabase.getInstance(context).savedRouteDao())
    }
    val scope = rememberCoroutineScope()
    
    val speedData by speedMonitor.speedData.collectAsState()
    val isVoiceEnabled by voiceService.isEnabled.collectAsState()
    val isVoiceReady by voiceService.isReady.collectAsState()
    
    // Current location state (demo: Manila area)
    var currentLocation by remember { mutableStateOf(LatLng(14.5995, 120.9842)) }
    var currentInstruction by remember { mutableStateOf("Head north on EDSA") }
    var distanceToTurn by remember { mutableStateOf(450) }
    var eta by remember { mutableStateOf("12 min") }
    var remainingDistance by remember { mutableStateOf("5.2 km") }
    var showLaneGuidance by remember { mutableStateOf(false) }
    var currentLaneGuidance by remember { mutableStateOf<LaneGuidance?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }
    
    // Save route dialog
    if (showSaveDialog) {
        var routeName by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Route") },
            text = {
                Column {
                    Text("Save this route for quick access later.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = routeName,
                        onValueChange = { routeName = it },
                        label = { Text("Route name") },
                        placeholder = { Text("e.g., Daily Commute") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (routeName.isNotBlank()) {
                            scope.launch {
                                repository.saveRoute(
                                    name = routeName,
                                    origin = origin,
                                    destination = destination,
                                    distance = remainingDistance.replace(" km", "").toDoubleOrNull() ?: 0.0,
                                    estimatedTime = eta.replace(" min", "").toIntOrNull() ?: 0
                                )
                            }
                            showSaveDialog = false
                        }
                    },
                    enabled = routeName.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Camera position state
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(currentLocation, 15f)
    }
    
    // Demo: Simulate location updates with lane guidance
    LaunchedEffect(Unit) {
        speedMonitor.setSpeedLimit(60)
        speedMonitor.startMonitoring()
        
        // Announce start
        delay(1000)
        voiceService.announce("Navigation started. $currentInstruction")
        
        // Simulate moving north (demo)
        var latOffset = 0.0
        var turnCounter = 0
        while (true) {
            delay(2000)
            latOffset += 0.0002 // Move north slightly
            currentLocation = LatLng(14.5995 + latOffset, 120.9842)
            
            // Update camera to follow
            cameraPositionState.move(
                CameraUpdateFactory.newLatLng(currentLocation)
            )
            
            // Simulate distance to turn
            distanceToTurn = (distanceToTurn - 25).coerceAtLeast(0)
            
            // Show lane guidance when approaching turn (within 300m)
            if (distanceToTurn < 300 && distanceToTurn > 0) {
                showLaneGuidance = true
                // Cycle through different lane configurations for demo
                currentLaneGuidance = when (turnCounter % 4) {
                    0 -> LaneConfigurations.leftTurnFromLeft()
                    1 -> LaneConfigurations.rightTurnFromRight()
                    2 -> LaneConfigurations.complexIntersection()
                    else -> LaneConfigurations.leftOrStraight()
                }
                currentLaneGuidance = currentLaneGuidance?.copy(distance = distanceToTurn)
            } else if (distanceToTurn == 0) {
                showLaneGuidance = false
                currentLaneGuidance = null
            }
            
            // Simulate turn instruction every 10 seconds
            if (distanceToTurn == 0) {
                val instructions = listOf(
                    "Turn right onto Quezon Avenue",
                    "Turn left onto España Boulevard",
                    "Continue straight on Roxas Boulevard",
                    "Take the exit to Makati Avenue"
                )
                turnCounter++
                val index = turnCounter % instructions.size
                currentInstruction = instructions[index]
                distanceToTurn = 450 // Reset distance
                voiceService.announce(currentInstruction)
            }
        }
    }
    
    // Monitor speed violations
    LaunchedEffect(speedData.isExceeding) {
        if (speedData.isExceeding) {
            speedData.speedLimit?.let { limit ->
                voiceService.announceSpeedWarning(speedData.currentSpeed.toInt(), limit)
            }
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            speedMonitor.stopMonitoring()
            voiceService.shutdown()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // Google Map
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = false,
                mapType = MapType.NORMAL
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                compassEnabled = true,
                mapToolbarEnabled = false
            )
        ) {
            // Current location marker
            Marker(
                state = MarkerState(position = currentLocation),
                title = "You are here",
                snippet = "Current speed: ${speedData.currentSpeed.toInt()} km/h"
            )
        }
        
        // Top Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            color = Color.White,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = eta,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E88E5)
                    )
                    Text(
                        text = remainingDistance,
                        fontSize = 14.sp,
                        color = Color(0xFF757575)
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Alternative routes button
                    IconButton(onClick = onViewAlternativeRoutes) {
                        Icon(
                            Icons.Default.List,
                            contentDescription = "View alternative routes",
                            tint = Color(0xFF1E88E5)
                        )
                    }
                    
                    // Save route button
                    IconButton(onClick = { showSaveDialog = true }) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = "Save route",
                            tint = Color(0xFFE91E63)
                        )
                    }
                    
                    // Voice toggle
                    IconButton(
                        onClick = { voiceService.setEnabled(!isVoiceEnabled) }
                    ) {
                        Icon(
                            if (isVoiceEnabled && isVoiceReady) Icons.Default.Notifications 
                            else Icons.Default.Clear,
                            contentDescription = "Toggle voice",
                            tint = if (isVoiceEnabled && isVoiceReady) Color(0xFF4CAF50) 
                                   else Color(0xFF9E9E9E)
                        )
                    }
                }
            }
        }
        
        // Lane Guidance (Top-Center, below top bar)
        if (showLaneGuidance && currentLaneGuidance != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 80.dp)
                    .align(Alignment.TopCenter)
            ) {
                LaneGuidanceDisplay(
                    laneGuidance = currentLaneGuidance!!,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .align(Alignment.Center)
                )
            }
        }
        
        // Speed Display Overlay (Top-Right)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .padding(top = 72.dp) // Below top bar
        ) {
            SpeedDisplay(
                currentSpeed = speedData.currentSpeed,
                speedLimit = speedData.speedLimit,
                isExceeding = speedData.isExceeding,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
        
        // Bottom Instruction Panel
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Direction Icon
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E88E5),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    
                    // Instruction Text
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentInstruction,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF212121)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "in $distanceToTurn m",
                            fontSize = 14.sp,
                            color = Color(0xFF757575)
                        )
                    }
                }
                
                // Speed Warning Banner
                if (speedData.isExceeding) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Exceeding speed limit!",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFD32F2F)
                            )
                        }
                    }
                }
                
                // Route Info
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "From: $origin",
                            fontSize = 12.sp,
                            color = Color(0xFF757575)
                        )
                        Text(
                            text = "To: $destination",
                            fontSize = 12.sp,
                            color = Color(0xFF757575)
                        )
                    }
                    
                    TextButton(onClick = { /* Show route options */ }) {
                        Text("Options", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
