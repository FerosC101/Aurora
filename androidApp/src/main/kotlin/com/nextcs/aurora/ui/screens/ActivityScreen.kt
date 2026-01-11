package com.nextcs.aurora.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextcs.aurora.navigation.TripHistoryService
import com.nextcs.aurora.navigation.SavedRoutesService
import com.nextcs.aurora.navigation.SavedRouteRecord
import com.nextcs.aurora.analytics.CostTrackingService
import com.nextcs.aurora.analytics.MonthlyCostSummary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class TripDisplayRecord(
    val id: String,
    val date: String,
    val route: String,
    val distance: String,
    val duration: String,
    val avgSpeed: String,
    val hazardsAvoided: Int
)

@Composable
fun ActivityScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tripHistoryService = remember { TripHistoryService(context) }
    val savedRoutesService = remember { SavedRoutesService(context) }
    val costTrackingService = remember { CostTrackingService(context) }
    val scope = rememberCoroutineScope()
    
    // Get current user ID to trigger reloads
    val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    
    var selectedTab by remember { mutableStateOf(0) } // 0 = Recent Trips, 1 = Saved Routes
    var tripHistory by remember { mutableStateOf<List<TripDisplayRecord>>(emptyList()) }
    var savedRoutes by remember { mutableStateOf<List<SavedRouteRecord>>(emptyList()) }
    var monthlyCosts by remember { mutableStateOf<List<MonthlyCostSummary>>(emptyList()) }
    var monthlyStats by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var isMonthlyExpanded by remember { mutableStateOf(false) }
    var weeklyDistance by remember { mutableStateOf("0.0") }
    var timeSaved by remember { mutableStateOf("0h 0m") }
    var hazardsAvoided by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAddCostDialog by remember { mutableStateOf(false) }
    
    // Load actual trip data - refresh when user changes
    LaunchedEffect(currentUserId) {
        scope.launch {
            android.util.Log.d("ActivityScreen", "Loading data for user: $currentUserId")
            isLoading = true
            val result = tripHistoryService.getAllTrips()
            result.onSuccess { trips ->
                android.util.Log.d("ActivityScreen", "Loaded ${trips.size} trips")
                // Convert to display format
                tripHistory = trips.map { trip ->
                    val dateFormat = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())
                    val date = dateFormat.format(Date(trip.timestamp))
                    val distanceKm = trip.distance / 1000.0
                    val durationMin = trip.duration / 60
                    val avgSpeed = if (durationMin > 0) {
                        (distanceKm / (durationMin / 60.0)).toInt()
                    } else 0
                    
                    TripDisplayRecord(
                        id = trip.id,
                        date = date,
                        route = "${trip.origin} → ${trip.destination}",
                        distance = String.format("%.1f km", distanceKm),
                        duration = "$durationMin min",
                        avgSpeed = "$avgSpeed km/h",
                        hazardsAvoided = trip.hazardsEncountered
                    )
                }
                
                // Calculate analytics
                val analyticsResult = tripHistoryService.getAnalytics()
                analyticsResult.onSuccess { analytics ->
                    weeklyDistance = String.format("%.1f", analytics.totalDistance)
                    val hours = analytics.totalTimeSaved.toInt()
                    val minutes = ((analytics.totalTimeSaved - hours) * 60).toInt()
                    timeSaved = "${hours}h ${minutes}m"
                    hazardsAvoided = analytics.hazardsAvoided
                }
            }.onFailure {
                android.util.Log.e("ActivityScreen", "Failed to load trips", it)
            }
            
            // Load saved routes
            val routesResult = savedRoutesService.getAllRoutes()
            routesResult.onSuccess { routes ->
                savedRoutes = routes
            }
            
            // Load monthly costs
            val costsResult = costTrackingService.getMonthlySummary()
            costsResult.onSuccess { costs ->
                monthlyCosts = costs
            }
            
            // Load monthly stats
            val statsResult = tripHistoryService.getMonthlyStats()
            statsResult.onSuccess { stats ->
                monthlyStats = stats
            }
            
            isLoading = false
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { 
                Text(
                    "Clear All Trips?",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1C1C1E)
                ) 
            },
            text = { 
                Text(
                    "This will permanently delete all your trip history. This action cannot be undone.",
                    fontSize = 15.sp,
                    color = Color(0xFF8E8E93)
                ) 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            tripHistoryService.clearAllTrips()
                            tripHistory = emptyList()
                            weeklyDistance = "0.0"
                            timeSaved = "0h 0m"
                            hazardsAvoided = 0
                            showDeleteDialog = false
                        }
                    }
                ) {
                    Text(
                        "Delete All",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFF3B30)
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(
                        "Cancel",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF007AFF)
                    )
                }
            },
            shape = RoundedCornerShape(14.dp),
            containerColor = Color.White
        )
    }
    
    // Add Cost Dialog
    if (showAddCostDialog) {
        var distance by remember { mutableStateOf("") }
        var tollCost by remember { mutableStateOf("") }
        var parkingCost by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showAddCostDialog = false },
            title = { 
                Text(
                    "Add Trip Cost",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1C1C1E)
                ) 
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Add cost data for a sample trip",
                        fontSize = 15.sp,
                        color = Color(0xFF8E8E93)
                    )
                    
                    OutlinedTextField(
                        value = distance,
                        onValueChange = { distance = it },
                        label = { Text("Distance (km)", fontSize = 15.sp) },
                        placeholder = { Text("25.0") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF007AFF),
                            focusedLabelColor = Color(0xFF007AFF)
                        )
                    )
                    
                    OutlinedTextField(
                        value = tollCost,
                        onValueChange = { tollCost = it },
                        label = { Text("Toll Cost ($)", fontSize = 15.sp) },
                        placeholder = { Text("5.50") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF007AFF),
                            focusedLabelColor = Color(0xFF007AFF)
                        )
                    )
                    
                    OutlinedTextField(
                        value = parkingCost,
                        onValueChange = { parkingCost = it },
                        label = { Text("Parking Cost ($)", fontSize = 15.sp) },
                        placeholder = { Text("10.00") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF007AFF),
                            focusedLabelColor = Color(0xFF007AFF)
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val dist = distance.toDoubleOrNull() ?: 0.0
                        val toll = tollCost.toDoubleOrNull() ?: 0.0
                        val parking = parkingCost.toDoubleOrNull() ?: 0.0
                        
                        if (dist > 0) {
                            scope.launch {
                                costTrackingService.saveTripCost(
                                    tripId = "manual_${System.currentTimeMillis()}",
                                    tollCost = toll,
                                    parkingCost = parking,
                                    distanceKm = dist,
                                    vehicleType = "driving"
                                )
                                
                                // Reload costs
                                costTrackingService.getMonthlySummary().onSuccess { costs ->
                                    monthlyCosts = costs
                                }
                                
                                showAddCostDialog = false
                            }
                        }
                    },
                    enabled = distance.toDoubleOrNull() != null && distance.toDoubleOrNull()!! > 0
                ) {
                    Text(
                        "Add",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF007AFF)
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCostDialog = false }) {
                    Text(
                        "Cancel",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF8E8E93)
                    )
                }
            },
            shape = RoundedCornerShape(14.dp),
            containerColor = Color.White
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F7))
    ) {
        // Minimal App Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.Transparent,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Activity",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1C1C1E),
                    letterSpacing = (-0.5).sp
                )
            }
        }
        
        // Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Loading State
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = Color(0xFF007AFF),
                        strokeWidth = 2.5.dp
                    )
                }
            } else {
                // Summary Metrics Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CompactMetricCard(
                        icon = Icons.Default.Place,
                        value = weeklyDistance,
                        unit = "km",
                        label = "Distance",
                        modifier = Modifier.weight(1f)
                    )
                    CompactMetricCard(
                        icon = Icons.Default.DateRange,
                        value = timeSaved.split(" ").firstOrNull() ?: "0h",
                        unit = timeSaved.split(" ").getOrNull(1) ?: "",
                        label = "Saved",
                        modifier = Modifier.weight(1f)
                    )
                    CompactMetricCard(
                        icon = Icons.Default.Build,
                        value = hazardsAvoided.toString(),
                        unit = "",
                        label = "Hazards",
                        modifier = Modifier.weight(1f)
                    )
                }
                    
                // Primary Highlight Card - Driving Score
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Score Badge
                        val avgScore = if (tripHistory.isEmpty()) 0 else {
                            tripHistory.mapNotNull { trip ->
                                try {
                                    val parts = trip.id.split("_")
                                    if (parts.size >= 2) {
                                        val harshBraking = trip.hazardsAvoided
                                        100 - (harshBraking * 5).coerceAtMost(30)
                                    } else {
                                        85
                                    }
                                } catch (e: Exception) {
                                    85
                                }
                            }.average().toInt()
                        }
                        
                        Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = when {
                                tripHistory.isEmpty() -> Color(0xFFF2F2F7)
                                avgScore >= 90 -> Color(0xFF34C759)
                                avgScore >= 75 -> Color(0xFF5AC8FA)
                                avgScore >= 60 -> Color(0xFFFFCC00)
                                else -> Color(0xFFFF9500)
                            }.copy(alpha = 0.15f),
                            modifier = Modifier.size(60.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (tripHistory.isEmpty()) "—" else avgScore.toString(),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = when {
                                        tripHistory.isEmpty() -> Color(0xFFAEAEB2)
                                        avgScore >= 90 -> Color(0xFF34C759)
                                        avgScore >= 75 -> Color(0xFF007AFF)
                                        avgScore >= 60 -> Color(0xFFFFCC00)
                                        else -> Color(0xFFFF9500)
                                    }
                                )
                            }
                        }
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Driving Score",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1C1C1E),
                                letterSpacing = (-0.3).sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (tripHistory.isEmpty()) "No trips recorded" else {
                                    when {
                                        avgScore >= 90 -> "Excellent driving!"
                                        avgScore >= 75 -> "Good and safe"
                                        avgScore >= 60 -> "Room to improve"
                                        else -> "Drive carefully"
                                    }
                                },
                                fontSize = 15.sp,
                                color = Color(0xFF8E8E93)
                            )
                        }
                    }
                }
                    
                // Monthly Costs Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAddCostDialog = true },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    text = "Monthly Costs",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1C1C1E),
                                    letterSpacing = (-0.3).sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (monthlyCosts.isNotEmpty()) monthlyCosts.first().month else "No data",
                                    fontSize = 15.sp,
                                    color = Color(0xFF8E8E93)
                                )
                            }
                            
                            Text(
                                text = if (monthlyCosts.isNotEmpty()) {
                                    "$${String.format("%.2f", monthlyCosts.first().totalTripCost)}"
                                } else {
                                    "$0.00"
                                },
                                fontSize = 28.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (monthlyCosts.isNotEmpty()) Color(0xFF1C1C1E) else Color(0xFFAEAEB2),
                                letterSpacing = (-0.5).sp
                            )
                        }
                        
                        if (monthlyCosts.isEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Complete trips to track costs automatically",
                                fontSize = 14.sp,
                                color = Color(0xFF8E8E93),
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
                
                // Monthly Reports Section
                if (monthlyStats.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isMonthlyExpanded = !isMonthlyExpanded },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Monthly Reports",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1C1C1E),
                                    letterSpacing = (-0.3).sp
                                )
                                Icon(
                                    if (isMonthlyExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Color(0xFF8E8E93),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            AnimatedVisibility(visible = isMonthlyExpanded) {
                                Column(modifier = Modifier.padding(top = 20.dp)) {
                                    monthlyStats.forEach { (month, tripCount) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = month,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF1C1C1E),
                                                modifier = Modifier.width(60.dp)
                                            )
                                            
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(horizontal = 12.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(6.dp)
                                                        .background(
                                                            Color(0xFFF2F2F7),
                                                            RoundedCornerShape(3.dp)
                                                        )
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(tripCount / monthlyStats.maxOf { it.second }.toFloat())
                                                        .height(6.dp)
                                                        .background(
                                                            Color(0xFF007AFF),
                                                            RoundedCornerShape(3.dp)
                                                        )
                                                )
                                            }
                                            
                                            Text(
                                                text = "$tripCount",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF007AFF),
                                                modifier = Modifier.width(30.dp),
                                                textAlign = TextAlign.End
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Trip History Section with Tabs
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Custom Tab Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .padding(top = 16.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TabButton(
                                text = "Recent Trips",
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 }
                            )
                            TabButton(
                                text = "Saved",
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 }
                            )
                        }
                        
                        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                            if (selectedTab == 0) {
                                if (tripHistory.isEmpty()) {
                                    EmptyState(
                                        icon = Icons.Default.Place,
                                        message = "No trips yet"
                                    )
                                } else {
                                    tripHistory.forEach { trip ->
                                        MinimalTripCard(trip)
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                }
                            } else {
                                if (savedRoutes.isEmpty()) {
                                    EmptyState(
                                        icon = Icons.Default.Star,
                                        message = "No saved routes"
                                    )
                                } else {
                                    savedRoutes.forEach { route ->
                                        MinimalSavedRouteCard(
                                            route = route,
                                            onToggleFavorite = {
                                                // Toggle favorite logic
                                            },
                                            onDelete = {
                                                scope.launch {
                                                    savedRoutesService.deleteRoute(route.id)
                                                        .onSuccess {
                                                            savedRoutes = savedRoutes.filter { it.id != route.id }
                                                        }
                                                }
                                            }
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Compact Metric Card - Minimal Design
@Composable
fun CompactMetricCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    unit: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(110.dp),
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
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1C1C1E),
                letterSpacing = (-0.5).sp,
                maxLines = 1
            )
            if (unit.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = unit,
                    fontSize = 13.sp,
                    color = Color(0xFF8E8E93)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                color = Color(0xFF8E8E93)
            )
        }
    }
}

