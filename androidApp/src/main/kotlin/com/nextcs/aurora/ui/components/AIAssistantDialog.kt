package com.nextcs.aurora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.window.DialogProperties
import com.nextcs.aurora.ai.ChatMessage
import com.nextcs.aurora.ai.RouteAssistantService
import kotlinx.coroutines.launch

@Composable
fun AIAssistantDialog(
    onDismiss: () -> Unit,
    onRouteSelected: (origin: String, destination: String) -> Unit
) {
    val context = LocalContext.current
    val routeAssistant = remember { RouteAssistantService(context) }
    val scope = rememberCoroutineScope()
    
    var messageText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(routeAssistant.getConversationHistory()) }
    var isLoading by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF007AFF),
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "AI Route Assistant",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1C1C1E),
                        letterSpacing = (-0.3).sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF8E8E93)
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().height(450.dp)) {
                // Chat messages
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    state = listState
                ) {
                    if (messages.isEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                color = Color(0xFFF2F2F7),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Color(0xFF007AFF),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "Hi! I'm your AI route assistant",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1C1C1E),
                                        letterSpacing = (-0.3).sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Ask me about routes, traffic, or navigation!",
                                        fontSize = 15.sp,
                                        color = Color(0xFF8E8E93)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                "Try asking:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF8E8E93)
                            )
                            
                            listOf(
                                "Best route to SM Mall of Asia",
                                "How to get to Makati avoiding traffic",
                                "Route to BGC with parking"
                            ).forEach { suggestion ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    onClick = {
                                        messageText = suggestion
                                    },
                                    color = Color(0xFFF8F9FA),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFB0B0B5))
                                ) {
                                    Text(
                                        suggestion,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF1C1C1E)
                                    )
                                }
                            }
                        }
                    } else {
                        items(messages) { message ->
                            ChatMessageBubble(message)
                        }
                        
                        if (isLoading) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Surface(
                                        color = Color(0xFFF2F2F7),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = Color(0xFF007AFF)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                "Thinking...",
                                                fontSize = 15.sp,
                                                color = Color(0xFF8E8E93)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Input field
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                "Ask me anything...",
                                fontSize = 15.sp,
                                color = Color(0xFF8E8E93)
                            )
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF007AFF),
                            unfocusedBorderColor = Color(0xFFD1D1D6)
                        ),
                        maxLines = 3
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    FloatingActionButton(
                        onClick = {
                            if (messageText.isNotBlank() && !isLoading) {
                                val query = messageText
                                messageText = ""
                                scope.launch {
                                    isLoading = true
                                    try {
                                        val response = routeAssistant.sendMessage(query)
                                        messages = routeAssistant.getConversationHistory()
                                        
                                        // Auto-scroll to bottom
                                        listState.animateScrollToItem(messages.size - 1)
                                        
                                        // Check if response contains route request
                                        response.routeRequest?.let { request ->
                                            onDismiss()
                                            onRouteSelected(request.origin, request.destination)
                                        }
                                    } catch (e: Exception) {
                                        // Show error
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        containerColor = if (messageText.isBlank() || isLoading) Color(0xFFD1D1D6) else Color(0xFF007AFF),
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Send",
                            tint = Color.White
                        )
                    }
                }
            }
        },
        confirmButton = {},
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.95f),
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White
    )
}

@Composable
fun ChatMessageBubble(message: ChatMessage) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isUser) {
            Icon(
                Icons.Default.Face,
                contentDescription = null,
                tint = Color(0xFF007AFF),
                modifier = Modifier.size(28.dp).padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        
        Surface(
            color = if (message.isUser) Color(0xFF007AFF) else Color(0xFFF2F2F7),
            shape = RoundedCornerShape(
                topStart = if (message.isUser) 18.dp else 4.dp,
                topEnd = if (message.isUser) 4.dp else 18.dp,
                bottomStart = 18.dp,
                bottomEnd = 18.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = message.text,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    color = if (message.isUser) Color.White else Color(0xFF1C1C1E)
                )
                
                message.routeRequest?.let { request ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = if (message.isUser) Color(0xFF0056CC) else Color.White,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "\ud83d\udccd Route Request",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (message.isUser) Color.White else Color(0xFF007AFF),
                                letterSpacing = (-0.2).sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "From: ${request.origin}",
                                fontSize = 13.sp,
                                color = if (message.isUser) Color.White.copy(alpha = 0.9f) else Color(0xFF8E8E93)
                            )
                            Text(
                                "To: ${request.destination}",
                                fontSize = 13.sp,
                                color = if (message.isUser) Color.White.copy(alpha = 0.9f) else Color(0xFF8E8E93)
                            )
                        }
                    }
                }
            }
        }
    }
}
