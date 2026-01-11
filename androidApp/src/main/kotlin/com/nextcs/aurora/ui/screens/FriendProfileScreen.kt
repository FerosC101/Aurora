package com.nextcs.aurora.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextcs.aurora.social.SocialFirebaseService
import com.nextcs.aurora.social.UserProfile
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendProfileScreen(
    friendId: String,
    onBack: () -> Unit,
    onMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val socialService = remember { SocialFirebaseService(context) }
    val scope = rememberCoroutineScope()
    
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var mutualFriends by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(friendId) {
        scope.launch {
            socialService.getUserProfile(friendId).onSuccess {
                profile = it
            }
            // Load mutual friends
            socialService.getMutualFriends(friendId).onSuccess {
                mutualFriends = it
            }
            isLoading = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
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
        } else {
            profile?.let { user ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(Color(0xFFF5F5F7)),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    // Profile Header
                    item {
                        ProfileHeader(user, onMessage)
                    }
                    
                    // Driving Score
                    item {
                        DrivingScoreCard(user.stats)
                    }
                    
                    // Statistics
                    item {
                        StatisticsCard(user.stats)
                    }
                    
                    // Mutual Friends
                    if (mutualFriends.isNotEmpty()) {
                        item {
                            MutualFriendsSection(mutualFriends)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileHeader(
    profile: UserProfile,
    onMessage: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Photo
            Surface(
                shape = CircleShape,
                color = Color(0xFF007AFF),
                modifier = Modifier.size(120.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = profile.displayName.firstOrNull()?.uppercase() ?: "?",
                        color = Color.White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Name
            Text(
                text = profile.displayName,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1C1E)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Email
            Text(
                text = profile.email,
                fontSize = 15.sp,
                color = Color(0xFF8E8E93)
            )
            
            if (profile.bio.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = profile.bio,
                    fontSize = 15.sp,
                    color = Color(0xFF1C1C1E),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Message Button
            Button(
                onClick = onMessage,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF007AFF)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Email, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Message", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun DrivingScoreCard(stats: com.nextcs.aurora.social.DrivingStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Driving Score",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1C1C1E)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Large Score Display
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = (stats.drivingScore / 100).toFloat(),
                    modifier = Modifier.fillMaxSize(),
                    color = when {
                        stats.drivingScore >= 80 -> Color(0xFF34C759)
                        stats.drivingScore >= 60 -> Color(0xFFFFCC00)
                        else -> Color(0xFFFF3B30)
                    },
                    strokeWidth = 12.dp,
                    trackColor = Color(0xFFE5E5EA)
                )
                Text(
                    text = "${stats.drivingScore.toInt()}",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1C1E)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = when {
                    stats.drivingScore >= 80 -> "Excellent Driver"
                    stats.drivingScore >= 60 -> "Good Driver"
                    stats.drivingScore >= 40 -> "Fair Driver"
                    else -> "New Driver"
                },
                fontSize = 15.sp,
                color = Color(0xFF8E8E93)
            )
        }
    }
}

@Composable
fun StatisticsCard(stats: com.nextcs.aurora.social.DrivingStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Statistics",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1C1E)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Rating
            StatRow(
                icon = Icons.Default.Star,
                iconColor = Color(0xFFFFCC00),
                label = "Average Rating",
                value = String.format("%.1f", stats.averageRating),
                subtitle = "${stats.totalRatings} ratings"
            )
            
            Divider(modifier = Modifier.padding(vertical = 12.dp))
            
            // Safety Score
            StatRow(
                icon = Icons.Default.CheckCircle,
                iconColor = Color(0xFF34C759),
                label = "Safety Score",
                value = "${stats.safetyScore.toInt()}%",
                subtitle = null
            )
            
            Divider(modifier = Modifier.padding(vertical = 12.dp))
            
            // Punctuality
            StatRow(
                icon = Icons.Default.Done,
                iconColor = Color(0xFF007AFF),
                label = "Punctuality",
                value = "${stats.punctualityScore.toInt()}%",
                subtitle = "${stats.onTimePercentage.toInt()}% on-time"
            )
            
            Divider(modifier = Modifier.padding(vertical = 12.dp))
            
            // Total Rides
            StatRow(
                icon = Icons.Default.Place,
                iconColor = Color(0xFFFF9500),
                label = "Total Rides",
                value = "${stats.totalRides}",
                subtitle = "${stats.completedRides} completed"
            )
            
            Divider(modifier = Modifier.padding(vertical = 12.dp))
            
            // Responsiveness
            StatRow(
                icon = Icons.Default.Notifications,
                iconColor = Color(0xFF5856D6),
                label = "Responsiveness",
                value = "${stats.responsiveness.toInt()}%",
                subtitle = null
            )
        }
    }
}

@Composable
fun StatRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    label: String,
    value: String,
    subtitle: String?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = label,
                    fontSize = 15.sp,
                    color = Color(0xFF1C1C1E)
                )
                subtitle?.let {
                    Text(
                        text = it,
                        fontSize = 13.sp,
                        color = Color(0xFF8E8E93)
                    )
                }
            }
        }
        
        Text(
            text = value,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1C1C1E)
        )
    }
}

@Composable
fun MutualFriendsSection(mutualFriends: List<UserProfile>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Mutual Friends (${mutualFriends.size})",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1C1E)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            mutualFriends.take(5).forEach { friend ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF007AFF),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = friend.displayName.firstOrNull()?.uppercase() ?: "?",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Text(
                        text = friend.displayName,
                        fontSize = 15.sp,
                        color = Color(0xFF1C1C1E)
                    )
                }
            }
            
            if (mutualFriends.size > 5) {
                Text(
                    text = "and ${mutualFriends.size - 5} more",
                    fontSize = 13.sp,
                    color = Color(0xFF8E8E93),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
