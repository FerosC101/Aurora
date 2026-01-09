package com.nextcs.aurora.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.window.Dialog
import com.nextcs.aurora.social.Friend
import com.nextcs.aurora.social.FriendLocation
import com.nextcs.aurora.social.FriendLocationSharingService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Carpool data models
data class CarpoolListing(
    val id: String = "",
    val driverId: String = "",
    val driverName: String = "",
    val origin: String = "",
    val destination: String = "",
    val departureTime: Long = 0,
    val availableSeats: Int = 0,
    val pricePerSeat: Double = 0.0,
    val vehicleModel: String = "",
    val preferences: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class RideRequest(
    val id: String = "",
    val requesterId: String = "",
    val requesterName: String = "",
    val pickupLocation: String = "",
    val dropoffLocation: String = "",
    val requestedTime: Long = 0,
    val passengers: Int = 1,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun SocialScreen(
    userName: String,
    userEmail: String,
    onNavigateToFriend: (FriendLocation) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val friendService = remember { FriendLocationSharingService(context) }
    val scope = rememberCoroutineScope()
    
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Friends", "Carpool", "Drivers")
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F7))
    ) {
        // Modern Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Social",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1C1E),
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Friends, carpool & ride sharing",
                    fontSize = 15.sp,
                    color = Color(0xFF8E8E93)
                )
            }
        }
        
        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = Color(0xFF007AFF)
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 15.sp,
                            fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Medium
                        )
                    }
                )
            }
        }
        
        // Content
        when (selectedTab) {
            0 -> FriendsTab(friendService, onNavigateToFriend)
            1 -> CarpoolTab()
            2 -> DriversTab()
        }
    }
}

@Composable
fun FriendsTab(
    friendService: FriendLocationSharingService,
    onNavigateToFriend: (FriendLocation) -> Unit
) {
    val scope = rememberCoroutineScope()
    var friends by remember { mutableStateOf<List<Friend>>(emptyList()) }
    var friendLocations by remember { mutableStateOf<List<FriendLocation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    
    // Load friends
    LaunchedEffect(Unit) {
        scope.launch {
            friendService.getFriends().onSuccess { friendList ->
                friends = friendList
                isLoading = false
            }
        }
    }
    
    // Monitor friend locations
    LaunchedEffect(friends) {
        if (friends.isNotEmpty()) {
            val friendIds = friends.map { it.userId }
            // TODO: Implement observeFriendLocations in service
            // friendService.observeFriendLocations(friendIds)
            //     .collect { locations ->
            //         friendLocations = locations
            //     }
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color(0xFF007AFF)
            )
        } else if (friends.isEmpty()) {
            EmptyFriendsState(modifier = Modifier.align(Alignment.Center))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(friends) { friend ->
                    val location = friendLocations.find { it.userId == friend.userId }
                    FriendCard(
                        friend = friend,
                        location = location,
                        onNavigate = { location?.let { onNavigateToFriend(it) } },
                        onShareTrip = { /* TODO */ }
                    )
                }
            }
        }
        
        // FAB
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = Color(0xFF007AFF),
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Friend")
        }
    }
    
    if (showAddDialog) {
        SocialAddFriendDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { userId, name, email ->
                scope.launch {
                    friendService.addFriend(userId, name, email).onSuccess {
                        friendService.getFriends().onSuccess { friendList ->
                            friends = friendList
                        }
                        showAddDialog = false
                    }
                }
            }
        )
    }
}

