package com.nextcs.aurora.ui.screens

import android.util.Log
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.LatLng
import com.nextcs.aurora.location.LocationService
import com.nextcs.aurora.navigation.TripHistoryService
import com.nextcs.aurora.navigation.TripRecord
import com.nextcs.aurora.ui.components.LocationSearchField
import com.nextcs.aurora.ui.components.AIAssistantDialog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    initialOrigin: String = "",
    initialDestination: String = "",
    initialOriginLocation: LatLng? = null,
    initialDestinationLocation: LatLng? = null,
    onStateChange: (String, String, LatLng?, LatLng?) -> Unit = { _, _, _, _ -> },
    onStartNavigation: (String, String, LatLng?, LatLng?) -> Unit,
    onMultiStopNavigation: () -> Unit = {},
    onMapPicker: (String, LatLng?, (LatLng, String) -> Unit) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val locationService = remember { LocationService(context) }
    val reminderService = remember { com.nextcs.aurora.reminder.DepartureReminderService(context) }
    val tripHistoryService = remember { TripHistoryService(context) }
    val placesService = remember { com.nextcs.aurora.location.PlacesAutocompleteService(context) }
    val scope = rememberCoroutineScope()
    
    // Get current user ID to trigger reloads
    val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    
    var origin by remember { mutableStateOf(initialOrigin) }
    var destination by remember { mutableStateOf(initialDestination) }
    var originLocation by remember { mutableStateOf(initialOriginLocation) }
    var destinationLocation by remember { mutableStateOf(initialDestinationLocation) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showAIAssistant by remember { mutableStateOf(false) }
    var recentTrips by remember { mutableStateOf<List<TripRecord>>(emptyList()) }
    
    // Load recent trips - refresh when user changes
    LaunchedEffect(currentUserId) {
        scope.launch {
            android.util.Log.d("HomeScreen", "Loading trips for user: $currentUserId")
            tripHistoryService.getAllTrips().onSuccess { trips ->
                recentTrips = trips.take(10) // Show up to 10 recent trips
                android.util.Log.d("HomeScreen", "Loaded ${trips.size} trips")
            }.onFailure {
                android.util.Log.e("HomeScreen", "Failed to load trips", it)
            }
        }
    }
    
    // Update parent state when local state changes
    LaunchedEffect(origin, destination, originLocation, destinationLocation) {
        onStateChange(origin, destination, originLocation, destinationLocation)
    }
    var showQuickActions by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F7))
                .verticalScroll(scrollState)
    ) {
        // Minimal App Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.Transparent,
            shadowElevation = 0.dp
        ) {
            Text(
                text = "Where to?",
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1C1C1E),
                letterSpacing = (-0.5).sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )
        }
        
        // Input Section Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                
                // Origin Field with Places Autocomplete
                LocationSearchField(
                    value = origin,
                    onValueChange = { newValue, location ->
                        origin = newValue
                        originLocation = location
                    },
                    placeholder = "Start location",
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color(0xFF4CAF50), shape = CircleShape)
                        )
                    },
                    onCurrentLocationClick = {
                        val currentLoc = locationService.getLastKnownLocation()
                        if (currentLoc != null) {
                            originLocation = currentLoc
                            origin = "Current Location"
                            // Reverse geocode in background
                            scope.launch {
                                val address = locationService.reverseGeocode(currentLoc)
                                origin = address
                            }
                        }
                    },
                    onMapPickerClick = {
                        Log.d("HomeScreen", "Opening map picker for origin, current: $originLocation")
                        onMapPicker("Select Origin", originLocation) { location, address ->
                            Log.d("HomeScreen", "Map picker returned origin: $location, address: $address")
                            originLocation = location
                            origin = address
                            Log.d("HomeScreen", "After update - origin field: $origin")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Destination Field with Places Autocomplete
                LocationSearchField(
                    value = destination,
                    onValueChange = { newValue, location ->
                        destination = newValue
                        destinationLocation = location
                    },
                    placeholder = "End location",
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF1976D2),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onMapPickerClick = {
                        Log.d("HomeScreen", "Opening map picker for destination, current: $destinationLocation")
                        onMapPicker("Select Destination", destinationLocation) { location, address ->
                            Log.d("HomeScreen", "Map picker returned destination: $location, address: $address")
                            destinationLocation = location
                            destination = address
                            Log.d("HomeScreen", "After update - destination field: $destination")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (destination.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { onStartNavigation(origin.ifEmpty { "Current location" }, destination, originLocation, destinationLocation) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF007AFF)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text(
                            "Start Navigation",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.3).sp
                        )
                    }
                }
                
                // Multi-stop button
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onMultiStopNavigation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF007AFF)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD1D1D6)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Plan Multi-Stop Route",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Set Reminder button (shown when destination is set)
                if (destination.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showReminderDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF8E8E93)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD1D1D6)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Set Departure Reminder",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Quick Actions
        if (showQuickActions) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = "Quick Actions",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF8E8E93),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        icon = Icons.Default.Home,
                        label = "Home",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            destination = "Home"
                            destinationLocation = null
                        }
                    )
                    
                    QuickActionCard(
                        icon = Icons.Default.Star,
                        label = "Work",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            destination = "Work"
                            destinationLocation = null
                        }
                    )
                    
                    QuickActionCard(
                        icon = Icons.Default.LocationOn,
                        label = "Nearby",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            // Use current location as origin
                            val currentLoc = locationService.getLastKnownLocation()
                            if (currentLoc != null) {
                                originLocation = currentLoc
                                origin = "Current Location"
                            }
                        }
                    )
                }
            }
        }
        
        // Recent Trips
        if (recentTrips.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = "Recent Trips",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF8E8E93),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                        recentTrips.forEachIndexed { index, trip ->
                            RecentTripItem(
                                trip = trip,
                                onClick = {
                                    destination = trip.destination
                                    destinationLocation = null
                                }
                            )
                            if (index < recentTrips.size - 1) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(Color(0xFFF2F2F7))
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
    
    // AI Assistant FAB
    FloatingActionButton(
        onClick = { showAIAssistant = true },
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(20.dp),
        containerColor = Color(0xFF007AFF),
        shape = RoundedCornerShape(16.dp),
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 4.dp,
            pressedElevation = 6.dp
        )
    ) {
        Icon(
            Icons.Default.LocationOn,
            contentDescription = "AI Assistant",
            tint = Color.White,
            modifier = Modifier.size(26.dp)
        )
    }
}
    
    // AI Assistant Dialog
    if (showAIAssistant) {
        AIAssistantDialog(
            onDismiss = { showAIAssistant = false },
            onRouteSelected = { fromAI, toAI, waypointsAI ->
                showAIAssistant = false
                
                // Handle multi-stop routes (waypoints provided)
                if (waypointsAI.isNotEmpty()) {
                    Log.d("HomeScreen", "Multi-stop route selected - Origin: '$fromAI', Dest: '$toAI', Waypoints: $waypointsAI")
                    // For multi-stop, navigate to multi-stop planner
                    destination = toAI
                    onMultiStopNavigation()
                } else {
                    // Single route - geocode addresses first, then start navigation
                    scope.launch {
                        try {
                            Log.d("HomeScreen", "AI Route Selected - Origin: '$fromAI', Dest: '$toAI'")
                            
                            // Handle origin - get actual current location if needed
                            val origCoords = if (fromAI.isNotEmpty() && fromAI != "Current location") {
                                Log.d("HomeScreen", "Geocoding origin: $fromAI")
                                val result = placesService.searchAndGetCoordinates(fromAI)
                                Log.d("HomeScreen", "Origin geocoded: ${result?.first}")
                                result
                            } else {
                                Log.d("HomeScreen", "Getting current location for origin")
                                val currentLoc = locationService.getLastKnownLocation()
                                Log.d("HomeScreen", "Current location: $currentLoc")
                                if (currentLoc != null) {
                                    Pair(currentLoc, "Current location")
                                } else {
                                    // Use default Manila location as fallback
                                    Log.w("HomeScreen", "Using default location as fallback")
                                    Pair(LatLng(14.5995, 120.9842), "Current location")
                                }
                            }
                            
                            // Handle destination
                            Log.d("HomeScreen", "Geocoding destination: $toAI")
                            val destCoords = placesService.searchAndGetCoordinates(toAI)
                            Log.d("HomeScreen", "Destination geocoded: ${destCoords?.first}")
                            
                            if (destCoords != null && origCoords != null) {
                                // Update state with coordinates
                                origin = fromAI.ifEmpty { "Current location" }
                                destination = toAI
                                originLocation = origCoords.first
                                destinationLocation = destCoords.first
                                
                                Log.d("HomeScreen", "✓ Both coordinates ready - Origin: ${origCoords.first}, Dest: ${destCoords.first}")
                                Log.d("HomeScreen", "Calling onStateChange with: origin=$origin, dest=$destination, origLoc=$originLocation, destLoc=$destinationLocation")
                                
                                // Notify parent of state change
                                onStateChange(origin, destination, originLocation, destinationLocation)
                                
                                Log.d("HomeScreen", "Calling onStartNavigation with: origin=$origin, dest=$destination, origLoc=$originLocation, destLoc=$destinationLocation")
                                
                                // Start navigation with coordinates
                                onStartNavigation(origin, destination, originLocation, destinationLocation)
                            } else {
                                Log.e("HomeScreen", "Failed to get coordinates - Origin: ${origCoords?.first}, Dest: ${destCoords?.first}")
                            }
                        } catch (e: Exception) {
                            Log.e("HomeScreen", "Error geocoding addresses", e)
                            e.printStackTrace()
                        }
                    }
                }
            }
        )
    }
    
    // Departure Reminder Dialog
    if (showReminderDialog) {
        ReminderDialog(
            destination = destination,
            onDismiss = { showReminderDialog = false },
            onConfirm = { departureTime, reminderMinutes, estimatedDuration ->
                try {
                    reminderService.scheduleReminder(
                        destination = destination,
                        departureTime = departureTime,
                        travelDuration = estimatedDuration,
                        reminderMinutesBefore = reminderMinutes
                    )
                    showReminderDialog = false
                    // Show success message (you could use a Snackbar here)
                    Log.d("HomeScreen", "Reminder scheduled for $destination")
                } catch (e: Exception) {
                    Log.e("HomeScreen", "Failed to schedule reminder", e)
                }
            }
        )
    }
}