// Custom Tab Button
@Composable
fun TabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Color(0xFF007AFF).copy(alpha = 0.1f) else Color.Transparent,
        modifier = Modifier.height(36.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) Color(0xFF007AFF) else Color(0xFF8E8E93)
            )
        }
    }
}

// Empty State Component
@Composable
fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFFD1D1D6),
            modifier = Modifier.size(44.dp)
        )
        Text(
            text = message,
            fontSize = 15.sp,
            color = Color(0xFF8E8E93)
        )
    }
}

// Minimal Trip Card
@Composable
fun MinimalTripCard(trip: TripDisplayRecord) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f).padding(end = if (trip.hazardsAvoided > 0) 8.dp else 0.dp)
            ) {
                Text(
                    text = trip.route,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1C1C1E),
                    letterSpacing = (-0.2).sp,
                    maxLines = 2,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = trip.date,
                    fontSize = 13.sp,
                    color = Color(0xFF8E8E93),
                    maxLines = 1
                )
            }
            
            if (trip.hazardsAvoided > 0) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF34C759).copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "${trip.hazardsAvoided}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 13.sp,
                        color = Color(0xFF34C759),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MinimalTripStat(trip.distance)
            MinimalTripStat(trip.duration)
            MinimalTripStat(trip.avgSpeed)
        }
        
        // Subtle divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFF2F2F7))
                .padding(top = 8.dp)
        )
    }
}

