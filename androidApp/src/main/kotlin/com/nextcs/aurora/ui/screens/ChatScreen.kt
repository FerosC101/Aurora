package com.nextcs.aurora.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nextcs.aurora.social.ChatService
import com.nextcs.aurora.social.ChatMessage
import com.nextcs.aurora.social.ChatConversation
import com.nextcs.aurora.social.MessageStatus
import com.nextcs.aurora.social.SocialFirebaseService
import com.nextcs.aurora.social.UserProfile
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/**
 * Facebook Messenger-style two-panel chat interface
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val chatService = remember { ChatService(context) }
    val socialService = remember { SocialFirebaseService(context) }
    val scope = rememberCoroutineScope()
    
    var chats by remember { mutableStateOf<List<ChatConversation>>(emptyList()) }
    var selectedChatId by remember { mutableStateOf<String?>(null) }
    var showNewMessageDialog by remember { mutableStateOf(false) }
    var chatsError by remember { mutableStateOf<String?>(null) }
    
    // Observe chats list with error handling
    LaunchedEffect(Unit) {
        try {
            chatService.observeChats().collect { chatList ->
                chats = chatList
                android.util.Log.d("ChatScreen", "Received ${chatList.size} chats")
            }
        } catch (e: Exception) {
            chatsError = "Chats unavailable: ${e.message}"
            android.util.Log.w("ChatScreen", "Chats collection failed", e)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Messages",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showNewMessageDialog = true }) {
                        Icon(
                            Icons.Default.Create,
                            contentDescription = "New Message",
                            tint = Color(0xFF007AFF)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF1C1C1E),
                    navigationIconContentColor = Color(0xFF007AFF)
                )
            )
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Left Panel - Conversation List
            ConversationListPanel(
                chats = chats,
                selectedChatId = selectedChatId,
                onSelectChat = { chatId -> selectedChatId = chatId },
                modifier = Modifier
                    .weight(0.35f)
                    .fillMaxHeight()
            )
            
            // Divider
            VerticalDivider(
                modifier = Modifier.fillMaxHeight(),
                color = Color(0xFFE5E5EA)
            )
            
            // Right Panel - Active Chat or Empty State
            Box(
                modifier = Modifier
                    .weight(0.65f)
                    .fillMaxHeight()
            ) {
                selectedChatId?.let { chatId ->
                    val chat = chats.find { it.id == chatId }
                    chat?.let {
                        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
                        val otherUserId = it.participants.firstOrNull { id -> id != currentUserId } ?: ""
                        val otherUserName = it.participantNames[otherUserId] ?: "Unknown"
                        ActiveChatPanel(
                            chatId = chatId,
                            otherUserName = otherUserName,
                            otherUserId = otherUserId,
                            chatService = chatService,
                            onClose = { selectedChatId = null }
                        )
                    }
                } ?: run {
                    // Empty state
                    EmptyChatState(
                        modifier = Modifier.align(Alignment.Center),
                        onNewMessage = { showNewMessageDialog = true }
                    )
                }
            }
        }
    }
    
    if (showNewMessageDialog) {
        NewMessageDialog(
            socialService = socialService,
            chatService = chatService,
            onDismiss = { showNewMessageDialog = false },
            onChatCreated = { chatId ->
                selectedChatId = chatId
                showNewMessageDialog = false
            }
        )
    }
}

@Composable
fun ConversationListPanel(
    chats: List<ChatConversation>,
    selectedChatId: String?,
    onSelectChat: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0xFFF5F5F7)
    ) {
        if (chats.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Email,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = Color(0xFFE5E5EA)
                    )
                    Text(
                        "No conversations",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF8E8E93)
                    )
                    Text(
                        "Start a new chat",
                        fontSize = 13.sp,
                        color = Color(0xFF8E8E93)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = chats,
                    key = { it.id }
                ) { chat ->
                    ConversationItem(
                        chat = chat,
                        isSelected = chat.id == selectedChatId,
                        onClick = { onSelectChat(chat.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ConversationItem(
    chat: ChatConversation,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val otherUserId = chat.participants.firstOrNull { it != currentUserId } ?: ""
    val otherUserName = chat.participantNames[otherUserId] ?: "Unknown"
    val unreadCount = chat.unreadCount[currentUserId] ?: 0
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (isSelected) Color(0xFFE3F2FD) else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Profile photo
            Box {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF007AFF),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = otherUserName.firstOrNull()?.uppercase() ?: "?",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // Unread indicator
                if (unreadCount > 0) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFF3B30),
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.TopEnd)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = otherUserName,
                        fontSize = 16.sp,
                        fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                        color = Color(0xFF1C1C1E),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = formatTimestamp(chat.lastMessageTime),
                        fontSize = 12.sp,
                        color = if (unreadCount > 0) Color(0xFF007AFF) else Color(0xFF8E8E93)
                    )
                }
                
                Text(
                    text = chat.lastMessage,
                    fontSize = 14.sp,
                    color = Color(0xFF8E8E93),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (unreadCount > 0) FontWeight.Medium else FontWeight.Normal
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveChatPanel(
    chatId: String,
    otherUserName: String,
    otherUserId: String,
    chatService: ChatService,
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val messages by chatService.observeMessages(chatId).collectAsState(initial = emptyList())
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
    // Mark messages as delivered when opening chat
    LaunchedEffect(chatId) {
        chatService.markMessagesAsDelivered(chatId)
    }
    
    // Mark as read when entering chat
    LaunchedEffect(chatId) {
        chatService.markAsRead(chatId)
    }
    
    // Mark messages as seen when viewing
    LaunchedEffect(chatId, messages.size) {
        if (messages.isNotEmpty()) {
            chatService.markMessagesAsSeen(chatId)
        }
    }
    
    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Chat header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 2.dp
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
                            text = otherUserName.firstOrNull()?.uppercase() ?: "?",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = otherUserName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1C1C1E)
                    )
                    Text(
                        text = "Active now",
                        fontSize = 13.sp,
                        color = Color(0xFF34C759)
                    )
                }
            }
        }
        
        // Messages
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFFF5F5F7))
        ) {
            if (messages.isEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF007AFF),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = otherUserName.firstOrNull()?.uppercase() ?: "?",
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = otherUserName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1C1E)
                    )
                    Text(
                        "Start the conversation!",
                        fontSize = 14.sp,
                        color = Color(0xFF8E8E93)
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { message ->
                        ChatMessageItem(message)
                    }
                }
            }
        }
        
        // Message input
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("Aa") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF007AFF),
                        unfocusedBorderColor = Color(0xFFE5E5EA),
                        focusedContainerColor = Color(0xFFF5F5F7),
                        unfocusedContainerColor = Color(0xFFF5F5F7)
                    ),
                    minLines = 1,
                    maxLines = 4
                )
                
                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            scope.launch {
                                chatService.sendMessage(chatId, messageText.trim())
                                messageText = ""
                            }
                        }
                    },
                    enabled = messageText.isNotBlank(),
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (messageText.isNotBlank()) Color(0xFF007AFF) else Color(0xFFE5E5EA),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage) {
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    val isCurrentUser = message.senderId == auth.currentUser?.uid
    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isCurrentUser) 18.dp else 4.dp,
                bottomEnd = if (isCurrentUser) 4.dp else 18.dp
            ),
            color = if (isCurrentUser) Color(0xFF007AFF) else Color.White,
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    message.message,
                    fontSize = 15.sp,
                    color = if (isCurrentUser) Color.White else Color(0xFF1C1C1E)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        dateFormat.format(Date(message.timestamp)),
                        fontSize = 11.sp,
                        color = if (isCurrentUser) Color.White.copy(alpha = 0.8f) else Color(0xFF8E8E93)
                    )
                    
                    // Show status indicators for sent messages
                    if (isCurrentUser) {
                        val statusIcon = when (message.status) {
                            MessageStatus.SEEN.name -> "✓✓" // Double check for seen
                            MessageStatus.DELIVERED.name -> "✓✓" // Double check for delivered
                            MessageStatus.SENT.name -> "✓" // Single check for sent
                            else -> "✓"
                        }
                        val statusColor = if (message.status == MessageStatus.SEEN.name) {
                            Color(0xFF34C759) // Green for seen
                        } else {
                            Color.White.copy(alpha = 0.8f)
                        }
                        
                        Text(
                            statusIcon,
                            fontSize = 10.sp,
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyChatState(
    modifier: Modifier = Modifier,
    onNewMessage: () -> Unit
) {
    Column(
        modifier = modifier.padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Default.Email,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color(0xFFE5E5EA)
        )
        Text(
            "Select a conversation",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1C1C1E)
        )
        Text(
            "Choose from existing conversations\nor start a new message",
            fontSize = 15.sp,
            color = Color(0xFF8E8E93),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Button(
            onClick = onNewMessage,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF007AFF)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                Icons.Default.Create,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("New Message", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun NewMessageDialog(
    socialService: SocialFirebaseService,
    chatService: ChatService,
    onDismiss: () -> Unit,
    onChatCreated: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    
    // Search friends
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            isSearching = true
            scope.launch {
                socialService.searchUsers(searchQuery).onSuccess { users ->
                    searchResults = users
                    isSearching = false
                }.onFailure {
                    isSearching = false
                }
            }
        } else {
            searchResults = emptyList()
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "New Message",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1C1E)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                HorizontalDivider()
                
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search friends...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF007AFF),
                        unfocusedBorderColor = Color(0xFFE5E5EA)
                    )
                )
                
                // Results
                if (isSearching) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF007AFF))
                    }
                } else if (searchResults.isEmpty() && searchQuery.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No friends found",
                            color = Color(0xFF8E8E93)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(searchResults) { user ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch {
                                            chatService.getOrCreateChat(
                                                otherUserId = user.userId,
                                                otherUserName = user.displayName
                                            ).onSuccess { chatId ->
                                                onChatCreated(chatId)
                                            }
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
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
                                                text = user.displayName.firstOrNull()?.uppercase() ?: "?",
                                                color = Color.White,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = user.displayName,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF1C1C1E)
                                        )
                                        Text(
                                            text = user.email,
                                            fontSize = 14.sp,
                                            color = Color(0xFF8E8E93)
                                        )
                                    }
                                    
                                    Icon(
                                        Icons.Default.Send,
                                        contentDescription = null,
                                        tint = Color(0xFF007AFF)
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

fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m"
        diff < 86400_000 -> {
            val dateFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            dateFormat.format(Date(timestamp))
        }
        diff < 604800_000 -> {
            val dateFormat = SimpleDateFormat("EEE", Locale.getDefault())
            dateFormat.format(Date(timestamp))
        }
        else -> {
            val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
            dateFormat.format(Date(timestamp))
        }
    }
}

// Legacy single-chat screen for backward compatibility
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreenLegacy(
    chatId: String,
    otherUserName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val chatService = remember { ChatService(context) }
    
    // Get other user ID from chat
    var otherUserId by remember { mutableStateOf("") }
    
    LaunchedEffect(chatId) {
        // Extract other user ID from chat
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val chatDoc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("chats")
            .document(chatId)
            .get()
            .await()
        
        val participants = chatDoc.get("participants") as? List<*> ?: emptyList<String>()
        otherUserId = participants.firstOrNull { it != currentUserId }?.toString() ?: ""
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        if (otherUserId.isNotEmpty()) {
            ActiveChatPanel(
                chatId = chatId,
                otherUserName = otherUserName,
                otherUserId = otherUserId,
                chatService = chatService,
                onClose = onBack
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsListScreen(
    onChatSelected: (String, String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val chatService = remember { ChatService(context) }
    
    var chats by remember { mutableStateOf<List<com.nextcs.aurora.social.ChatConversation>>(emptyList()) }
    var chatsError by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        try {
            chatService.observeChats().collect { chatList ->
                chats = chatList
            }
        } catch (e: Exception) {
            chatsError = "Chats unavailable"
            android.util.Log.w("ChatsListScreen", "Error observing chats: ${e.message}")
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages") },
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
        if (chats.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Email,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No conversations yet",
                        fontSize = 17.sp,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(chats) { chat ->
                    val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
                    val otherUserName = chat.participantNames
                        .filterKeys { it != currentUserId }
                        .values
                        .firstOrNull() ?: "Unknown"
                    
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        onClick = { onChatSelected(chat.id, otherUserName) },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar
                            Surface(
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = Color(0xFF007AFF),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = otherUserName.firstOrNull()?.uppercase() ?: "?",
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = otherUserName,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1C1C1E)
                                    )
                                }
                                
                                Text(
                                    text = chat.lastMessage,
                                    fontSize = 14.sp,
                                    color = Color(0xFF8E8E93),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
