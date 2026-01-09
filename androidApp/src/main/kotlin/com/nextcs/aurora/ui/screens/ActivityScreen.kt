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
    
    // Load actual trip data
    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            val result = tripHistoryService.getAllTrips()
            result.onSuccess { trips ->
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
            title = { Text("Clear All Trips?") },
            text = { Text("This will permanently delete all your trip history. This action cannot be undone.") },
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
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFD32F2F)
                    )
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Add Cost Dialog
    if (showAddCostDialog) {
        var distance by remember { mutableStateOf("") }
        var tollCost by remember { mutableStateOf("") }
        var parkingCost by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showAddCostDialog = false },
            title = { Text("Add Trip Cost") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Add cost data for a sample trip", fontSize = 14.sp, color = Color(0xFF757575))
                    
                    OutlinedTextField(
                        value = distance,
                        onValueChange = { distance = it },
                        label = { Text("Distance (km)") },
                        placeholder = { Text("25.0") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = tollCost,
                        onValueChange = { tollCost = it },
                        label = { Text("Toll Cost ($)") },
                        placeholder = { Text("5.50") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = parkingCost,
                        onValueChange = { parkingCost = it },
                        label = { Text("Parking Cost ($)") },
                        placeholder = { Text("10.00") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
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
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCostDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // Fixed Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Text(
                text = "Activity",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF212121),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )
        }
        
        // Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stats Summary
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            } else {
                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        icon = Icons.Default.Star,
                        value = weeklyDistance,
                        unit = "km",
                        label = "Distance",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = Icons.Default.DateRange,
                        value = timeSaved,
                        unit = "",
                        label = "Saved",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = Icons.Default.Build,
                        value = hazardsAvoided.toString(),
                        unit = "",
                        label = "Hazards",
                        modifier = Modifier.weight(1f)
                    )
                }
                    
                    // Driving Score Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Driving score badge
                            Surface(
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = when {
                                    tripHistory.isEmpty() -> Color(0xFF9E9E9E)
                                    else -> {
                                        val avgScore = tripHistory.mapNotNull { trip ->
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
                                        when {
                                            avgScore >= 90 -> Color(0xFF4CAF50)
                                            avgScore >= 75 -> Color(0xFF8BC34A)
                                            avgScore >= 60 -> Color(0xFFFFC107)
                                            else -> Color(0xFFFF9800)
                                        }
                                    }
                                },
                                modifier = Modifier.size(64.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = if (tripHistory.isEmpty()) "--" else {
                                            val avgScore = tripHistory.mapNotNull { trip ->
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
                                            avgScore.toString()
                                        },
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Score",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Driving Score",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF212121)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (tripHistory.isEmpty()) "No trips yet" else {
                                        val avgScore = tripHistory.mapNotNull { trip ->
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
                                        when {
                                            avgScore >= 90 -> "Excellent driving!"
                                            avgScore >= 75 -> "Good and safe!"
                                            avgScore >= 60 -> "Room for improvement"
                                            else -> "Drive more carefully"
                                        }
                                    },
                                    fontSize = 13.sp,
                                    color = Color(0xFF757575)
                                )
                            }
                            
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    
                    // Monthly Cost Summary Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAddCostDialog = true },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            if (monthlyCosts.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Monthly Costs",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF212121)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = monthlyCosts.first().month,
                                            fontSize = 13.sp,
                                            color = Color(0xFF757575)
                                        )
                                    }
                                    
                                    Text(
                                        text = "$${String.format("%.2f", monthlyCosts.first().totalTripCost)}",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1976D2)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    CostBreakdownItem(
                                        label = "Toll",
                                        amount = monthlyCosts.first().totalTollCost
                                    )
                                    CostBreakdownItem(
                                        label = "Parking",
                                        amount = monthlyCosts.first().totalParkingCost
                                    )
                                    CostBreakdownItem(
                                        label = "Fuel",
                                        amount = monthlyCosts.first().totalFuelCost
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Text(
                                    text = "${monthlyCosts.first().totalTrips} trips • Avg: $${String.format("%.2f", monthlyCosts.first().averageCostPerTrip)}/trip",
                                    fontSize = 12.sp,
                                    color = Color(0xFF757575)
                                )
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Monthly Costs",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF212121)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "No cost data yet",
                                            fontSize = 13.sp,
                                            color = Color(0xFF757575)
                                        )
                                    }
                                    
                                    Text(
                                        text = "$0.00",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF9E9E9E)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Text(
                                    text = "Complete trips to track costs automatically",
                                    fontSize = 13.sp,
                                    color = Color(0xFF757575)
                                )
                            }
                        }
                    }
                
                // Monthly Reports Card
                if (monthlyStats.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                                    text = "Monthly Reports",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF212121)
                                )
                                IconButton(onClick = { isMonthlyExpanded = !isMonthlyExpanded }) {
                                    Icon(
                                        if (isMonthlyExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = Color(0xFF757575)
                                    )
                                }
                            }
                            
                            AnimatedVisibility(visible = isMonthlyExpanded) {
                                Column(modifier = Modifier.padding(top = 12.dp)) {
                                    monthlyStats.forEach { (month, tripCount) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = month,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF212121),
                                                modifier = Modifier.width(70.dp)
                                            )
                                            
                                            Box(modifier = Modifier.weight(1f)) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(tripCount / monthlyStats.maxOf { it.second }.toFloat())
                                                        .height(28.dp)
                                                        .background(
                                                            Color(0xFF1976D2).copy(alpha = 0.15f),
                                                            RoundedCornerShape(6.dp)
                                                        )
                                                )
                                            }
                                            
                                            Text(
                                                text = "$tripCount",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF1976D2),
                                                modifier = Modifier.width(40.dp),
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
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = Color.Transparent,
                            contentColor = Color(0xFF1976D2)
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = {
                                    Text(
                                        "Recent Trips",
                                        fontSize = 15.sp,
                                        fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (selectedTab == 0) Color(0xFF1976D2) else Color(0xFF757575)
                                    )
                                }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = {
                                    Text(
                                        "Saved",
                                        fontSize = 15.sp,
                                        fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (selectedTab == 1) Color(0xFF1976D2) else Color(0xFF757575)
                                    )
                                }
                            )
                        }
                        
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (selectedTab == 0) {
                                if (tripHistory.isEmpty()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.Place,
                                            contentDescription = null,
                                            tint = Color(0xFF9E9E9E),
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "No trips yet",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF757575)
                                        )
                                    }
                                } else {
                                    tripHistory.forEach { trip ->
                                        TripCard(trip)
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                }
                            } else {
                                if (savedRoutes.isEmpty()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color(0xFF9E9E9E),
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "No saved routes",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF757575)
                                        )
                                    }
                                } else {
                                    savedRoutes.forEach { route ->
                                        SavedRouteCard(
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

@Composable
fun StatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    unit: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                tint = Color(0xFF1976D2),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = value,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121)
                )
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = unit,
                        fontSize = 13.sp,
                        color = Color(0xFF757575)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color(0xFF757575)
            )
        }
    }
}