@Composable
fun MinimalTripStat(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        color = Color(0xFF8E8E93)
    )
}

// Minimal Saved Route Card
@Composable
fun MinimalSavedRouteCard(
    route: SavedRouteRecord,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { 
                Text(
                    "Delete Route?",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                ) 
            },
            text = { 
                Text(
                    "Are you sure you want to delete \"${route.name}\"?",
                    fontSize = 15.sp,
                    color = Color(0xFF8E8E93)
                ) 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFFF3B30)
                    )
                ) {
                    Text("Delete", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = Color(0xFF007AFF))
                }
            },
            shape = RoundedCornerShape(14.dp),
            containerColor = Color.White
        )
    }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = route.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1C1C1E),
                letterSpacing = (-0.2).sp,
                modifier = Modifier.weight(1f)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Favorite",
                        tint = if (route.isFavorite) Color(0xFFFFCC00) else Color(0xFFD1D1D6),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFD1D1D6),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        
        Text(
            text = "${route.origin} → ${route.destination}",
            fontSize = 13.sp,
            color = Color(0xFF8E8E93)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MinimalTripStat(String.format("%.1f km", route.distance))
            MinimalTripStat("${route.estimatedTime} min")
        }
        
        // Subtle divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFF2F2F7))
                .padding(top = 8.dp)
        )
    }
}

@Composable
fun CostBreakdownItem(
    label: String,
    amount: Double
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF757575)
        )
        Text(
            text = "$${String.format("%.2f", amount)}",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF212121)
        )
    }
}
