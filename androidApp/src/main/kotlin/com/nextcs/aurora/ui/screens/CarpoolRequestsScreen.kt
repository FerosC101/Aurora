package com.nextcs.aurora.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.nextcs.aurora.social.RideRequest
import com.nextcs.aurora.social.SocialFirebaseService
import com.nextcs.aurora.social.UserProfile
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarpoolRequestsScreen(
    carpoolId: String,
    onBack: () -> Unit,
    onViewProfile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val socialService = remember { SocialFirebaseService(context) }
    val scope = rememberCoroutineScope()
    
    var rideRequests by remember { mutableStateOf<List<RideRequest>>(emptyList()) }
    var requesterProfiles by remember { mutableStateOf<Map<String, UserProfile>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var processingRequests by remember { mutableStateOf<Set<String>>(emptySet()) }
    var availableSeats by remember { mutableStateOf(0) }
    
    // Load ride requests for this carpool
    LaunchedEffect(carpoolId) {
        isLoading = true
        scope.launch {
            // Get ride requests specifically for THIS carpool (carpoolId must match)
            // DO NOT include requests with empty carpoolId - those are direct driver requests
            socialService.getRideRequests().onSuccess { allRequests ->
                rideRequests = allRequests.filter { it.carpoolId == carpoolId }
                android.util.Log.d("CarpoolRequests", "Filtering for carpoolId=$carpoolId")
                android.util.Log.d("CarpoolRequests", "Total requests: ${allRequests.size}, carpool requests: ${rideRequests.size}")
                
                android.util.Log.d("CarpoolRequests", "Found ${rideRequests.size} requests for carpool $carpoolId")
                
                // Load profiles for all requesters
                rideRequests.forEach { request ->
                    if (!requesterProfiles.containsKey(request.requesterId)) {
                        socialService.getUserProfile(request.requesterId).onSuccess { profile ->
                            requesterProfiles = requesterProfiles + (request.requesterId to profile)
                        }
                    }
                }
                
                isLoading = false
            }.onFailure { error ->
                android.util.Log.e("CarpoolRequests", "Failed to load requests: ${error.message}")
                isLoading = false
            }
        }
    }
    
    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = { Text("Ride Requests") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF1C1C1E)
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF007AFF))
            }
        } else if (rideRequests.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = Color(0xFFE5E5EA)
                    )
                    Text(
                        text = "No Ride Requests",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1C1E)
                    )
                    Text(
                        text = "Requests for your carpool will appear here",
                        fontSize = 15.sp,
                        color = Color(0xFF8E8E93)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F8FF)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF007AFF)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${rideRequests.size} Total Requests",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1C1C1E)
                                )
                                val pending = rideRequests.count { it.status == "pending" }
                                if (pending > 0) {
                                    Text(
                                        text = "$pending pending review",
                                        fontSize = 13.sp,
                                        color = Color(0xFF8E8E93)
                                    )
                                }
                            }
                        }
                    }
                }
                
                items(rideRequests) { request ->
                    val requesterProfile = requesterProfiles[request.requesterId]
                    val isProcessing = processingRequests.contains(request.id)
                    
                    RideRequestCard(
                        request = request,
                        requesterProfile = requesterProfile,
                        isProcessing = isProcessing,
                        onAccept = {
                            android.util.Log.d("CarpoolRequests", "Accept clicked for request: ${request.id}")
                            if (request.id.isEmpty()) {
                                android.util.Log.e("CarpoolRequests", "Request ID is empty!")
                                return@RideRequestCard
                            }
                            processingRequests = processingRequests + request.id
                            scope.launch {
                                try {
                                    android.util.Log.d("CarpoolRequests", "Calling updateRideRequestStatus...")
                                    val result = socialService.updateRideRequestStatus(request.id, "accepted")
                                    android.util.Log.d("CarpoolRequests", "Update result: ${result.isSuccess}")
                                    if (result.isSuccess) {
                                        android.util.Log.d("CarpoolRequests", "Successfully accepted, updating UI")
                                        rideRequests = rideRequests.map { r ->
                                            if (r.id == request.id) {
                                                android.util.Log.d("CarpoolRequests", "Updating request ${r.id} status to accepted")
                                                r.copy(status = "accepted")
                                            } else r
                                        }
                                    } else {
                                        android.util.Log.e("CarpoolRequests", "Update failed: ${result.exceptionOrNull()?.message}")
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("CarpoolRequests", "Exception: ${e.message}", e)
                                } finally {
                                    processingRequests = processingRequests - request.id
                                }
                            }
                        },
                        onDecline = {
                            android.util.Log.d("CarpoolRequests", "Decline clicked for request: ${request.id}")
                            if (request.id.isEmpty()) {
                                android.util.Log.e("CarpoolRequests", "Request ID is empty!")
                                return@RideRequestCard
                            }
                            processingRequests = processingRequests + request.id
                            scope.launch {
                                try {
                                    android.util.Log.d("CarpoolRequests", "Calling updateRideRequestStatus for decline...")
                                    val result = socialService.updateRideRequestStatus(request.id, "declined")
                                    android.util.Log.d("CarpoolRequests", "Decline result: ${result.isSuccess}")
                                    if (result.isSuccess) {
                                        android.util.Log.d("CarpoolRequests", "Successfully declined, updating UI")
                                        rideRequests = rideRequests.map { r ->
                                            if (r.id == request.id) {
                                                android.util.Log.d("CarpoolRequests", "Updating request ${r.id} status to declined")
                                                r.copy(status = "declined")
                                            } else r
                                        }
                                    } else {
                                        android.util.Log.e("CarpoolRequests", "Decline failed: ${result.exceptionOrNull()?.message}")
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("CarpoolRequests", "Exception: ${e.message}", e)
                                } finally {
                                    processingRequests = processingRequests - request.id
                                }
                            }
                        },
                        onViewProfile = { onViewProfile(request.requesterId) }
                    )
                }
            }
        }
    }
}