@Composable
fun FriendCard(
    friend: Friend,
    location: FriendLocation?,
    onNavigate: () -> Unit,
    onShareTrip: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Avatar
                    Box {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF007AFF),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = friend.displayName.firstOrNull()?.uppercase() ?: "?",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        // Online indicator
                        if (location?.isSharing == true) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF34C759),
                                modifier = Modifier
                                    .size(16.dp)
                                    .align(Alignment.BottomEnd)
                            ) {}
                        }
                    }
                    
                    Column {
                        Text(
                            text = friend.displayName,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1C1C1E)
                        )
                        
                        if (location != null && location.isSharing) {
                            if (location.destination != null) {
                                Text(
                                    text = "→ ${location.destination}",
                                    fontSize = 15.sp,
                                    color = Color(0xFF007AFF)
                                )
                                location.eta?.let { eta ->
                                    Text(
                                        text = "ETA: $eta",
                                        fontSize = 13.sp,
                                        color = Color(0xFF8E8E93)
                                    )
                                }
                            } else {
                                Text(
                                    text = "Sharing location",
                                    fontSize = 15.sp,
                                    color = Color(0xFF34C759)
                                )
                            }
                        } else {
                            Text(
                                text = "Offline",
                                fontSize = 15.sp,
                                color = Color(0xFF8E8E93)
                            )
                        }
                    }
                }
            }
            
            // Quick actions
            if (location != null && location.isSharing) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigate,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF007AFF)
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            width = 1.5.dp,
                            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF007AFF))
                        )
                    ) {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Navigate", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                    
                    OutlinedButton(
                        onClick = onShareTrip,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF8E8E93)
                        )
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
fun CarpoolTab() {
    var showCreateDialog by remember { mutableStateOf(false) }
    var carpoolListings by remember { mutableStateOf<List<CarpoolListing>>(emptyList()) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (carpoolListings.isEmpty()) {
            EmptyCarpoolState(modifier = Modifier.align(Alignment.Center))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(carpoolListings) { listing ->
                    CarpoolCard(listing)
                }
            }
        }
        
        FloatingActionButton(
            onClick = { showCreateDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = Color(0xFF007AFF),
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Create Carpool")
        }
    }
    
    if (showCreateDialog) {
        CreateCarpoolDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { listing ->
                carpoolListings = carpoolListings + listing
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun CarpoolCard(listing: CarpoolListing) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = listing.driverName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1C1C1E)
                    )
                    Text(
                        text = listing.vehicleModel,
                        fontSize = 13.sp,
                        color = Color(0xFF8E8E93)
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFE3F2FD)
                ) {
                    Text(
                        text = "$${listing.pricePerSeat}/seat",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF007AFF),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            
            // Route
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Place,
                    contentDescription = null,
                    tint = Color(0xFF34C759),
                    modifier = Modifier.size(20.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = listing.origin,
                        fontSize = 15.sp,
                        color = Color(0xFF1C1C1E)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color(0xFF8E8E93),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = listing.destination,
                        fontSize = 15.sp,
                        color = Color(0xFF1C1C1E)
                    )
                }
            }
            
            // Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        tint = Color(0xFF8E8E93),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
                            .format(Date(listing.departureTime)),
                        fontSize = 13.sp,
                        color = Color(0xFF8E8E93)
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF8E8E93),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${listing.availableSeats} seats",
                        fontSize = 13.sp,
                        color = Color(0xFF8E8E93)
                    )
                }
            }
            
            Button(
                onClick = { /* TODO: Request ride */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF007AFF)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Request Ride",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun DriversTab() {
    var showOfferDialog by remember { mutableStateOf(false) }
    var rideRequests by remember { mutableStateOf<List<RideRequest>>(emptyList()) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (rideRequests.isEmpty()) {
            EmptyDriverState(modifier = Modifier.align(Alignment.Center))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(rideRequests) { request ->
                    RideRequestCard(request)
                }
            }
        }
        
        FloatingActionButton(
            onClick = { showOfferDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = Color(0xFF007AFF),
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Offer Ride")
        }
    }
}

