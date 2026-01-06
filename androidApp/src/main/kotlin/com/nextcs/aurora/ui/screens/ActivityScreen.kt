package com.nextcs.aurora.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.nextcs.aurora.navigation.TripHistoryService
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
    val scope = rememberCoroutineScope()
    
    var selectedTab by remember { mutableStateOf(0) }
    var tripHistory by remember { mutableStateOf<List<TripDisplayRecord>>(emptyList()) }
    var weeklyDistance by remember { mutableStateOf("0.0") }
    var timeSaved by remember { mutableStateOf("0h 0m") }
    var hazardsAvoided by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // Header
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
                    text = "Activity",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Stats Summary
                if (isLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            icon = Icons.Default.Star,
                            value = weeklyDistance,
                            unit = "km",
                            label = "Total Distance",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            icon = Icons.Default.DateRange,
                            value = timeSaved,
                            unit = "",
                            label = "Time Saved",
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
                }
            }
        }
        
        // Trip History
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
                    text = "Recent Trips",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF212121)
                )
                
                if (tripHistory.isNotEmpty()) {
                    TextButton(
                        onClick = { showDeleteDialog = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFFD32F2F)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear all trips",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear All", fontSize = 14.sp)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (tripHistory.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = null,
                            tint = Color(0xFF9E9E9E),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No trips yet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF212121)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Start navigating to see your trip history",
                            fontSize = 14.sp,
                            color = Color(0xFF757575)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(tripHistory) { trip ->
                        TripCard(trip)
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
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color(0xFF1E88E5),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = value,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121)
                )
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = unit,
                        fontSize = 12.sp,
                        color = Color(0xFF757575)
                    )
                }
            }
            Text(
                text = label,
                fontSize = 11.sp,
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
        shape = RoundedCornerShape(12.dp)
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
                        color = Color(0xFF9E9E9E)
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
