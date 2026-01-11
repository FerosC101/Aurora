package com.nextcs.aurora.ui.screens

import androidx.compose.foundation.BorderStroke
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
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SocialScreen(
    userName: String,
    userEmail: String,
    onNavigateToFriend: (FriendLocation) -> Unit,
    onNavigateToChat: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val friendService = remember { FriendLocationSharingService(context) }
    val socialService = remember { SocialFirebaseService(context) }
    val scope = rememberCoroutineScope()
    
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Friends", "Requests", "Carpool", "Drivers")
    
    // State for navigation to friend profile
    var selectedFriendId by remember { mutableStateOf<String?>(null) }
    
    // Show friend profile if selected
    selectedFriendId?.let { friendId ->
        FriendProfileScreen(
            friendId = friendId,
            onBack = { selectedFriendId = null },
            onMessage = {
                selectedFriendId = null
                onNavigateToChat()
            }
        )
        return
    }
    
    // Initialize user profile on launch
    LaunchedEffect(Unit) {
        scope.launch {
            android.util.Log.d("SocialScreen", "========== INITIALIZING SOCIAL SCREEN ==========")
            
            // Step 1: Initialize current user profile
            android.util.Log.d("SocialScreen", "Step 1: Initializing user profile...")
            socialService.getCurrentUserProfile()
                .onSuccess { profile ->
                    android.util.Log.d("SocialScreen", "✅ User profile initialized: $profile")
                }
                .onFailure { error ->
                    android.util.Log.e("SocialScreen", "❌ Failed to initialize profile: ${error.message}", error)
                }
            
            // Step 2: Test direct Firestore access
            android.util.Log.d("SocialScreen", "Step 2: Testing direct Firestore access...")
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance(
                    com.google.firebase.FirebaseApp.getInstance(), 
                    "sfse"
                )
                android.util.Log.d("SocialScreen", "✅ Got Firestore instance for sfse database")
                
                val snapshot = db.collection("users").get().await()
                android.util.Log.d("SocialScreen", "✅ Direct query successful!")
                android.util.Log.d("SocialScreen", "📊 Total documents: ${snapshot.documents.size}")
                snapshot.documents.forEach { doc ->
                    android.util.Log.d("SocialScreen", "  📄 Document ID: ${doc.id}")
                    android.util.Log.d("SocialScreen", "     Data: ${doc.data}")
                }
            } catch (e: Exception) {
                android.util.Log.e("SocialScreen", "❌ Direct Firestore query failed: ${e.message}", e)
            }
            
            // Step 3: Try to fetch all users using service
            android.util.Log.d("SocialScreen", "Step 3: Fetching users via service...")
            socialService.searchUsers("").onSuccess { users ->
                android.util.Log.d("SocialScreen", "🔍 Total users from service: ${users.size}")
                users.forEach { user ->
                    android.util.Log.d("SocialScreen", "  - User: ${user.displayName} (${user.email})")
                }
            }.onFailure { error ->
                android.util.Log.e("SocialScreen", "❌ Failed to fetch users: ${error.message}", error)
            }
            
            android.util.Log.d("SocialScreen", "================================================")
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F7))
    ) {
        // Modern Header with Actions
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
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
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Notifications Icon
                        IconButton(onClick = onNavigateToNotifications) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = Color(0xFF007AFF)
                            )
                        }
                        
                        // Chat Icon
                        IconButton(onClick = onNavigateToChat) {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = "Messages",
                                tint = Color(0xFF007AFF)
                            )
                        }
                    }
                }
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
            0 -> FriendsTab(socialService, onNavigateToFriend, onClickProfile = { friendId -> selectedFriendId = friendId })
            1 -> FriendRequestsTab(socialService, onClickProfile = { friendId -> selectedFriendId = friendId })
            2 -> CarpoolTab(socialService)
            3 -> DriversTab(socialService)
        }
    }
    
    // Navigate to friend profile
    selectedFriendId?.let { friendId ->
        FriendProfileScreen(
            friendId = friendId,
            onBack = { selectedFriendId = null },
            onMessage = {
                selectedFriendId = null
                onNavigateToChat()
            }
        )
    }
}

