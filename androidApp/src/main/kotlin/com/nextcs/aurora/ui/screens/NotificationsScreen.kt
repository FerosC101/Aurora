package com.nextcs.aurora.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.nextcs.aurora.social.NotificationService
import com.nextcs.aurora.social.Notification
import com.nextcs.aurora.social.SocialFirebaseService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onNavigateToFriendRequests: () -> Unit = {},
    onNavigateToChat: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val notificationService = remember { NotificationService(context) }
    val socialService = remember { SocialFirebaseService(context) }
    val scope = rememberCoroutineScope()
    
    var notifications by remember { mutableStateOf<List<com.nextcs.aurora.social.Notification>>(emptyList()) }
    var notificationsError by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        try {
            notificationService.observeNotifications().collect { notificationList ->
                notifications = notificationList
            }
        } catch (e: Exception) {
            notificationsError = "Notifications unavailable"
            android.util.Log.w("NotificationsScreen", "Error observing notifications: ${e.message}")
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Notifications")
                        if (notifications.count { !it.read } > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.Red
                            ) {
                                Text(
                                    "${notifications.count { !it.read }}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6200EE),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8F9FA))
        ) {
            if (notifications.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No notifications",
                        fontSize = 18.sp,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(notifications) { notification ->
                        NotificationItem(
                            notification = notification,
                            onMarkAsRead = {
                                scope.launch {
                                    notificationService.markAsRead(notification.id)
                                }
                            },
                            onDelete = {
                                scope.launch {
                                    notificationService.deleteNotification(notification.id)
                                }
                            },
                            onAcceptFriendRequest = { requestId ->
                                scope.launch {
                                    socialService.acceptFriendRequest(requestId)
                                    notificationService.markAsRead(notification.id)
                                }
                            },
                            onRejectFriendRequest = { requestId ->
                                scope.launch {
                                    socialService.rejectFriendRequest(requestId)
                                    notificationService.deleteNotification(notification.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: Notification,
    onMarkAsRead: () -> Unit,
    onDelete: () -> Unit,
    onAcceptFriendRequest: (String) -> Unit,
    onRejectFriendRequest: (String) -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (notification.read) Color.White else Color(0xFFF0E6FF),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
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
                        when (notification.type) {
                            "friend_request" -> Icons.Default.Person
                            "message" -> Icons.Default.Email
                            "friend_accepted" -> Icons.Default.Check
                            else -> Icons.Default.Notifications
                        },
                        contentDescription = null,
                        tint = Color(0xFF6200EE),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            notification.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            notification.message,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Text(
                            dateFormat.format(Date(notification.timestamp)),
                            fontSize = 12.sp,
                            color = Color.LightGray
                        )
                    }
                }
                
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Delete",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            // Action buttons for friend requests
            if (notification.type == "friend_request" && !notification.read) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            notification.actionData["requestId"]?.let { requestId ->
                                onAcceptFriendRequest(requestId)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6200EE)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Accept")
                    }
                    
                    OutlinedButton(
                        onClick = {
                            notification.actionData["requestId"]?.let { requestId ->
                                onRejectFriendRequest(requestId)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Decline")
                    }
                }
            }
        }
    }
}
