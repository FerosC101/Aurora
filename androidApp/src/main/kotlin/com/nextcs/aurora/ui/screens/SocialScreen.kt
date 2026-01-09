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
import com.nextcs.aurora.social.FriendLocation
import com.nextcs.aurora.social.FriendLocationSharingService
import com.nextcs.aurora.social.SocialFirebaseService
import com.nextcs.aurora.social.CarpoolListing
import com.nextcs.aurora.social.RideRequest
import com.nextcs.aurora.social.UserProfile
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SocialScreen(
    userName: String,
    userEmail: String,
    onNavigateToFriend: (FriendLocation) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val friendService = remember { FriendLocationSharingService(context) }
    val socialService = remember { SocialFirebaseService(context) }
    val scope = rememberCoroutineScope()
    
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Friends", "Carpool", "Drivers")
    
    // Initialize user profile on launch
    LaunchedEffect(Unit) {
        scope.launch {
            socialService.getCurrentUserProfile()
        }
    }
    
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
            0 -> FriendsTab(socialService, onNavigateToFriend)
            1 -> CarpoolTab(socialService)
            2 -> DriversTab(socialService)
        }
    }
}

@Composable
fun FriendsTab(
    socialService: SocialFirebaseService,
    onNavigateToFriend: (FriendLocation) -> Unit
) {
    val scope = rememberCoroutineScope()
    var friends by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var friendLocations by remember { mutableStateOf<List<FriendLocation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    
    // Load friends
    LaunchedEffect(Unit) {
        scope.launch {
            socialService.getFriends().onSuccess { friendList ->
                friends = friendList
                isLoading = false
            }.onFailure {
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
        SearchFriendDialog(
            socialService = socialService,
            onDismiss = { showAddDialog = false },
            onAdd = { userId ->
                scope.launch {
                    socialService.addFriend(userId).onSuccess {
                        // Refresh friends list
                        socialService.getFriends().onSuccess { friendList ->
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
    friend: UserProfile,
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
fun CarpoolTab(socialService: SocialFirebaseService) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var carpoolListings by remember { mutableStateOf<List<CarpoolListing>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    
    // Load carpool listings
    LaunchedEffect(Unit) {
        scope.launch {
            socialService.getCarpoolListings().onSuccess { listings ->
                carpoolListings = listings
                isLoading = false
            }.onFailure {
                isLoading = false
            }
        }
    }
    
    // Observe real-time updates
    LaunchedEffect(Unit) {
        socialService.observeCarpoolListings().collect { listings ->
            carpoolListings = listings
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color(0xFF007AFF)
            )
        } else if (carpoolListings.isEmpty()) {
            EmptyCarpoolState(modifier = Modifier.align(Alignment.Center))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(carpoolListings) { listing ->
                    CarpoolCard(
                        listing = listing,
                        socialService = socialService,
                        onDelete = {
                            scope.launch {
                                socialService.deleteCarpoolListing(listing.id).onSuccess {
                                    carpoolListings = carpoolListings.filter { it.id != listing.id }
                                }
                            }
                        }
                    )
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
                scope.launch {
                    socialService.createCarpoolListing(listing).onSuccess {
                        showCreateDialog = false
                        // List will update automatically via observer
                    }
                }
            }
        )
    }
}

@Composable
fun CarpoolCard(
    listing: CarpoolListing,
    socialService: SocialFirebaseService,
    onDelete: () -> Unit
) {
    val currentUserId = socialService.getCurrentUserId()
    val isOwnListing = listing.driverId == currentUserId
    
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
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    
                    if (isOwnListing) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color(0xFFFF3B30),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
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
                shape = RoundedCornerShape(12.dp),
                enabled = !isOwnListing
            ) {
                Text(
                    if (isOwnListing) "Your Listing" else "Request Ride",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun DriversTab(socialService: SocialFirebaseService) {
    var showOfferDialog by remember { mutableStateOf(false) }
    var rideRequests by remember { mutableStateOf<List<RideRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    
    // Load ride requests
    LaunchedEffect(Unit) {
        scope.launch {
            socialService.getRideRequests().onSuccess { requests ->
                rideRequests = requests
                isLoading = false
            }.onFailure {
                isLoading = false
            }
        }
    }
    
    // Observe real-time updates
    LaunchedEffect(Unit) {
        socialService.observeRideRequests().collect { requests ->
            rideRequests = requests
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color(0xFF007AFF)
            )
        } else if (rideRequests.isEmpty()) {
            EmptyDriverState(modifier = Modifier.align(Alignment.Center))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(rideRequests) { request ->
                    RideRequestCard(
                        request = request,
                        socialService = socialService,
                        onAccept = {
                            scope.launch {
                                socialService.updateRideRequestStatus(request.id, "accepted")
                            }
                        },
                        onDecline = {
                            scope.launch {
                                socialService.updateRideRequestStatus(request.id, "declined")
                            }
                        }
                    )
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
    
    if (showOfferDialog) {
        CreateRideRequestDialog(
            onDismiss = { showOfferDialog = false },
            onCreate = { request ->
                scope.launch {
                    socialService.createRideRequest(request).onSuccess {
                        showOfferDialog = false
                    }
                }
            }
        )
    }
}

@Composable
fun RideRequestCard(
    request: RideRequest,
    socialService: SocialFirebaseService,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val currentUserId = socialService.getCurrentUserId()
    val isOwnRequest = request.requesterId == currentUserId
    
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
            
            // Status badge
            if (request.status != "pending") {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (request.status) {
                        "accepted" -> Color(0xFFE3F2FD)
                        "declined" -> Color(0xFFFFEBEE)
                        else -> Color(0xFFF5F5F5)
                    }
                ) {
                    Text(
                        text = request.status.uppercase(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when (request.status) {
                            "accepted" -> Color(0xFF007AFF)
                            "declined" -> Color(0xFFFF3B30)
                            else -> Color(0xFF8E8E93)
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            
            if (!isOwnRequest && request.status == "pending") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDecline,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF8E8E93)
                        )
                    ) {
                        Text("Decline", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                    
                    Button(
                        onClick = onAccept,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF007AFF)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Accept", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else if (isOwnRequest) {
                Text(
                    text = "Your Request",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF8E8E93),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
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
fun SearchFriendDialog(
    socialService: SocialFirebaseService,
    onDismiss: () -> Unit,
    onAdd: (userId: String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Debounced search
    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            isSearching = true
            kotlinx.coroutines.delay(500) // Debounce
            socialService.searchUsers(searchQuery).onSuccess { users ->
                searchResults = users
                isSearching = false
            }.onFailure {
                isSearching = false
            }
        } else {
            searchResults = emptyList()
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Friend",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1C1C1E),
                        letterSpacing = (-0.5).sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF8E8E93)
                        )
                    }
                }
                
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search by name or email", fontSize = 15.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF8E8E93)
                        )
                    },
                    trailingIcon = {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF007AFF)
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF007AFF),
                        focusedLabelColor = Color(0xFF007AFF)
                    )
                )
                
                // Search results
                if (searchQuery.length < 2) {
                    Text(
                        text = "Type at least 2 characters to search",
                        fontSize = 13.sp,
                        color = Color(0xFF8E8E93),
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else if (searchResults.isEmpty() && !isSearching) {
                    Text(
                        text = "No users found",
                        fontSize = 15.sp,
                        color = Color(0xFF8E8E93),
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(searchResults) { user ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onAdd(user.userId) },
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF5F5F7)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF007AFF),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = user.displayName.firstOrNull()?.uppercase() ?: "?",
                                                color = Color.White,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = user.displayName,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF1C1C1E)
                                        )
                                        Text(
                                            text = user.email,
                                            fontSize = 13.sp,
                                            color = Color(0xFF8E8E93)
                                        )
                                    }
                                    
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Add",
                                        tint = Color(0xFF007AFF),
                                        modifier = Modifier.size(24.dp)
                                    )
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
fun CreateRideRequestDialog(
    onDismiss: () -> Unit,
    onCreate: (RideRequest) -> Unit
) {
    var pickup by remember { mutableStateOf("") }
    var dropoff by remember { mutableStateOf("") }
    var passengers by remember { mutableStateOf("1") }
    var notes by remember { mutableStateOf("") }
    
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
                    text = "Request a Ride",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1C1C1E),
                    letterSpacing = (-0.5).sp
                )
                
                OutlinedTextField(
                    value = pickup,
                    onValueChange = { pickup = it },
                    label = { Text("Pickup Location", fontSize = 15.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF007AFF),
                        focusedLabelColor = Color(0xFF007AFF)
                    )
                )
                
                OutlinedTextField(
                    value = dropoff,
                    onValueChange = { dropoff = it },
                    label = { Text("Dropoff Location", fontSize = 15.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF007AFF),
                        focusedLabelColor = Color(0xFF007AFF)
                    )
                )
                
                OutlinedTextField(
                    value = passengers,
                    onValueChange = { passengers = it },
                    label = { Text("Number of Passengers", fontSize = 15.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF007AFF),
                        focusedLabelColor = Color(0xFF007AFF)
                    )
                )
                
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)", fontSize = 15.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
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
                                RideRequest(
                                    id = UUID.randomUUID().toString(),
                                    pickupLocation = pickup,
                                    dropoffLocation = dropoff,
                                    passengers = passengers.toIntOrNull() ?: 1,
                                    notes = notes,
                                    requestedTime = System.currentTimeMillis()
                                )
                            )
                        },
                        enabled = pickup.isNotBlank() && dropoff.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF007AFF)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Request",
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