@Composable
fun FriendsTab(
    socialService: SocialFirebaseService,
    onNavigateToFriend: (FriendLocation) -> Unit,
    onClickProfile: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var friends by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var friendRequests by remember { mutableStateOf<List<com.nextcs.aurora.social.FriendRequest>>(emptyList()) }
    var friendLocations by remember { mutableStateOf<List<FriendLocation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    val currentUserId = socialService.getCurrentUserId()
    
    // Load friends - refresh when user ID changes
    LaunchedEffect(currentUserId) {
        isLoading = true
        scope.launch {
            android.util.Log.d("SocialScreen", "Loading friends for user: $currentUserId")
            socialService.getFriends().onSuccess { friendList ->
                friends = friendList
                android.util.Log.d("SocialScreen", "Loaded ${friendList.size} friends")
                isLoading = false
            }.onFailure {
                android.util.Log.e("SocialScreen", "Failed to load friends", it)
                isLoading = false
            }
        }
    }
    
    // Observe friend requests (with error handling for missing security rules)
    LaunchedEffect(currentUserId) {
        try {
            socialService.observePendingRequests().collect { requests ->
                friendRequests = requests
                android.util.Log.d("SocialScreen", "Received ${requests.size} pending friend requests")
            }
        } catch (e: Exception) {
            android.util.Log.w("SocialScreen", "Friend requests not available (missing security rules?): ${e.message}")
            // Continue without friend requests - app should still work
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
        } else if (friends.isEmpty() && friendRequests.isEmpty()) {
            EmptyFriendsState(modifier = Modifier.align(Alignment.Center))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Friend Requests Section
                if (friendRequests.isNotEmpty()) {
                    item {
                        Text(
                            text = "Friend Requests (${friendRequests.size})",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1C1C1E),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    items(friendRequests) { request ->
                        FriendRequestCard(
                            request = request,
                            onAccept = {
                                scope.launch {
                                    socialService.acceptFriendRequest(request.id).onSuccess {
                                        // Refresh friends list
                                        socialService.getFriends().onSuccess { friendList ->
                                            friends = friendList
                                        }
                                    }
                                }
                            },
                            onDecline = {
                                scope.launch {
                                    socialService.rejectFriendRequest(request.id)
                                }
                            }
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                
                // Friends Section
                if (friends.isNotEmpty()) {
                    item {
                        Text(
                            text = "My Friends (${friends.size})",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1C1C1E),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
                
                items(friends) { friend ->
                    val location = friendLocations.find { it.userId == friend.userId }
                    FriendCard(
                        friend = friend,
                        location = location,
                        onNavigate = { location?.let { onNavigateToFriend(it) } },
                        onShareTrip = { /* TODO */ },
                        onClickProfile = { onClickProfile(friend.userId) }
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
            onAdd = { userId, friendName, friendEmail ->
                scope.launch {
                    // Send friend request instead of directly adding
                    socialService.sendFriendRequest(userId, friendName, friendEmail).onSuccess {
                        showAddDialog = false
                    }.onFailure { error ->
                        android.util.Log.e("FriendsTab", "Failed to send friend request: ${error.message}")
                    }
                }
            }
        )
    }
}

@Composable
fun FriendRequestCard(
    request: com.nextcs.aurora.social.FriendRequest,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F8FF)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF007AFF),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = request.fromUserName.firstOrNull()?.uppercase() ?: "?",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Column {
                    Text(
                        text = request.fromUserName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1C1C1E)
                    )
                    Text(
                        text = request.fromUserEmail,
                        fontSize = 13.sp,
                        color = Color(0xFF8E8E93)
                    )
                    Text(
                        text = "Wants to be friends",
                        fontSize = 12.sp,
                        color = Color(0xFF007AFF),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF34C759)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Accept", fontSize = 13.sp)
                }
                
                OutlinedButton(
                    onClick = onDecline,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFFF3B30)
                    )
                ) {
                    Text("Decline", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun FriendCard(
    friend: UserProfile,
    location: FriendLocation?,
    onNavigate: () -> Unit,
    onShareTrip: () -> Unit,
    onClickProfile: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClickProfile() },
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
    val currentUserId = socialService.getCurrentUserId()
    
    // Load carpool listings - refresh when user ID changes
    LaunchedEffect(currentUserId) {
        isLoading = true
        scope.launch {
            android.util.Log.d("SocialScreen", "Loading carpool listings for user: $currentUserId")
            socialService.getCarpoolListings().onSuccess { listings ->
                carpoolListings = listings
                android.util.Log.d("SocialScreen", "Loaded ${listings.size} carpool listings")
                isLoading = false
            }.onFailure {
                android.util.Log.e("SocialScreen", "Failed to load carpools", it)
                isLoading = false
            }
        }
    }
    
    // Observe real-time updates - restart when user changes
    LaunchedEffect(currentUserId) {
        socialService.observeCarpoolListings().collect { listings ->
            carpoolListings = listings
            android.util.Log.d("SocialScreen", "Real-time update: ${listings.size} carpools")
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
                    android.util.Log.d("SocialScreen", "Creating carpool listing: $listing")
                    socialService.createCarpoolListing(listing).onSuccess {
                        android.util.Log.d("SocialScreen", "Carpool created successfully: $it")
                        showCreateDialog = false
                        // List will update automatically via observer
                    }.onFailure { error ->
                        android.util.Log.e("SocialScreen", "Failed to create carpool: ${error.message}", error)
                        // TODO: Show error to user
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
                            .format(Date(listing.departureTimestamp)),
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
    val currentUserId = socialService.getCurrentUserId()
    
    // Load ride requests - refresh when user ID changes (excludes own requests)
    LaunchedEffect(currentUserId) {
        isLoading = true
        scope.launch {
            android.util.Log.d("SocialScreen", "Loading ride requests for user: $currentUserId")
            socialService.getRideRequests().onSuccess { requests ->
                rideRequests = requests
                android.util.Log.d("SocialScreen", "Loaded ${requests.size} ride requests (excluding own)")
                isLoading = false
            }.onFailure {
                android.util.Log.e("SocialScreen", "Failed to load ride requests", it)
                isLoading = false
            }
        }
    }
    
    // Observe real-time updates - restart when user changes
    LaunchedEffect(currentUserId) {
        socialService.observeRideRequests().collect { requests ->
            rideRequests = requests
            android.util.Log.d("SocialScreen", "Real-time update: ${requests.size} ride requests")
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
                    android.util.Log.d("SocialScreen", "Creating ride request: $request")
                    socialService.createRideRequest(request).onSuccess {
                        android.util.Log.d("SocialScreen", "Ride request created successfully: $it")
                        showOfferDialog = false
                    }.onFailure { error ->
                        android.util.Log.e("SocialScreen", "Failed to create ride request: ${error.message}", error)
                        // TODO: Show error to user
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
fun FriendRequestsTab(
    socialService: SocialFirebaseService,
    onClickProfile: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var friendRequests by remember { mutableStateOf<List<com.nextcs.aurora.social.FriendRequest>>(emptyList()) }
    var requestProfiles by remember { mutableStateOf<Map<String, UserProfile>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var processingRequests by remember { mutableStateOf<Set<String>>(emptySet()) }
    val currentUserId = socialService.getCurrentUserId()
    
    // Observe friend requests
    LaunchedEffect(currentUserId) {
        isLoading = true
        try {
            socialService.observePendingRequests().collect { requests ->
                friendRequests = requests
                android.util.Log.d("FriendRequestsTab", "Received ${requests.size} pending friend requests")
                
                // Load profiles for all request senders
                requests.forEach { request ->
                    if (!requestProfiles.containsKey(request.fromUserId)) {
                        scope.launch {
                            socialService.getUserProfile(request.fromUserId).onSuccess { profile ->
                                requestProfiles = requestProfiles + (request.fromUserId to profile)
                            }
                        }
                    }
                }
                isLoading = false
            }
        } catch (e: Exception) {
            android.util.Log.w("FriendRequestsTab", "Friend requests not available: ${e.message}")
            isLoading = false
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color(0xFF007AFF)
            )
        } else if (friendRequests.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = Color(0xFFE5E5EA)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No pending friend requests",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1C1C1E)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "New friend requests will appear here",
                    fontSize = 15.sp,
                    color = Color(0xFF8E8E93),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Friend Requests (${friendRequests.size})",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1C1E),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                items(friendRequests) { request ->
                    val senderProfile = requestProfiles[request.fromUserId]
                    val isProcessing = processingRequests.contains(request.id)
                    
                    EnhancedFriendRequestCard(
                        request = request,
                        senderProfile = senderProfile,
                        isProcessing = isProcessing,
                        onAccept = {
                            processingRequests = processingRequests + request.id
                            scope.launch {
                                socialService.acceptFriendRequest(request.id).onSuccess {
                                    android.util.Log.d("FriendRequestsTab", "Accepted friend request from ${request.fromUserName}")
                                    processingRequests = processingRequests - request.id
                                }.onFailure { error ->
                                    android.util.Log.e("FriendRequestsTab", "Failed to accept request: ${error.message}")
                                    processingRequests = processingRequests - request.id
                                }
                            }
                        },
                        onDecline = {
                            processingRequests = processingRequests + request.id
                            scope.launch {
                                socialService.rejectFriendRequest(request.id).onSuccess {
                                    android.util.Log.d("FriendRequestsTab", "Declined friend request from ${request.fromUserName}")
                                    processingRequests = processingRequests - request.id
                                }.onFailure { error ->
                                    android.util.Log.e("FriendRequestsTab", "Failed to decline request: ${error.message}")
                                    processingRequests = processingRequests - request.id
                                }
                            }
                        },
                        onClickProfile = { onClickProfile(request.fromUserId) }
                    )
                }
            }
        }
    }
}

@Composable
fun EnhancedFriendRequestCard(
    request: com.nextcs.aurora.social.FriendRequest,
    senderProfile: UserProfile?,
    isProcessing: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onClickProfile: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isProcessing) { onClickProfile() },
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
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Profile photo
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF007AFF),
                        modifier = Modifier.size(60.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = request.fromUserName.firstOrNull()?.uppercase() ?: "?",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = request.fromUserName,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1C1E)
                        )
                        
                        senderProfile?.let { profile ->
                            // Driving score badge
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when {
                                        profile.stats.drivingScore >= 80 -> Color(0xFF34C759)
                                        profile.stats.drivingScore >= 60 -> Color(0xFFFFCC00)
                                        else -> Color(0xFFFF9500)
                                    }.copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = null,
                                            tint = when {
                                                profile.stats.drivingScore >= 80 -> Color(0xFF34C759)
                                                profile.stats.drivingScore >= 60 -> Color(0xFFFFCC00)
                                                else -> Color(0xFFFF9500)
                                            },
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "${profile.stats.drivingScore.toInt()}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1C1C1E)
                                        )
                                    }
                                }
                                
                                Text(
                                    text = "Driving Score",
                                    fontSize = 12.sp,
                                    color = Color(0xFF8E8E93)
                                )
                            }
                            
                            // Quick stats
                            if (profile.stats.totalRides > 0) {
                                Text(
                                    text = "${profile.stats.totalRides} rides · ${String.format("%.1f", profile.stats.averageRating)}★",
                                    fontSize = 13.sp,
                                    color = Color(0xFF8E8E93)
                                )
                            }
                        }
                        
                        Text(
                            text = "Wants to be friends",
                            fontSize = 13.sp,
                            color = Color(0xFF007AFF),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        
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
                            text = timeAgo,
                            fontSize = 12.sp,
                            color = Color(0xFF8E8E93),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAccept,
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f).height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF007AFF),
                        disabledContainerColor = Color(0xFF007AFF).copy(alpha = 0.5f)
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
                        Text(
                            text = "Accept",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                OutlinedButton(
                    onClick = onDecline,
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f).height(44.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFFF3B30)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFFF3B30)),
                    shape = RoundedCornerShape(12.dp)
                ) {
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

@Composable
fun SearchFriendDialog(
    socialService: SocialFirebaseService,
    onDismiss: () -> Unit,
    onAdd: (userId: String, friendName: String, friendEmail: String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var allUsers by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Load all users on launch
    LaunchedEffect(Unit) {
        android.util.Log.d("SearchFriendDialog", "🔵 Loading all users...")
        isSearching = true
        socialService.searchUsers("").onSuccess { users ->
            android.util.Log.d("SearchFriendDialog", "✅ Loaded ${users.size} users")
            users.forEach { user ->
                android.util.Log.d("SearchFriendDialog", "  - ${user.displayName} (${user.email})")
            }
            allUsers = users
            searchResults = users
            isSearching = false
        }.onFailure { error ->
            android.util.Log.e("SearchFriendDialog", "❌ Failed to load users: ${error.message}", error)
            isSearching = false
        }
    }
    
    // Debounced search
    LaunchedEffect(searchQuery) {
        android.util.Log.d("SearchFriendDialog", "🔍 Search query changed to: '$searchQuery'")
        if (searchQuery.length >= 2) {
            isSearching = true
            kotlinx.coroutines.delay(500) // Debounce
            android.util.Log.d("SearchFriendDialog", "🔵 Searching for: '$searchQuery'")
            socialService.searchUsers(searchQuery).onSuccess { users ->
                android.util.Log.d("SearchFriendDialog", "✅ Search found ${users.size} results")
                searchResults = users
                isSearching = false
            }.onFailure { error ->
                android.util.Log.e("SearchFriendDialog", "❌ Search failed: ${error.message}", error)
                isSearching = false
            }
        } else {
            // Show all users when search is cleared
            android.util.Log.d("SearchFriendDialog", "🔄 Showing all ${allUsers.size} users (query cleared)")
            searchResults = allUsers
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
                
                // Search results or all users
                if (isSearching) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF007AFF))
                    }
                } else if (searchResults.isEmpty()) {
                    Text(
                        text = if (searchQuery.isEmpty()) "No users available" else "No users found",
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
                                    .clickable { onAdd(user.userId, user.displayName, user.email) },
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
                            val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                            val currentTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                            onCreate(
                                RideRequest(
                                    pickupLocation = pickup,
                                    dropoffLocation = dropoff,
                                    passengers = passengers.toIntOrNull() ?: 1,
                                    notes = notes,
                                    requestedDate = currentDate,
                                    requestedTime = currentTime,
                                    requestedTimestamp = System.currentTimeMillis()
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
                            val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                            val currentTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                            onCreate(
                                CarpoolListing(
                                    origin = origin,
                                    destination = destination,
                                    departureDate = currentDate,
                                    departureTime = currentTime,
                                    departureTimestamp = System.currentTimeMillis(),
                                    availableSeats = seats.toIntOrNull() ?: 0,
                                    pricePerSeat = price.toDoubleOrNull() ?: 0.0,
                                    vehicleModel = vehicle,
                                    preferences = ""
                                )
                            )
                        },
                        enabled = origin.isNotBlank() && destination.isNotBlank() && seats.isNotBlank(),
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
