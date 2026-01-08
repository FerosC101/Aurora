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
            // Minimalist Top Bar - Clean and Simple
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF212121)
                        )
                    }
                }
                
                // ETA Info - Center
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        currentWeather?.let { weather ->
                            Text(
                                text = weatherService.getWeatherEmoji(weather),
                                fontSize = 14.sp
                            )
                        }
                        Text(
                            text = eta,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2)
                        )
                        Text(
                            text = "·",
                            fontSize = 14.sp,
                            color = Color(0xFFBDBDBD)
                        )
                        Text(
                            text = remainingDistance,
                            fontSize = 14.sp,
                            color = Color(0xFF757575)
                        )
                    }
                }
                
                // Menu button - Shows options
                var showMenu by remember { mutableStateOf(false) }
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Box {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = Color(0xFF212121)
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Alternative Routes") },
                                onClick = {
                                    showMenu = false
                                    onViewAlternativeRoutes()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.List, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Save Route") },
                                onClick = {
                                    showMenu = false
                                    showSaveDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Favorite, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { 
                                    Text(if (showTrafficLayer) "Hide Traffic" else "Show Traffic") 
                                },
                                onClick = {
                                    showTrafficLayer = !showTrafficLayer
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (showTrafficLayer) Color(0xFFFF9800) else Color.Unspecified
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { 
                                    Text(if (isVoiceEnabled) "Voice: ON" else "Voice: OFF") 
                                },
                                onClick = {
                                    voiceService.setEnabled(!isVoiceEnabled)
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = if (isVoiceEnabled) Color(0xFF4CAF50) else Color.Unspecified
                                    )
                                }
                            )
                            if (isNavigating && remainingDistance.contains("m") && 
                                remainingDistance.replace(" m", "").toIntOrNull()?.let { it < 500 } == true) {
                                DropdownMenuItem(
                                    text = { Text("Find Parking") },
                                    onClick = {
                                        showMenu = false
                                        scope.launch {
                                            destinationLocation?.let { dest ->
                                                parkingService.findParkingNearDestination(dest).onSuccess { spots ->
                                                    parkingSpots = spots
                                                    showParkingSheet = true
                                                }
                                            }
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Place, contentDescription = null)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Compact Lane Guidance
        if (showLaneGuidance && currentLaneGuidance != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 70.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.95f),
                shadowElevation = 4.dp
            ) {
                CompactLaneGuidance(
                    laneGuidance = currentLaneGuidance!!,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
        
        // Compact Speed Display - Bottom Left Corner
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 180.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 4.dp
        ) {
            SpeedDisplay(
                currentSpeed = speedData.currentSpeed,
                speedLimit = speedData.speedLimit,
                isExceeding = speedData.isExceeding,
                modifier = Modifier.padding(12.dp)
            )
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 70.dp)
        ) {
            
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
            
            // Compact Weather/Hazard Warnings (only show critical)
            currentWeather?.let { weather ->
                if (weather.isDangerous) {
                    val alerts = weatherService.getWeatherAlerts(weather)
                    if (alerts.isNotEmpty()) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(top = 90.dp, start = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFD32F2F).copy(alpha = 0.9f),
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = "Warning",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = alerts.first().message,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
        
        
        // Minimalist Bottom Navigation Panel
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            color = Color.White,
            shadowElevation = 8.dp,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            when {
                // Loading state - compact
                routeInfo == null -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF1976D2),
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = "Calculating route...",
                            fontSize = 15.sp,
                            color = Color(0xFF757575)
                        )
                    }
                }
                
                // Ready state - Clean and simple
                !isNavigating -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Just destination and start button
                        Text(
                            text = destination,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF212121),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "${routeInfo?.distance?.div(1000)?.toInt() ?: 0} km",
                                fontSize = 14.sp,
                                color = Color(0xFF757575)
                            )
                            Text(
                                text = "\u00b7",
                                fontSize = 14.sp,
                                color = Color(0xFFBDBDBD)
                            )
                            Text(
                                text = "${routeInfo?.duration?.div(60)?.toInt() ?: 0} min",
                                fontSize = 14.sp,
                                color = Color(0xFF757575)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Single prominent start button
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
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1976D2)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Start",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
                
                // Active navigation - Compact turn-by-turn
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        // Main instruction - clean layout
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Direction icon - smaller
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1976D2),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.ArrowBack,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            
                            // Instruction text
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentInstruction,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF212121),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "in $distanceToTurn m",
                                    fontSize = 13.sp,
                                    color = Color(0xFF757575)
                                )
                            }
                        }
                        
                        // Next instruction - only if exists, more compact
                        if (nextInstruction != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.padding(start = 60.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Then:",
                                    fontSize = 12.sp,
                                    color = Color(0xFF9E9E9E)
                                )
                                Text(
                                    text = nextInstruction!!,
                                    fontSize = 12.sp,
                                    color = Color(0xFF757575),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
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