@Composable
fun RideRequestCard(
    request: RideRequest,
    requesterProfile: UserProfile?,
    isProcessing: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onViewProfile: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isProcessing) { onViewProfile() },
        colors = CardDefaults.cardColors(
            containerColor = when (request.status) {
                "accepted" -> Color(0xFFF0FFF4)
                "declined" -> Color(0xFFFFF5F5)
                else -> Color.White
            }
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with profile and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF007AFF),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = request.requesterName.firstOrNull()?.uppercase() ?: "?",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = request.requesterName,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1C1E)
                        )
                        
                        requesterProfile?.let { profile ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFCC00),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${profile.stats.drivingScore.toInt()} score",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1C1C1E)
                                )
                                Text(
                                    text = "· ${profile.stats.totalRides} rides",
                                    fontSize = 13.sp,
                                    color = Color(0xFF8E8E93)
                                )
                            }
                            
                            if (profile.stats.averageRating > 0) {
                                Text(
                                    text = "${String.format("%.1f", profile.stats.averageRating)}★ rating",
                                    fontSize = 13.sp,
                                    color = Color(0xFF8E8E93)
                                )
                            }
                        }
                    }
                }
                
                // Status badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (request.status) {
                        "accepted" -> Color(0xFF34C759).copy(alpha = 0.15f)
                        "declined" -> Color(0xFFFF3B30).copy(alpha = 0.15f)
                        else -> Color(0xFFFFCC00).copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        text = request.status.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (request.status) {
                            "accepted" -> Color(0xFF34C759)
                            "declined" -> Color(0xFFFF3B30)
                            else -> Color(0xFFFF9500)
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            
            HorizontalDivider()
            
            // Request details
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF007AFF),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Pickup: ${request.pickupLocation}",
                        fontSize = 14.sp,
                        color = Color(0xFF1C1C1E)
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF8E8E93),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "${request.passengers} passenger${if (request.passengers > 1) "s" else ""}",
                        fontSize = 14.sp,
                        color = Color(0xFF8E8E93)
                    )
                }
                
                if (request.notes.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = null,
                            tint = Color(0xFF8E8E93),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = request.notes,
                            fontSize = 14.sp,
                            color = Color(0xFF1C1C1E),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // Time ago
                val timeAgo = remember(request.timestamp) {
                    val now = System.currentTimeMillis()
                    val diff = now - request.timestamp
                    when {
                        diff < 60_000 -> "Just now"
                        diff < 3600_000 -> "${diff / 60_000}m ago"
                        diff < 86400_000 -> "${diff / 3600_000}h ago"
                        else -> "${diff / 86400_000}d ago"
                    }
                }
                Text(
                    text = "Requested $timeAgo",
                    fontSize = 12.sp,
                    color = Color(0xFF8E8E93)
                )
            }
            
            // Action buttons (only show for pending requests)
            if (request.status == "pending") {
                HorizontalDivider()
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onAccept,
                        enabled = !isProcessing,
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF34C759),
                            disabledContainerColor = Color(0xFF34C759).copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Accept",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    
                    Button(
                        onClick = onDecline,
                        enabled = !isProcessing,
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF3B30),
                            disabledContainerColor = Color(0xFFFF3B30).copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Decline",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
