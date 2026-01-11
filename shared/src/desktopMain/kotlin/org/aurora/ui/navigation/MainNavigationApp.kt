package org.aurora.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.aurora.maps.MapsProvider

/**
 * Desktop Main Navigation App with Sidebar Navigation
 * Ports all Android features to desktop with appropriate layout
 */
@Composable
fun MainNavigationApp(
    userName: String,
    userEmail: String,
    mapsProvider: MapsProvider,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedScreen by remember { mutableStateOf<NavScreen>(NavScreen.Home) }
    var notificationCount by remember { mutableStateOf(3) }
    var unreadMessageCount by remember { mutableStateOf(2) }
    
    Row(modifier = modifier.fillMaxSize()) {
        // Left Sidebar Navigation
        DesktopSideNavigation(
            selectedScreen = selectedScreen,
            onSelectScreen = { selectedScreen = it },
            userName = userName,
            notificationCount = notificationCount,
            unreadMessageCount = unreadMessageCount,
            onLogout = onLogout
        )
        
        // Main Content Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color(0xFFF5F5F7))
        ) {
            AnimatedContent(
                targetState = selectedScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith
                            fadeOut(animationSpec = tween(300))
                }
            ) { screen ->
                when (screen) {
                    NavScreen.Home -> DesktopHomeScreen(
                        mapsProvider = mapsProvider
                    )
                    NavScreen.Social -> DesktopSocialScreen(
                        userName = userName,
                        userEmail = userEmail
                    )
                    NavScreen.Activity -> DesktopActivityScreen()
                    NavScreen.Profile -> DesktopProfileScreen(
                        userName = userName,
                        userEmail = userEmail,
                        onLogout = onLogout
                    )
                    NavScreen.Notifications -> DesktopNotificationsScreen()
                    NavScreen.Chats -> DesktopChatScreen()
                }
            }
        }
    }
}

/**
 * Desktop-style sidebar navigation (vertical)
 */
@Composable
fun DesktopSideNavigation(
    selectedScreen: NavScreen,
    onSelectScreen: (NavScreen) -> Unit,
    userName: String,
    notificationCount: Int,
    unreadMessageCount: Int,
    onLogout: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight(),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // App Logo/Title
            Column(modifier = Modifier.padding(vertical = 20.dp)) {
                Text(
                    text = "Aurora",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF007AFF)
                )
                Text(
                    text = "Desktop Travel",
                    fontSize = 14.sp,
                    color = Color(0xFF8E8E93)
                )
            }
            
            Divider(color = Color(0xFFE5E5EA))
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Navigation Items
            SideNavItem(
                icon = Icons.Default.Home,
                label = "Home",
                selected = selectedScreen == NavScreen.Home,
                onClick = { onSelectScreen(NavScreen.Home) }
            )
            
            SideNavItem(
                icon = Icons.Default.People,
                label = "Social",
                selected = selectedScreen == NavScreen.Social,
                onClick = { onSelectScreen(NavScreen.Social) }
            )
            
            SideNavItem(
                icon = Icons.Default.Message,
                label = "Messages",
                selected = selectedScreen == NavScreen.Chats,
                badgeCount = if (unreadMessageCount > 0) unreadMessageCount else null,
                onClick = { onSelectScreen(NavScreen.Chats) }
            )
            
            SideNavItem(
                icon = Icons.Default.Notifications,
                label = "Notifications",
                selected = selectedScreen == NavScreen.Notifications,
                badgeCount = if (notificationCount > 0) notificationCount else null,
                onClick = { onSelectScreen(NavScreen.Notifications) }
            )
            
            SideNavItem(
                icon = Icons.Default.History,
                label = "Activity",
                selected = selectedScreen == NavScreen.Activity,
                onClick = { onSelectScreen(NavScreen.Activity) }
            )
            
            SideNavItem(
                icon = Icons.Default.Person,
                label = "Profile",
                selected = selectedScreen == NavScreen.Profile,
                onClick = { onSelectScreen(NavScreen.Profile) }
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Divider(color = Color(0xFFE5E5EA))
            
            // User Info at Bottom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = Color(0xFF007AFF).copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = userName.take(1).uppercase(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF007AFF)
                        )
                    }
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1C1C1E)
                    )
                    Text(
                        text = "Online",
                        fontSize = 12.sp,
                        color = Color(0xFF34C759)
                    )
                }
            }
        }
    }
}

@Composable
fun SideNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    badgeCount: Int? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        color = if (selected) Color(0xFF007AFF).copy(alpha = 0.1f) else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (selected) Color(0xFF007AFF) else Color(0xFF8E8E93),
                modifier = Modifier.size(24.dp)
            )
            
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) Color(0xFF007AFF) else Color(0xFF1C1C1E),
                modifier = Modifier.weight(1f)
            )
            
            if (badgeCount != null && badgeCount > 0) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFF3B30),
                    modifier = Modifier.size(22.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (badgeCount > 9) "9+" else badgeCount.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// Navigation Screens enum
sealed class NavScreen {
    object Home : NavScreen()
    object Social : NavScreen()
    object Chats : NavScreen()
    object Notifications : NavScreen()
    object Activity : NavScreen()
    object Profile : NavScreen()
}

// Placeholder screens - to be implemented with full functionality
@Composable
fun DesktopHomeScreen(mapsProvider: MapsProvider) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Icon(
                Icons.Default.Map,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = Color(0xFF007AFF).copy(alpha = 0.3f)
            )
            Text(
                text = "Home Screen",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1C1E)
            )
            Text(
                text = "Navigation and route planning coming soon",
                fontSize = 16.sp,
                color = Color(0xFF8E8E93)
            )
        }
    }
}