@Composable
fun TripCard(trip: TripDisplayRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = trip.route,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF212121)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = trip.date,
                        fontSize = 12.sp,
                        color = Color(0xFF757575)
                    )
                }
                
                if (trip.hazardsAvoided > 0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFE8F5E9)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Build,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "${trip.hazardsAvoided} avoided",
                                fontSize = 11.sp,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TripInfoItem(Icons.Default.Star, trip.distance)
                TripInfoItem(Icons.Default.DateRange, trip.duration)
                TripInfoItem(Icons.Default.Star, trip.avgSpeed)
            }
        }
    }
}

@Composable
fun TripInfoItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFF757575),
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            fontSize = 12.sp,
            color = Color(0xFF757575)
        )
    }
}

@Composable
fun SavedRouteCard(
    route: SavedRouteRecord,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Route?") },
            text = { Text("Are you sure you want to delete \"${route.name}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFD32F2F)
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
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
                    text = route.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF212121),
                    modifier = Modifier.weight(1f)
                )
                Row {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Toggle favorite",
                            tint = if (route.isFavorite) Color(0xFFFFD700) else Color(0xFFE0E0E0),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete route",
                            tint = Color(0xFF757575),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Place,
                    contentDescription = null,
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${route.origin} → ${route.destination}",
                    fontSize = 14.sp,
                    color = Color(0xFF757575)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = null,
                        tint = Color(0xFF1976D2),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = String.format("%.1f km", route.distance),
                        fontSize = 13.sp,
                        color = Color(0xFF424242)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        tint = Color(0xFF1976D2),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${route.estimatedTime} min",
                        fontSize = 13.sp,
                        color = Color(0xFF424242)
                    )
                }
            }
        }
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