@Composable
fun QuickActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color(0xFF007AFF),
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                color = Color(0xFF1C1C1E),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun RecentTripItem(
    trip: TripRecord,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault()) }
    val distanceKm = (trip.distance / 1000.0).let { 
        if (it < 1) "%.0f m".format(trip.distance.toDouble()) 
        else "%.1f km".format(it) 
    }
    val durationMin = (trip.duration / 60).toString()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Place,
            contentDescription = null,
            tint = Color(0xFF007AFF),
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = trip.destination,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1C1C1E),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$distanceKm • $durationMin min • ${dateFormat.format(Date(trip.timestamp))}",
                fontSize = 13.sp,
                color = Color(0xFF8E8E93)
            )
        }
        
        Icon(
            Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFFD1D1D6),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun RecentDestinationItem(
    name: String,
    address: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = Color(0xFFF5F5F5),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color(0xFF757575),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF212121)
            )
            Text(
                text = address,
                fontSize = 13.sp,
                color = Color(0xFF757575)
            )
        }
        
        Icon(
            Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFF757575)
        )
    }
}

@Composable
fun ReminderDialog(
    destination: String,
    onDismiss: () -> Unit,
    onConfirm: (departureTime: Long, reminderMinutes: Int, estimatedDuration: Int) -> Unit
) {
    var selectedHour by remember { mutableStateOf(12) }
    var selectedMinute by remember { mutableStateOf(0) }
    var reminderMinutes by remember { mutableStateOf(10) }
    var estimatedDuration by remember { mutableStateOf(30) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Set Departure Reminder",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1C1C1E),
                letterSpacing = (-0.3).sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Destination: $destination",
                    fontSize = 15.sp,
                    color = Color(0xFF8E8E93)
                )
                
                // Departure Time Picker
                Text(
                    "Departure Time",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1C1C1E)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hour
                    OutlinedTextField(
                        value = selectedHour.toString(),
                        onValueChange = { 
                            it.toIntOrNull()?.let { hour ->
                                if (hour in 0..23) selectedHour = hour
                            }
                        },
                        label = { Text("Hour", fontSize = 13.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF007AFF),
                            unfocusedBorderColor = Color(0xFFD1D1D6)
                        )
                    )
                    Text(":", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1C1C1E))
                    // Minute
                    OutlinedTextField(
                        value = selectedMinute.toString().padStart(2, '0'),
                        onValueChange = { 
                            it.toIntOrNull()?.let { minute ->
                                if (minute in 0..59) selectedMinute = minute
                            }
                        },
                        label = { Text("Minute", fontSize = 13.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF007AFF),
                            unfocusedBorderColor = Color(0xFFD1D1D6)
                        )
                    )
                }
                
                // Estimated Duration
                Text(
                    "Estimated Travel Time (minutes)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1C1C1E)
                )
                OutlinedTextField(
                    value = estimatedDuration.toString(),
                    onValueChange = {
                        it.toIntOrNull()?.let { duration ->
                            if (duration > 0) estimatedDuration = duration
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF007AFF),
                        unfocusedBorderColor = Color(0xFFD1D1D6)
                    )
                )
                
                // Reminder Before
                Text(
                    "Remind me before (minutes)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1C1C1E)
                )
                OutlinedTextField(
                    value = reminderMinutes.toString(),
                    onValueChange = {
                        it.toIntOrNull()?.let { minutes ->
                            if (minutes > 0) reminderMinutes = minutes
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF007AFF),
                        unfocusedBorderColor = Color(0xFFD1D1D6)
                    )
                )
                
                Surface(
                    color = Color(0xFFF2F2F7),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        "You'll be notified $reminderMinutes minutes before $selectedHour:${selectedMinute.toString().padStart(2, '0')}",
                        modifier = Modifier.padding(12.dp),
                        fontSize = 13.sp,
                        color = Color(0xFF8E8E93),
                        lineHeight = 18.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Calculate departure time (today at selected time)
                    val calendar = java.util.Calendar.getInstance()
                    calendar.set(java.util.Calendar.HOUR_OF_DAY, selectedHour)
                    calendar.set(java.util.Calendar.MINUTE, selectedMinute)
                    calendar.set(java.util.Calendar.SECOND, 0)
                    calendar.set(java.util.Calendar.MILLISECOND, 0)
                    
                    // If time is in the past, set it for tomorrow
                    if (calendar.timeInMillis < System.currentTimeMillis()) {
                        calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
                    }
                    
                    onConfirm(calendar.timeInMillis, reminderMinutes, estimatedDuration)
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(0xFF007AFF)
                )
            ) {
                Text("Set Reminder", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(0xFF8E8E93)
                )
            ) {
                Text("Cancel", fontSize = 15.sp)
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White
    )
}
