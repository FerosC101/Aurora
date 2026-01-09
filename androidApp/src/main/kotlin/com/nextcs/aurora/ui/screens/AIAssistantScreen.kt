package com.nextcs.aurora.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.nextcs.aurora.ai.ChatMessage
import com.nextcs.aurora.ai.RouteAssistantService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIAssistantScreen(
    onNavigateToRoute: (origin: String, destination: String, waypoints: List<String>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val assistantService = remember { RouteAssistantService(context) }
    val scope = rememberCoroutineScope()
    
    var messages by remember { mutableStateOf(assistantService.getConversationHistory()) }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    val listState = rememberLazyListState()
    
    // Initial greeting
    LaunchedEffect(Unit) {
        if (messages.isEmpty()) {
            messages = listOf(
                ChatMessage(
                    text = "Hi! I'm Aurora, your navigation assistant. Where would you like to go today?",
                    isUser = false
                )
            )
        }
    }
    
    // Auto-scroll to bottom when new message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1976D2)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Aurora Assistant", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("AI-powered routing", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
            // Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { message ->
                    ChatBubble(
                        message = message,
                        onStartRoute = { routeRequest ->
                            try {
                                Log.d("AIAssistant", "Start Route button clicked!")
                                Log.d("AIAssistant", "RouteRequest: $routeRequest")
                                Log.d("AIAssistant", "Origin: ${routeRequest.origin}")
                                Log.d("AIAssistant", "Destination: ${routeRequest.destination}")
                                Log.d("AIAssistant", "Waypoints: ${routeRequest.waypoints}")
                                onNavigateToRoute(
                                    routeRequest.origin,
                                    routeRequest.destination,
                                    routeRequest.waypoints
                                )
                                Log.d("AIAssistant", "onNavigateToRoute called successfully")
                            } catch (e: Exception) {
                                Log.e("AIAssistant", "Navigation error", e)
                                Log.e("AIAssistant", "Error details: ${e.message}")
                                e.printStackTrace()
                            }
                        }
                    )
                }
                
                if (isLoading) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Aurora is thinking...", fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
            
            // Quick Suggestions (only show when no conversation)
            if (messages.size <= 1) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    items(assistantService.getQuickSuggestions()) { suggestion ->
                        SuggestionChip(
                            onClick = {
                                inputText = suggestion
                            },
                            label = { Text(suggestion, fontSize = 12.sp) }
                        )
                    }
                }
            }
            
            // Input Area
            Surface(
                shadowElevation = 8.dp,
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask Aurora anything...", color = Color(0xFF757575)) },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1976D2),
                            unfocusedBorderColor = Color.LightGray,
                            focusedTextColor = Color(0xFF212121),
                            unfocusedTextColor = Color(0xFF212121),
                            cursorColor = Color(0xFF1976D2)
                        ),
                        maxLines = 4
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    FloatingActionButton(
                        onClick = {
                            if (inputText.isNotBlank() && !isLoading) {
                                scope.launch {
                                    isLoading = true
                                    val response = assistantService.sendMessage(inputText)
                                    messages = assistantService.getConversationHistory()
                                    inputText = ""
                                    isLoading = false
                                }
                            }
                        },
                        containerColor = Color(0xFF1976D2),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    onStartRoute: (com.nextcs.aurora.ai.RouteRequest) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isUser) Color(0xFF1976D2) else Color.White
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.text,
                    color = if (message.isUser) Color.White else Color.Black,
                    fontSize = 14.sp
                )
                
                // Show "Start Route" button if route request is present
                message.routeRequest?.let { routeRequest ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { 
                            Log.d("ChatBubble", "Start Route button clicked in ChatBubble")
                            Log.d("ChatBubble", "Route: ${routeRequest.origin} -> ${routeRequest.destination}")
                            onStartRoute(routeRequest)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Start Route")
                    }
                }
            }
        }
    }
}
