package com.nextcs.aurora.ui.screens

import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
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
import com.nextcs.aurora.navigation.SavedRoutesService
import com.nextcs.aurora.weather.WeatherService
import com.nextcs.aurora.weather.WeatherData
import com.nextcs.aurora.sensors.SpeedMonitor
import com.nextcs.aurora.social.FriendLocationSharingService
import com.nextcs.aurora.social.FriendLocation
import com.nextcs.aurora.services.ParkingFinderService
import com.nextcs.aurora.services.ParkingSpot
import com.nextcs.aurora.navigation.TrafficAwareNavigationService
import com.nextcs.aurora.navigation.LiveTrafficLevel
import com.nextcs.aurora.analytics.DrivingBehaviorAnalyzer
import com.nextcs.aurora.analytics.DrivingBehavior
import com.nextcs.aurora.alerts.RouteChangeAlertService
import com.nextcs.aurora.ui.components.CompactLaneGuidance
import com.nextcs.aurora.ui.components.LaneGuidanceDisplay
import com.nextcs.aurora.ui.components.SpeedDisplay

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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
    val savedRoutesService = remember { SavedRoutesService(context) }
    val weatherService = remember { WeatherService(context) }
    val friendService = remember { FriendLocationSharingService(context) }
    val parkingService = remember { ParkingFinderService(context) }
    val trafficService = remember { TrafficAwareNavigationService(context) }
    val behaviorAnalyzer = remember { DrivingBehaviorAnalyzer() }
    val routeAlertService = remember { RouteChangeAlertService(context) }
    val scope = rememberCoroutineScope()
    
    val speedData by speedMonitor.speedData.collectAsState()
    val isVoiceEnabled by voiceService.isEnabled.collectAsState()
    val isVoiceReady by voiceService.isReady.collectAsState()
    
    var currentWeather by remember { mutableStateOf<WeatherData?>(null) }
    var isSharingLocation by remember { mutableStateOf(false) }
    var friendLocations by remember { mutableStateOf<List<FriendLocation>>(emptyList()) }
    var showParkingSheet by remember { mutableStateOf(false) }
    var parkingSpots by remember { mutableStateOf<List<ParkingSpot>>(emptyList()) }
    var showTrafficLayer by remember { mutableStateOf(false) }
    var currentDrivingBehavior by remember { mutableStateOf<DrivingBehavior?>(null) }
    var isOffRoute by remember { mutableStateOf(false) }
    
    // Get real current location or use origin location or default
    val initialLocation = locationService.getLastKnownLocation() ?: originLocation ?: LatLng(14.5995, 120.9842)
    var currentLocation by remember { mutableStateOf(initialLocation) }
    var routeInfo by remember { mutableStateOf<RouteInfo?>(null) }
    var detectedHazards by remember { mutableStateOf(emptyList<com.nextcs.aurora.navigation.DetectedHazard>()) }
    var safetyScore by remember { mutableStateOf(100) }
    var currentStepIndex by remember { mutableStateOf(0) }
    var currentInstruction by remember { mutableStateOf("Calculating route...") }
    var nextInstruction by remember { mutableStateOf<String?>(null) }
    var distanceToTurn by remember { mutableStateOf(0) }
    var eta by remember { mutableStateOf("--") }
    var remainingDistance by remember { mutableStateOf("--") }
    var showLaneGuidance by remember { mutableStateOf(false) }
    var currentLaneGuidance by remember { mutableStateOf<LaneGuidance?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var isLoadingRoute by remember { mutableStateOf(false) }
    var showHazardAlert by remember { mutableStateOf(false) }
    var nextHazard by remember { mutableStateOf<com.nextcs.aurora.navigation.DetectedHazard?>(null) }
    var isNavigating by remember { mutableStateOf(false) }
    
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
                        if (routeName.isNotBlank() && routeInfo != null) {
                            scope.launch {
                                // Calculate or parse distance and time from routeInfo
                                val distanceKm = routeInfo!!.distance / 1000.0
                                val timeMinutes = routeInfo!!.duration / 60
                                
                                savedRoutesService.saveRoute(
                                    name = routeName,
                                    origin = origin,
                                    destination = destination,
                                    distance = distanceKm,
                                    estimatedTime = timeMinutes,
                                    waypoints = waypoints
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
    
    // Parking Bottom Sheet
    if (showParkingSheet && parkingSpots.isNotEmpty()) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showParkingSheet = false },
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "\ud83c\udd7f\ufe0f Nearby Parking",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { showParkingSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "${parkingSpots.size} parking spots found near destination",
                    fontSize = 14.sp,
                    color = Color(0xFF757575)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                parkingSpots.take(5).forEach { spot ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                // Navigate to parking spot
                            },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    spot.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "${String.format("%.0f", spot.distanceFromDestination)} m away",
                                    fontSize = 13.sp,
                                    color = Color(0xFF757575)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = Color(0xFFFFC107)
                                    )
                                    Text(
                                        " ${spot.rating}",
                                        fontSize = 12.sp,
                                        color = Color(0xFF757575)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        if (spot.isOpenNow) "Open" else "Closed",
                                        fontSize = 12.sp,
                                        color = if (spot.isOpenNow) Color(0xFF4CAF50) else Color(0xFFE53935),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            
                            Surface(
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = Color(0xFF2196F3)
                            ) {
                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.padding(8.dp).size(20.dp),
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
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
            
            // Analyze driving behavior if navigating
            if (isNavigating) {
                val loc = Location("gps").apply {
                    latitude = location.latitude
                    longitude = location.longitude
                    speed = speedData.currentSpeed / 3.6f // Convert km/h to m/s
                }
                behaviorAnalyzer.analyzeBehavior(loc, speedData.currentSpeed, speedData.speedLimit)
                currentDrivingBehavior = behaviorAnalyzer.getBehavior()
                
                // Check if off-route
                routeInfo?.let { route ->
                    if (trafficService.checkOffRoute(location, route)) {
                        isOffRoute = true
                        // Auto-reroute
                        scope.launch {
                            destinationLocation?.let { dest ->
                                trafficService.autoReroute(location, dest).onSuccess { newRoute ->
                                    routeInfo = newRoute
                                    isOffRoute = false
                                }
                            }
                        }
                    } else {
                        isOffRoute = false
                    }
                }
            }
            
            // Update shared location if sharing is enabled
            if (isSharingLocation && isNavigating) {
                scope.launch {
                    friendService.updateLocation(
                        location = location,
                        eta = eta
                    )
                }
            }
            
            // Fetch weather for current location every 5 minutes
            if (currentWeather == null || System.currentTimeMillis() % 300000 < 5000) {
                scope.launch {
                    val weatherResult = weatherService.getWeather(location)
                    weatherResult.onSuccess { weather ->
                        currentWeather = weather
                    }
                }
            }
        }
    }
    
    // Observe friends' locations
    LaunchedEffect(Unit) {
        friendService.observeFriendsLocations().collect { locations ->
            friendLocations = locations.filter { it.isSharing }
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
                    
                    // Set next instruction preview
                    if (it.steps.size > 1) {
                        nextInstruction = it.steps[1].instruction
                    }
                    
                    // Find first hazard on route
                    nextHazard = detectedHazards.minByOrNull { hazard -> hazard.distanceFromStart }
                    
                    // Don't auto-announce, wait for Start Navigation button
                }
            } ?: run {
                currentInstruction = "Unable to calculate route"
            }
            isLoadingRoute = false
        }
        
        speedMonitor.setSpeedLimit(60)
        speedMonitor.startMonitoring()
    }
    
    // Update navigation based on real GPS location
    LaunchedEffect(currentLocation, routeInfo, isNavigating) {
        if (!isNavigating) return@LaunchedEffect
        
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
                    
                    // Update next instruction preview
                    if (currentStepIndex < route.steps.size - 1) {
                        nextInstruction = route.steps[currentStepIndex + 1].instruction
                    } else {
                        nextInstruction = null
                    }
                    
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
                            val behavior = currentDrivingBehavior ?: behaviorAnalyzer.getBehavior()
                            
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
                                safetyScore = behavior.smoothDrivingScore,
                                routeType = "Regular",
                                harshBrakingCount = behavior.harshBrakingCount,
                                rapidAccelerationCount = behavior.rapidAccelerationCount,
                                speedingIncidents = behavior.speedingIncidents
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
            
            // Friend location markers
            friendLocations.forEach { friendLocation ->
                Marker(
                    state = MarkerState(position = friendLocation.toLatLng()),
                    title = friendLocation.displayName,
                    snippet = if (friendLocation.destination != null && friendLocation.eta != null) {
                        "Going to ${friendLocation.destination} • ETA: ${friendLocation.eta}"
                    } else {
                        "Sharing location"
                    },
                    icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(
                        com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_AZURE
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
                    // Weather display
                    currentWeather?.let { weather ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Text(
                                text = weatherService.getWeatherEmoji(weather),
                                fontSize = 16.sp
                            )
                            Text(
                                text = "${String.format("%.0f", weather.temperature)}°C",
                                fontSize = 13.sp,
                                color = if (weather.isDangerous) Color(0xFFD32F2F) else Color(0xFF757575)
                            )
                        }
                    }
                    
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
                    
                    // Traffic toggle
                    IconButton(onClick = { showTrafficLayer = !showTrafficLayer }) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = "Toggle traffic",
                            tint = if (showTrafficLayer) Color(0xFFFF9800) else Color(0xFF9E9E9E)
                        )
                    }
                    
                    // Parking finder (show when near destination)
                    if (isNavigating && remainingDistance.contains("m") && 
                        remainingDistance.replace(" m", "").toIntOrNull()?.let { it < 500 } == true) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    destinationLocation?.let { dest ->
                                        parkingService.findParkingNearDestination(dest).onSuccess { spots ->
                                            parkingSpots = spots
                                            showParkingSheet = true
                                        }
                                    }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Place,
                                contentDescription = "Find parking",
                                tint = Color(0xFF2196F3)
                            )
                        }
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
                    
                    // Finish Navigation button
                    IconButton(
                        onClick = {
                            scope.launch {
                                routeInfo?.let { route ->
                                    try {
                                        val totalDistance = route.steps.sumOf { it.distance }
                                        val totalDuration = route.steps.sumOf { it.duration }
                                        val behavior = currentDrivingBehavior ?: behaviorAnalyzer.getBehavior()
                                        
                                        tripHistoryService.saveTrip(
                                            origin = origin,
                                            destination = destination,
                                            routeInfo = RouteInfo(
                                                polyline = route.polyline,
                                                steps = route.steps,
                                                distance = totalDistance,
                                                duration = totalDuration,
                                                overview = "$origin to $destination"
                                            ),
                                            hazards = detectedHazards,
                                            safetyScore = behavior.smoothDrivingScore,
                                            routeType = selectedRoute?.name ?: "Regular",
                                            harshBrakingCount = behavior.harshBrakingCount,
                                            rapidAccelerationCount = behavior.rapidAccelerationCount,
                                            speedingIncidents = behavior.speedingIncidents
                                        )
                                        android.util.Log.d("RealNavigation", "Trip manually finished and saved")
                                    } catch (e: Exception) {
                                        android.util.Log.e("RealNavigation", "Failed to save trip", e)
                                    }
                                }
                                onBack()
                            }
                        },
                        enabled = routeInfo != null
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Finish navigation",
                            tint = if (routeInfo != null) Color(0xFF4CAF50) else Color(0xFF9E9E9E)
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
            
            // Off-Route Banner
            if (isOffRoute) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp),
                    color = Color(0xFFFF9800),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Off Route - Recalculating...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
            
            // Weather alerts
            currentWeather?.let { weather ->
                val alerts = weatherService.getWeatherAlerts(weather)
                if (alerts.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 90.dp)
                            .fillMaxWidth(0.95f)
                    ) {
                        alerts.take(2).forEach { alert ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = when (alert.severity) {
                                    com.nextcs.aurora.weather.AlertSeverity.DANGER -> Color(0xFFFFEBEE)
                                    com.nextcs.aurora.weather.AlertSeverity.WARNING -> Color(0xFFFFF3E0)
                                    else -> Color(0xFFE3F2FD)
                                },
                                shadowElevation = 2.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        when (alert.severity) {
                                            com.nextcs.aurora.weather.AlertSeverity.DANGER -> Icons.Default.Warning
                                            else -> Icons.Default.Info
                                        },
                                        contentDescription = null,
                                        tint = when (alert.severity) {
                                            com.nextcs.aurora.weather.AlertSeverity.DANGER -> Color(0xFFD32F2F)
                                            com.nextcs.aurora.weather.AlertSeverity.WARNING -> Color(0xFFF57C00)
                                            else -> Color(0xFF1976D2)
                                        },
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = alert.message,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF212121)
                                        )
                                        Text(
                                            text = alert.recommendation,
                                            fontSize = 11.sp,
                                            color = Color(0xFF757575)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        
        // Bottom Navigation Panel - 3 States
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            when {
                // State 1: Loading - Calculating route
                routeInfo == null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF1E88E5),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Calculating route...",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF757575)
                        )
                    }
                }
                
                // State 2: Route Preview - Show Start Navigation button
                !isNavigating -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Route Summary
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = destination,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF212121),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "${routeInfo?.distance?.div(1000)?.toInt() ?: 0} km",
                                        fontSize = 14.sp,
                                        color = Color(0xFF757575)
                                    )
                                    Text(
                                        text = "•",
                                        fontSize = 14.sp,
                                        color = Color(0xFF757575)
                                    )
                                    Text(
                                        text = "${routeInfo?.duration?.div(60)?.toInt() ?: 0} min",
                                        fontSize = 14.sp,
                                        color = Color(0xFF757575)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Location Sharing Toggle Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Share Location Toggle
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        if (isSharingLocation) {
                                            friendService.stopSharingLocation()
                                            isSharingLocation = false
                                        } else {
                                            val result = friendService.startSharingLocation(
                                                location = currentLocation,
                                                tripId = null,
                                                destination = destination,
                                                eta = eta
                                            )
                                            result.onSuccess {
                                                isSharingLocation = true
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSharingLocation) Color(0xFFE3F2FD) else Color.Transparent
                                )
                            ) {
                                Icon(
                                    if (isSharingLocation) Icons.Default.Share else Icons.Default.Share,
                                    contentDescription = null,
                                    tint = if (isSharingLocation) Color(0xFF1976D2) else Color(0xFF757575),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isSharingLocation) "Sharing" else "Share",
                                    fontSize = 14.sp,
                                    color = if (isSharingLocation) Color(0xFF1976D2) else Color(0xFF757575)
                                )
                            }
                            
                            // View Friends Button
                            OutlinedButton(
                                onClick = { /* Navigate to friends screen */ },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF757575),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Friends (${friendLocations.size})",
                                    fontSize = 14.sp,
                                    color = Color(0xFF757575)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Start Navigation Button
                        Button(
                            onClick = {
                                isNavigating = true
                                routeInfo?.let { route ->
                                    if (route.steps.isNotEmpty()) {
                                        voiceService.announce("Starting navigation to $destination")
                                        speedMonitor.startMonitoring()
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E88E5)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Start Navigation",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
                
                // State 3: Active Navigation - Show turn-by-turn
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Current Instruction
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
                        
                        // Next Instruction Preview
                        if (nextInstruction != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFFF5F5F5),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        tint = Color(0xFF757575),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Then: $nextInstruction",
                                        fontSize = 14.sp,
                                        color = Color(0xFF757575)
                                    )
                                }
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
                        
                        // ETA and Distance
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "ETA: ${routeInfo?.duration?.div(60)?.toInt() ?: 0} min",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF212121)
                                )
                                Text(
                                    text = "${routeInfo?.distance?.div(1000)?.toInt() ?: 0} km remaining",
                                    fontSize = 12.sp,
                                    color = Color(0xFF757575)
                                )
                            }
                            
                            TextButton(
                                onClick = {
                                    isNavigating = false
                                    speedMonitor.stopMonitoring()
                                }
                            ) {
                                Text("End", fontSize = 14.sp, color = Color(0xFFD32F2F))
                            }
                        }
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
