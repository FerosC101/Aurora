package com.nextcs.aurora.ui.screens

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
import com.nextcs.aurora.audio.VoiceNavigationService
import com.nextcs.aurora.database.AppDatabase
import com.nextcs.aurora.database.WaypointData
import com.nextcs.aurora.location.LocationService
import com.nextcs.aurora.models.LaneConfigurations
import com.nextcs.aurora.models.LaneGuidance
import com.nextcs.aurora.navigation.DirectionsService
import com.nextcs.aurora.navigation.NavigationStep
import com.nextcs.aurora.navigation.RouteInfo
import com.nextcs.aurora.navigation.HazardDetectionService
import com.nextcs.aurora.navigation.TripHistoryService
import com.nextcs.aurora.repository.SavedRoutesRepository
import com.nextcs.aurora.sensors.SpeedMonitor
import com.nextcs.aurora.ui.components.CompactLaneGuidance
import com.nextcs.aurora.ui.components.LaneGuidanceDisplay
import com.nextcs.aurora.ui.components.SpeedDisplay

@Composable
fun RealNavigationScreen(
    origin: String,
    destination: String,
    originLocation: LatLng? = null,
    destinationLocation: LatLng? = null,
    waypoints: List<LatLng> = emptyList(),
    selectedRoute: com.nextcs.aurora.navigation.RouteAlternative? = null,
    onBack: () -> Unit,
    onViewAlternativeRoutes: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val locationService = remember { LocationService(context) }
    val directionsService = remember { DirectionsService(context) }
    val hazardService = remember { HazardDetectionService() }
    val tripHistoryService = remember { TripHistoryService(context) }
    val voiceService = remember { VoiceNavigationService(context) }
    val speedMonitor = remember { SpeedMonitor(context) }
    // TODO: Re-enable when KSP is working
    // Repository disabled due to missing Room implementation
    val scope = rememberCoroutineScope()
    
    val speedData by speedMonitor.speedData.collectAsState()
    val isVoiceEnabled by voiceService.isEnabled.collectAsState()
    val isVoiceReady by voiceService.isReady.collectAsState()
    
    // Get real current location or use origin location or default
    val initialLocation = locationService.getLastKnownLocation() ?: originLocation ?: LatLng(14.5995, 120.9842)
    var currentLocation by remember { mutableStateOf(initialLocation) }
    var routeInfo by remember { mutableStateOf<RouteInfo?>(null) }
    var detectedHazards by remember { mutableStateOf(emptyList<com.nextcs.aurora.navigation.DetectedHazard>()) }
    var safetyScore by remember { mutableStateOf(100) }
    var currentStepIndex by remember { mutableStateOf(0) }
    var currentInstruction by remember { mutableStateOf("Calculating route...") }
    var distanceToTurn by remember { mutableStateOf(0) }
    var eta by remember { mutableStateOf("--") }
    var remainingDistance by remember { mutableStateOf("--") }
    var showLaneGuidance by remember { mutableStateOf(false) }
    var currentLaneGuidance by remember { mutableStateOf<LaneGuidance?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var isLoadingRoute by remember { mutableStateOf(false) }
    var showHazardAlert by remember { mutableStateOf(false) }
    var nextHazard by remember { mutableStateOf<com.nextcs.aurora.navigation.DetectedHazard?>(null) }
    
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
                            // TODO: Re-enable when KSP is working
                            // scope.launch {
                            //     repository.saveRoute(
                            //         name = routeName,
                            //         origin = origin,
                            //         destination = destination,
                            //         distance = remainingDistance.replace(" km", "").toDoubleOrNull() ?: 0.0,
                            //         estimatedTime = eta.replace(" min", "").toIntOrNull() ?: 0
                            //     )
                            // }
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
        position = CameraPosition.fromLatLngZoom(currentLocation, 17f)
    }
    
    // Track real GPS location updates
    LaunchedEffect(Unit) {
        locationService.getCurrentLocationFlow().collect { location ->
            currentLocation = location
            // Update camera to follow current location
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(location, 17f),
                durationMs = 1000
            )
        }
    }
    
    // Fetch route from Directions API
    LaunchedEffect(originLocation, destinationLocation, selectedRoute) {
        if (originLocation != null && destinationLocation != null) {
            isLoadingRoute = true
            
            // Use selected route if available, otherwise fetch from API
            val route = if (selectedRoute != null) {
                selectedRoute.routeInfo
            } else {
                val result = directionsService.getDirections(originLocation, destinationLocation, waypoints)
                result.getOrNull()
            }
            
            route?.let {
                routeInfo = it
                eta = directionsService.formatDuration(it.duration)
                remainingDistance = directionsService.formatDistance(it.distance)
                
                // Use detected hazards from selected route if available
                if (selectedRoute != null) {
                    detectedHazards = selectedRoute.hazards
                    safetyScore = selectedRoute.safetyScore
                } else {
                    // Detect hazards on route
                    detectedHazards = hazardService.detectHazards(it.steps)
                    safetyScore = hazardService.calculateSafetyScore(detectedHazards)
                }
                
                if (it.steps.isNotEmpty()) {
                    currentStepIndex = 0
                    currentInstruction = it.steps[0].instruction
                    distanceToTurn = it.steps[0].distance
                    
                    // Find first hazard on route
                    nextHazard = detectedHazards.minByOrNull { hazard -> hazard.distanceFromStart }
                    
                    val hazardInfo = if (detectedHazards.isNotEmpty()) {
                        " Found ${detectedHazards.size} hazards on route."
                    } else {
                        ""
                    }
                    val routeTypeInfo = if (selectedRoute != null) " Using ${selectedRoute.name}." else ""
                    voiceService.announce("Navigation started to $destination. ${it.steps[0].instruction}$hazardInfo$routeTypeInfo")
                }
            } ?: run {
                currentInstruction = "Unable to calculate route"
                voiceService.announce(currentInstruction)
            }
            isLoadingRoute = false
        }
        
        speedMonitor.setSpeedLimit(60)
        speedMonitor.startMonitoring()
    }
    
    // Update navigation based on real GPS location
    LaunchedEffect(currentLocation, routeInfo) {
        routeInfo?.let { route ->
            while (currentStepIndex < route.steps.size) {
                delay(1000) // Check every second
                
                val currentStep = route.steps[currentStepIndex]
                
                // Calculate distance to next turn
                val distanceToNextStep = directionsService.calculateDistanceBetween(
                    currentLocation,
                    currentStep.endLocation
                ).toInt()
                
                distanceToTurn = distanceToNextStep
                
                // Show lane guidance when approaching turn (within 300m)
                if (distanceToNextStep < 300 && distanceToNextStep > 50) {
                    showLaneGuidance = true
                    // Use maneuver to determine lane guidance
                    currentLaneGuidance = when (currentStep.maneuver) {
                        "turn-left" -> LaneConfigurations.leftTurnFromLeft()
                        "turn-right" -> LaneConfigurations.rightTurnFromRight()
                        "turn-slight-left", "turn-slight-right" -> LaneConfigurations.leftOrStraight()
                        else -> LaneConfigurations.complexIntersection()
                    }
                    currentLaneGuidance = currentLaneGuidance?.copy(distance = distanceToNextStep)
                } else if (distanceToNextStep <= 50) {
                    showLaneGuidance = false
                    currentLaneGuidance = null
                }
                
                // Move to next step when close enough (within 50m)
                if (distanceToNextStep < 50 && currentStepIndex < route.steps.size - 1) {
                    currentStepIndex++
                    val nextStep = route.steps[currentStepIndex]
                    currentInstruction = nextStep.instruction
                    voiceService.announce(currentInstruction)
                    
                    // Calculate remaining distance and time
                    val remainingSteps = route.steps.drop(currentStepIndex)
                    val totalRemainingDistance = remainingSteps.sumOf { it.distance }
                    val totalRemainingDuration = remainingSteps.sumOf { it.duration }
                    remainingDistance = directionsService.formatDistance(totalRemainingDistance)
                    eta = directionsService.formatDuration(totalRemainingDuration)
                } else if (distanceToNextStep < 50 && currentStepIndex == route.steps.size - 1) {
                    // Arrived at destination - save trip
                    currentInstruction = "You have arrived at $destination"
                    voiceService.announce(currentInstruction)
                    
                    // Save completed trip
                    scope.launch {
                        try {
                            val totalDistance = route.steps.sumOf { it.distance }
                            val totalDuration = route.steps.sumOf { it.duration }
                            
                            tripHistoryService.saveTrip(
                                origin = origin,
                                destination = destination,
                                routeInfo = com.nextcs.aurora.navigation.RouteInfo(
                                    polyline = route.polyline,
                                    steps = route.steps,
                                    distance = totalDistance,
                                    duration = totalDuration,
                                    overview = "$origin to $destination"
                                ),
                                hazards = detectedHazards,
                                safetyScore = 85, // Calculate based on hazards and speed violations
                                routeType = "Regular"
                            )
                            android.util.Log.d("RealNavigation", "Trip saved successfully")
                        } catch (e: Exception) {
                            android.util.Log.e("RealNavigation", "Failed to save trip", e)
                        }
                    }
                    break
                }
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
        // Google Map with real location tracking
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = locationService.hasLocationPermission(),
                mapType = MapType.NORMAL
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = true,
                compassEnabled = true,
                mapToolbarEnabled = false
            )
        ) {
            // Draw route polyline if available
            routeInfo?.let { route ->
                // Decode and draw polyline
                val polylinePoints = decodePolyline(route.polyline)
                if (polylinePoints.isNotEmpty()) {
                    Polyline(
                        points = polylinePoints,
                        color = Color(0xFF1E88E5),
                        width = 10f
                    )
                }
            }
            
            // Origin marker if coordinates available
            originLocation?.let { origLoc ->
                Marker(
                    state = MarkerState(position = origLoc),
                    title = origin,
                    snippet = "Starting point",
                    icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(
                        com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_GREEN
                    )
                )
            }
            
            // Destination marker if coordinates available
            destinationLocation?.let { destLoc ->
                Marker(
                    state = MarkerState(position = destLoc),
                    title = destination,
                    snippet = "Destination",
                    icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(
                        com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED
                    )
                )
            }
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
        
        // Speed Display Overlay (Top-Left, below back button)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .padding(16.dp)
                .padding(top = 140.dp) // Below top bar and back button
        ) {
            SpeedDisplay(
                currentSpeed = speedData.currentSpeed,
                speedLimit = speedData.speedLimit,
                isExceeding = speedData.isExceeding,
                modifier = Modifier.align(Alignment.TopStart)
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

/**
 * Decode Google Maps encoded polyline string into list of LatLng points
 */
private fun decodePolyline(encoded: String): List<LatLng> {
    val poly = ArrayList<LatLng>()
    var index = 0
    val len = encoded.length
    var lat = 0
    var lng = 0

    while (index < len) {
        var b: Int
        var shift = 0
        var result = 0
        do {
            b = encoded[index++].code - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lat += dlat

        shift = 0
        result = 0
        do {
            b = encoded[index++].code - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lng += dlng

        val p = LatLng(
            lat.toDouble() / 1E5,
            lng.toDouble() / 1E5
        )
        poly.add(p)
    }

    return poly
}
