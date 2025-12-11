package org.aurora.android.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

@Composable
fun HomeScreen(
    initialOrigin: String = "",
    initialDestination: String = "",
    initialOriginLocation: LatLng? = null,
    initialDestinationLocation: LatLng? = null,
    onStateChange: (String, String, LatLng?, LatLng?) -> Unit = { _, _, _, _ -> },
    onStartNavigation: (String, String) -> Unit,
    onMultiStopNavigation: () -> Unit = {},
    onMapPicker: (String, LatLng?, (LatLng, String) -> Unit) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val locationService = remember { LocationService(context) }
    val reminderService = remember { org.aurora.android.reminder.DepartureReminderService(context) }
    
    var origin by remember { mutableStateOf(initialOrigin) }
    var destination by remember { mutableStateOf(initialDestination) }
    var originLocation by remember { mutableStateOf(initialOriginLocation) }
    var destinationLocation by remember { mutableStateOf(initialDestinationLocation) }
    var showReminderDialog by remember { mutableStateOf(false) }
    
    // Update parent state when local state changes
    LaunchedEffect(origin, destination, originLocation, destinationLocation) {
        onStateChange(origin, destination, originLocation, destinationLocation)
    }
    var showQuickActions by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // Top Header
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
                Text(
                    text = "Where to?",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Origin Field
                OutlinedTextField(
                    value = origin,
                    onValueChange = { 
                        origin = it
                        originLocation = null // Clear location when typing manually
                    },
                    placeholder = { Text("Current location", color = Color(0xFF9E9E9E)) },
                    trailingIcon = {
                        Row {
                            // Use Current Location button
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
                            // Pick on Map button
                            IconButton(
                                onClick = {
                                    Log.d("HomeScreen", "Opening map picker for origin, current: $originLocation")
                                    onMapPicker("Select Origin", originLocation) { location, address ->
                                        Log.d("HomeScreen", "Map picker returned origin: $location, address: $address")
                                        originLocation = location
                                        origin = address
                                        Log.d("HomeScreen", "After update - origin field: $origin")
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
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1E88E5),
                        unfocusedBorderColor = Color(0xFFE0E0E0)
                    ),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Destination Field
                OutlinedTextField(
                    value = destination,
                    onValueChange = { 
                        destination = it
                        destinationLocation = null // Clear location when typing manually
                    },
                    placeholder = { Text("Where are you going?", color = Color(0xFF9E9E9E)) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF1E88E5)
                        )
                    },
                    trailingIcon = {
                        Row {
                            if (destination.isNotEmpty()) {
                                IconButton(onClick = { 
                                    destination = ""
                                    destinationLocation = null
                                }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = Color(0xFF9E9E9E)
                                    )
                                }
                            }
                            // Pick on Map button
                            IconButton(
                                onClick = {
                                    Log.d("HomeScreen", "Opening map picker for destination, current: $destinationLocation")
                                    onMapPicker("Select Destination", destinationLocation) { location, address ->
                                        Log.d("HomeScreen", "Map picker returned destination: $location, address: $address")
                                        destinationLocation = location
                                        destination = address
                                        Log.d("HomeScreen", "After update - destination field: $destination")
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
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1E88E5),
                        unfocusedBorderColor = Color(0xFFE0E0E0)
                    ),
                    singleLine = true
                )
                
                if (destination.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = { onStartNavigation(origin.ifEmpty { "Current location" }, destination) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E88E5)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Navigation", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Multi-stop button
                OutlinedButton(
                    onClick = onMultiStopNavigation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF1E88E5)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Plan Multi-Stop Route", fontSize = 16.sp, fontWeight = FontWeight.Medium)
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
                            contentColor = Color(0xFFFF9800)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Set Departure Reminder", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        
        // Quick Actions
        if (showQuickActions) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Quick Actions",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF757575),
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
                        onClick = { destination = "Home" }
                    )
                    
                    QuickActionCard(
                        icon = Icons.Default.Star,
                        label = "Work",
                        modifier = Modifier.weight(1f),
                        onClick = { destination = "Work" }
                    )
                    
                    QuickActionCard(
                        icon = Icons.Default.FavoriteBorder,
                        label = "Favorites",
                        modifier = Modifier.weight(1f),
                        onClick = { /* Show favorites */ }
                    )
                }
            }
        }
        
        // Recent Destinations
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Recent",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF757575),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    RecentDestinationItem(
                        name = "Mall of Asia",
                        address = "Seaside Boulevard, Pasay",
                        icon = Icons.Default.ShoppingCart,
                        onClick = { destination = "Mall of Asia" }
                    )
                    Divider(color = Color(0xFFE0E0E0))
                    RecentDestinationItem(
                        name = "Coffee Shop",
                        address = "Makati Avenue, Makati",
                        icon = Icons.Default.Place,
                        onClick = { destination = "Coffee Shop" }
                    )
                }
            }
        }
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
            .height(80.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color(0xFF1E88E5),
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color(0xFF212121),
                fontWeight = FontWeight.Medium
            )
        }
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
            tint = Color(0xFF9E9E9E)
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
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Destination: $destination",
                    fontSize = 14.sp,
                    color = Color(0xFF757575)
                )
                
                // Departure Time Picker
                Text("Departure Time", fontWeight = FontWeight.Medium)
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
                        label = { Text("Hour") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Text(":", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    // Minute
                    OutlinedTextField(
                        value = selectedMinute.toString().padStart(2, '0'),
                        onValueChange = { 
                            it.toIntOrNull()?.let { minute ->
                                if (minute in 0..59) selectedMinute = minute
                            }
                        },
                        label = { Text("Minute") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                
                // Estimated Duration
                Text("Estimated Travel Time (minutes)", fontWeight = FontWeight.Medium)
                OutlinedTextField(
                    value = estimatedDuration.toString(),
                    onValueChange = {
                        it.toIntOrNull()?.let { duration ->
                            if (duration > 0) estimatedDuration = duration
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Reminder Before
                Text("Remind me before (minutes)", fontWeight = FontWeight.Medium)
                OutlinedTextField(
                    value = reminderMinutes.toString(),
                    onValueChange = {
                        it.toIntOrNull()?.let { minutes ->
                            if (minutes > 0) reminderMinutes = minutes
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Text(
                    "You'll be notified $reminderMinutes minutes before $selectedHour:${selectedMinute.toString().padStart(2, '0')}",
                    fontSize = 12.sp,
                    color = Color(0xFF757575),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
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
                }
            ) {
                Text("Set Reminder", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
