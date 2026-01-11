package com.nextcs.aurora.ai

import android.content.Context
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class RouteRequest(
    val origin: String = "",
    val destination: String = "",
    val waypoints: List<String> = emptyList()
)

data class ChatMessage(
    val id: String = "",
    val text: String = "",
    val isUser: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String = "",
    val routeRequest: RouteRequest? = null
)

class RouteAssistantService(private val context: Context) {
    
    private val TAG = "RouteAssistantService"
    private val apiKey = getGeminiApiKey()
    private val model = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = apiKey
    )
    
    private val conversationHistory = mutableListOf<ChatMessage>()
    private val firestore = FirebaseFirestore.getInstance(FirebaseApp.getInstance(), "sfse")
    private val auth = FirebaseAuth.getInstance()
    
    init {
        Log.d(TAG, "Initializing RouteAssistantService")
        Log.d(TAG, "API Key length: ${apiKey.length}")
        Log.d(TAG, "API Key starts with: ${apiKey.take(10)}...")
    }
    
    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
    
    private fun getGeminiApiKey(): String {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(
                context.packageName,
                android.content.pm.PackageManager.GET_META_DATA
            )
            val key = appInfo.metaData?.getString("com.google.android.geo.GEMINI_API_KEY") ?: ""
            Log.d(TAG, "Retrieved API key from manifest: ${if (key.isEmpty()) "EMPTY" else "OK"}")
            key
        } catch (e: Exception) {
            Log.e(TAG, "Error getting API key", e)
            ""
        }
    }
    
    suspend fun sendMessage(userMessage: String): ChatMessage = withContext(Dispatchers.IO) {
        val userId = getCurrentUserId() ?: return@withContext ChatMessage(
            id = java.util.UUID.randomUUID().toString(),
            text = "Please log in to use the AI assistant.",
            isUser = false
        )
        
        // Add user message to history
        val userChatMessage = ChatMessage(
            id = java.util.UUID.randomUUID().toString(),
            text = userMessage,
            isUser = true,
            userId = userId
        )
        conversationHistory.add(userChatMessage)
        saveConversationHistory()
        
        Log.d(TAG, "Sending message to Gemini: $userMessage")
        
        try {
            // Build context-aware prompt
            val systemPrompt = """
                You are Aurora, a friendly and detailed navigation assistant. Help users plan their routes with comprehensive information.
                
                When users ask for directions or routes:
                1. Extract origin, destination, and any waypoints (stops along the way)
                2. Provide a DETAILED response that includes:
                   - A friendly greeting/acknowledgment
                   - The full route description (origin → waypoints → destination)
                   - Estimated distance and travel time
                   - Route sequence clearly showing all stops
                3. ALWAYS end with route JSON on a new line
                
                CRITICAL FORMATTING RULES:
                • For single destination: {"origin": "location", "destination": "location", "waypoints": []}
                • For multiple stops: {"origin": "location", "destination": "final location", "waypoints": ["stop1", "stop2", "stop3"]}
                • Waypoints are stops BETWEEN origin and destination (not including them)
                • ALWAYS include ROUTE_JSON even if just giving info
                
                Example for multi-stop route:
                "Perfect! I'll create a multi-stop route for you.
                
                📍 Your Multi-Stop Journey:
                1. Starting Point: Current Location
                2. Stop 1: SM Mall Manila
                3. Stop 2: Robinsons Place
                4. Final Destination: Greenbelt Makati
                
                🛣️ Route Details:
                • Total Estimated Distance: ~15 km
                • Total Estimated Time: 45-60 minutes
                • Number of Stops: 2 waypoints
                
                I'll optimize the route to minimize travel time between all your stops."
                
                ROUTE_JSON:
                {"origin": "current location", "destination": "Greenbelt Makati", "waypoints": ["SM Mall Manila", "Robinsons Place"]}
                
                Example for single destination:
                "Great! I'll help you get to SM Mall Makati."
                
                ROUTE_JSON:
                {"origin": "current location", "destination": "SM Mall Makati", "waypoints": []}
                
                ALWAYS include ROUTE_JSON for ANY navigation request.
                If they say "go to X via Y", then Y is a waypoint.
                If they say "stop at X on the way to Y", then X is a waypoint.
            """.trimIndent()
            
            val prompt = "$systemPrompt\n\nUser: $userMessage\n\nAssistant:"
            
            Log.d(TAG, "Calling Gemini API...")
            val response = model.generateContent(prompt)
            val aiText = response.text ?: "I'm having trouble understanding. Could you rephrase that?"
            
            Log.d(TAG, "Gemini response received: ${aiText.take(100)}...")
            Log.d(TAG, "Full response length: ${aiText.length}")
            Log.d(TAG, "Contains ROUTE_JSON: ${aiText.contains("ROUTE_JSON:")}")
            
            // Parse response for route information
            val routeRequest = parseRouteFromResponse(aiText)
            Log.d(TAG, "Parsed route request: $routeRequest")
            
            // Clean AI response (remove JSON if present)
            val cleanedText = aiText.split("ROUTE_JSON:")[0].trim()
            val formattedText = stripMarkdown(cleanedText)
            
            val aiMessage = ChatMessage(
                id = java.util.UUID.randomUUID().toString(),
                text = formattedText,
                isUser = false,
                userId = getCurrentUserId() ?: "",
                routeRequest = routeRequest
            )
            
            conversationHistory.add(aiMessage)
            saveConversationHistory()
            aiMessage
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message to Gemini", e)
            Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Exception message: ${e.message}")
            
            val errorText = when {
                e.message?.contains("403") == true || e.message?.contains("Forbidden") == true -> 
                    "API key error. Please enable Generative Language API in Google Cloud Console."
                e.message?.contains("API key") == true -> 
                    "Invalid API key. Please check your Google Cloud Console settings."
                e.message?.contains("network") == true || e.message?.contains("Unable to resolve host") == true -> 
                    "Network error. Please check your internet connection."
                else -> 
                    "Error: ${e.message ?: "Unknown error"}. Please try again."
            }
            
            val errorMessage = ChatMessage(
                id = java.util.UUID.randomUUID().toString(),
                text = errorText,
                isUser = false,
                userId = getCurrentUserId() ?: ""
            )
            conversationHistory.add(errorMessage)
            saveConversationHistory()
            errorMessage
        }
    }
    
    private fun stripMarkdown(text: String): String {
        return text
            .replace("**", "")  // Remove bold markers
            .replace("__", "")  // Remove bold markers
            .replace("*", "")   // Remove italic markers
            .replace("_", "")   // Remove italic markers
            .replace("##", "")  // Remove heading markers
            .replace("#", "")   // Remove heading markers
            .trim()
    }
    
    private fun parseRouteFromResponse(response: String): RouteRequest? {
        return try {
            Log.d(TAG, "Attempting to parse route from response")
            
            if (!response.contains("ROUTE_JSON:")) {
                Log.d(TAG, "No ROUTE_JSON found in response")
                return null
            }
            
            // Extract JSON part after ROUTE_JSON:
            val parts = response.split("ROUTE_JSON:")
            if (parts.size < 2) {
                Log.e(TAG, "ROUTE_JSON found but no content after it")
                return null
            }
            
            val jsonPart = parts[1].trim()
            Log.d(TAG, "Extracted JSON part: $jsonPart")
            
            // Find the JSON object (between first { and matching })
            val startIdx = jsonPart.indexOf('{')
            val endIdx = jsonPart.indexOf('}', startIdx)
            
            if (startIdx == -1 || endIdx == -1) {
                Log.e(TAG, "Could not find valid JSON braces")
                return null
            }
            
            val jsonString = jsonPart.substring(startIdx, endIdx + 1)
            Log.d(TAG, "Parsed JSON string: $jsonString")
            
            val json = JSONObject(jsonString)
            
            val origin = json.optString("origin", "")
            val destination = json.optString("destination", "")
            val waypointsArray = json.optJSONArray("waypoints")
            
            Log.d(TAG, "Origin: $origin, Destination: $destination")
            
            val waypoints = mutableListOf<String>()
            waypointsArray?.let { array ->
                for (i in 0 until array.length()) {
                    val waypoint = array.getString(i).trim()
                    // Only add non-empty waypoints
                    if (waypoint.isNotEmpty()) {
                        waypoints.add(waypoint)
                    }
                }
            }
            
            Log.d(TAG, "Parsed waypoints: $waypoints (count: ${waypoints.size})")
            
            if (origin.isNotEmpty() && destination.isNotEmpty()) {
                val request = RouteRequest(origin, destination, waypoints)
                Log.d(TAG, "Successfully created RouteRequest: $request")
                request
            } else {
                Log.e(TAG, "Origin or destination is empty")
                null
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing route from response", e)
            Log.e(TAG, "Exception: ${e.message}")
            null
        }
    }
    
    suspend fun loadConversationHistory() {
        withContext(Dispatchers.IO) {
            try {
                val userId = getCurrentUserId() ?: return@withContext
                
                val messages = firestore.collection("chatHistory")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()
                    .toObjects(ChatMessage::class.java)
                    .sortedBy { it.timestamp }
                
                conversationHistory.clear()
                conversationHistory.addAll(messages)
                Log.d(TAG, "Loaded ${conversationHistory.size} messages from Firestore")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading conversation history from Firestore", e)
                conversationHistory.clear()
            }
        }
    }
    
    private suspend fun saveConversationHistory() {
        withContext(Dispatchers.IO) {
            try {
                val userId = getCurrentUserId() ?: return@withContext
                
                // Save only recent messages (last message)
                val recentMessages = conversationHistory.takeLast(1)
                for (msg in recentMessages) {
                    val messageWithUser = ChatMessage(
                        id = msg.id,
                        text = msg.text,
                        isUser = msg.isUser,
                        timestamp = msg.timestamp,
                        userId = userId,
                        routeRequest = msg.routeRequest
                    )
                    firestore.collection("chatHistory")
                        .document(msg.id)
                        .set(messageWithUser)
                        .await()
                }
                Log.d(TAG, "Saved messages to Firestore")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving conversation history to Firestore", e)
            }
        }
    }
    
    fun getConversationHistory(): List<ChatMessage> {
        return conversationHistory.toList()
    }
    
    suspend fun clearHistory() {
        withContext(Dispatchers.IO) {
            try {
                val userId = getCurrentUserId() ?: return@withContext
                
                val messages = firestore.collection("chatHistory")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()
                
                val batch = firestore.batch()
                messages.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }
                batch.commit().await()
                
                conversationHistory.clear()
                Log.d(TAG, "Cleared conversation history from Firestore")
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing conversation history", e)
            }
        }
    }
    
    // Quick suggestions for user
    fun getQuickSuggestions(): List<String> {
        return listOf(
            "Take me home",
            "Navigate to the nearest gas station",
            "Find parking near SM Mall",
            "Route to Makati via EDSA",
            "What's the fastest way to BGC?"
        )
    }
}