@Composable
fun DesktopSocialScreen(userName: String, userEmail: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Icon(
                Icons.Default.People,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = Color(0xFF007AFF).copy(alpha = 0.3f)
            )
            Text(
                text = "Social",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1C1E)
            )
            Text(
                text = "Friends, carpool & ride sharing coming soon",
                fontSize = 16.sp,
                color = Color(0xFF8E8E93)
            )
        }
    }
}

@Composable
fun DesktopActivityScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Icon(
                Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = Color(0xFF007AFF).copy(alpha = 0.3f)
            )
            Text(
                text = "Activity",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1C1E)
            )
            Text(
                text = "Trip history and analytics coming soon",
                fontSize = 16.sp,
                color = Color(0xFF8E8E93)
            )
        }
    }
}

@Composable
fun DesktopProfileScreen(
    userName: String,
    userEmail: String,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp)
    ) {
        Text(
            text = "Profile",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1C1C1E)
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // Profile Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(100.dp),
                    shape = CircleShape,
                    color = Color(0xFF007AFF).copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = userName.take(1).uppercase(),
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF007AFF)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = userName,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1C1E)
                )
                
                Text(
                    text = userEmail,
                    fontSize = 16.sp,
                    color = Color(0xFF8E8E93)
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = onLogout,
                    modifier = Modifier.height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF3B30)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout", fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun DesktopNotificationsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp)
    ) {
        Text(
            text = "Notifications",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1C1C1E)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Sample notifications
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NotificationCard(
                title = "New Friend Request",
                message = "John Doe wants to be your friend",
                time = "5 min ago",
                unread = true
            )
            NotificationCard(
                title = "Carpool Request Accepted",
                message = "Your carpool request to Manila was accepted",
                time = "1 hour ago",
                unread = true
            )
            NotificationCard(
                title = "Trip Completed",
                message = "You completed a trip to Makati",
                time = "2 hours ago",
                unread = false
            )
        }
    }
}

@Composable
fun NotificationCard(
    title: String,
    message: String,
    time: String,
    unread: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (unread) Color(0xFFE3F2FD) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = Color(0xFF007AFF).copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        tint = Color(0xFF007AFF)
                    )
                }
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1C1C1E)
                    )
                    Text(
                        text = time,
                        fontSize = 13.sp,
                        color = Color(0xFF8E8E93)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = Color(0xFF3C3C43)
                )
            }
        }
    }
}

@Composable
fun DesktopChatScreen() {
    Row(modifier = Modifier.fillMaxSize()) {
        // Left: Chat List
        Surface(
            modifier = Modifier
                .width(350.dp)
                .fillMaxHeight(),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Messages",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1C1E)
                        )
                        IconButton(onClick = { }) {
                            Icon(
                                Icons.Default.Create,
                                contentDescription = "New Message",
                                tint = Color(0xFF007AFF)
                            )
                        }
                    }
                }
                
                // Chat List
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ChatListItem(
                        name = "John Doe",
                        lastMessage = "Let's meet at the station",
                        time = "2 min",
                        unread = true,
                        selected = true
                    )
                    ChatListItem(
                        name = "Jane Smith",
                        lastMessage = "Thanks for the ride!",
                        time = "1 hour",
                        unread = false,
                        selected = false
                    )
                    ChatListItem(
                        name = "Carpool Group",
                        lastMessage = "See you tomorrow",
                        time = "3 hours",
                        unread = false,
                        selected = false
                    )
                }
            }
        }
        
        // Right: Active Chat
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color(0xFFF5F5F7)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.Message,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = Color(0xFF007AFF).copy(alpha = 0.3f)
                )
                Text(
                    text = "Select a conversation",
                    fontSize = 20.sp,
                    color = Color(0xFF8E8E93)
                )
            }
        }
    }
}

@Composable
fun ChatListItem(
    name: String,
    lastMessage: String,
    time: String,
    unread: Boolean,
    selected: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (selected) Color(0xFFE5E5EA) else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        onClick = { }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = Color(0xFF007AFF).copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = name.take(1),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF007AFF)
                    )
                }
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = name,
                        fontSize = 15.sp,
                        fontWeight = if (unread) FontWeight.SemiBold else FontWeight.Normal,
                        color = Color(0xFF1C1C1E)
                    )
                    Text(
                        text = time,
                        fontSize = 13.sp,
                        color = if (unread) Color(0xFF007AFF) else Color(0xFF8E8E93)
                    )
                }
                Text(
                    text = lastMessage,
                    fontSize = 14.sp,
                    color = Color(0xFF8E8E93),
                    maxLines = 1
                )
            }
            
            if (unread) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF007AFF),
                    modifier = Modifier.size(10.dp)
                ) {}
            }
        }
    }
}