@Composable
fun RideRequestCard(request: RideRequest) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF007AFF),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = request.requesterName.firstOrNull()?.uppercase() ?: "?",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Column {
                        Text(
                            text = request.requesterName,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1C1C1E)
                        )
                        Text(
                            text = "${request.passengers} passenger${if (request.passengers > 1) "s" else ""}",
                            fontSize = 13.sp,
                            color = Color(0xFF8E8E93)
                        )
                    }
                }
            }
            
            // Locations
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFF34C759),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = request.pickupLocation,
                        fontSize = 15.sp,
                        color = Color(0xFF1C1C1E)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = null,
                        tint = Color(0xFFFF3B30),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = request.dropoffLocation,
                        fontSize = 15.sp,
                        color = Color(0xFF1C1C1E)
                    )
                }
            }
            
            if (request.notes.isNotEmpty()) {
                Text(
                    text = request.notes,
                    fontSize = 13.sp,
                    color = Color(0xFF8E8E93),
                    lineHeight = 18.sp
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { /* TODO: Decline */ },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF8E8E93)
                    )
                ) {
                    Text("Decline", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
                
                Button(
                    onClick = { /* TODO: Accept */ },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF007AFF)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Accept", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun EmptyFriendsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Default.Person,
            contentDescription = null,
            tint = Color(0xFFD1D1D6),
            modifier = Modifier.size(72.dp)
        )
        Text(
            text = "No friends yet",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1C1C1E)
        )
        Text(
            text = "Add friends to share locations\nand coordinate trips together",
            fontSize = 15.sp,
            color = Color(0xFF8E8E93),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun EmptyCarpoolState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Default.Person,
            contentDescription = null,
            tint = Color(0xFFD1D1D6),
            modifier = Modifier.size(72.dp)
        )
        Text(
            text = "No carpool listings",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1C1C1E)
        )
        Text(
            text = "Share rides and split costs\nwith other travelers",
            fontSize = 15.sp,
            color = Color(0xFF8E8E93),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun EmptyDriverState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Default.Person,
            contentDescription = null,
            tint = Color(0xFFD1D1D6),
            modifier = Modifier.size(72.dp)
        )
        Text(
            text = "No ride requests",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1C1C1E)
        )
        Text(
            text = "Check back later for passengers\nlooking for rides",
            fontSize = 15.sp,
            color = Color(0xFF8E8E93),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun SocialAddFriendDialog(
    onDismiss: () -> Unit,
    onAdd: (userId: String, name: String, email: String) -> Unit
) {
    var userId by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Add Friend",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1C1C1E),
                    letterSpacing = (-0.5).sp
                )
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Friend's Name", fontSize = 15.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF007AFF),
                        focusedLabelColor = Color(0xFF007AFF)
                    )
                )
                
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email", fontSize = 15.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF007AFF),
                        focusedLabelColor = Color(0xFF007AFF)
                    )
                )
                
                OutlinedTextField(
                    value = userId,
                    onValueChange = { userId = it },
                    label = { Text("User ID", fontSize = 15.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF007AFF),
                        focusedLabelColor = Color(0xFF007AFF)
                    )
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            "Cancel",
                            fontSize = 17.sp,
                            color = Color(0xFF8E8E93),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (userId.isNotBlank() && name.isNotBlank() && email.isNotBlank()) {
                                onAdd(userId, name, email)
                            }
                        },
                        enabled = userId.isNotBlank() && name.isNotBlank() && email.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF007AFF),
                            disabledContainerColor = Color(0xFFD1D1D6)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Add",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CreateCarpoolDialog(
    onDismiss: () -> Unit,
    onCreate: (CarpoolListing) -> Unit
) {
    var origin by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var seats by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var vehicle by remember { mutableStateOf("") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Create Carpool",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1C1C1E),
                    letterSpacing = (-0.5).sp
                )
                
                OutlinedTextField(
                    value = origin,
                    onValueChange = { origin = it },
                    label = { Text("From", fontSize = 15.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF007AFF),
                        focusedLabelColor = Color(0xFF007AFF)
                    )
                )
                
                OutlinedTextField(
                    value = destination,
                    onValueChange = { destination = it },
                    label = { Text("To", fontSize = 15.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF007AFF),
                        focusedLabelColor = Color(0xFF007AFF)
                    )
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = seats,
                        onValueChange = { seats = it },
                        label = { Text("Seats", fontSize = 15.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF007AFF),
                            focusedLabelColor = Color(0xFF007AFF)
                        )
                    )
                    
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Price", fontSize = 15.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF007AFF),
                            focusedLabelColor = Color(0xFF007AFF)
                        )
                    )
                }
                
                OutlinedTextField(
                    value = vehicle,
                    onValueChange = { vehicle = it },
                    label = { Text("Vehicle", fontSize = 15.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF007AFF),
                        focusedLabelColor = Color(0xFF007AFF)
                    )
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            "Cancel",
                            fontSize = 17.sp,
                            color = Color(0xFF8E8E93),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onCreate(
                                CarpoolListing(
                                    id = UUID.randomUUID().toString(),
                                    driverId = "current_user",
                                    driverName = "You",
                                    origin = origin,
                                    destination = destination,
                                    departureTime = System.currentTimeMillis(),
                                    availableSeats = seats.toIntOrNull() ?: 0,
                                    pricePerSeat = price.toDoubleOrNull() ?: 0.0,
                                    vehicleModel = vehicle,
                                    preferences = ""
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF007AFF)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Create",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
