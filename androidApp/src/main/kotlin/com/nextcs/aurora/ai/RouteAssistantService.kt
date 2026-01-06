package com.nextcs.aurora.ai

import android.content.Context
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class RouteRequest(
    val origin: String,
    val destination: String,
    val waypoints: List<String> = emptyList()
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
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
    private val sharedPrefs = context.getSharedPreferences("ai_chat_history", Context.MODE_PRIVATE)
    private val CHAT_HISTORY_KEY = "chat_messages"
    
    init {
        Log.d(TAG, "Initializing RouteAssistantService")
        Log.d(TAG, "API Key length: ${apiKey.length}")
        Log.d(TAG, "API Key starts with: ${apiKey.take(10)}...")
        // Load saved conversation history
        loadConversationHistory()
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
        // Add user message to history
        val userChatMessage = ChatMessage(text = userMessage, isUser = true)
        conversationHistory.add(userChatMessage)
        saveConversationHistory()
        
        Log.d(TAG, "Sending message to Gemini: $userMessage")
        
        try {
            // Build context-aware prompt
            val systemPrompt = """
                You are Aurora, a friendly and detailed navigation assistant. Help users plan their routes with comprehensive information.
                
                When users ask for directions or routes:
                1. Extract origin, destination, and any waypoints
                2. Provide a DETAILED response that includes:
                   - A friendly greeting/acknowledgment
                   - The full route description (origin → waypoints → destination)
                   - Estimated distance and travel time (you can estimate based on typical routes)
                   - Suggested route preferences (fastest, avoid tolls, scenic, etc.)
                   - Any relevant tips (traffic patterns, best times to travel, landmarks to look for)
                   - Alternative route suggestions if applicable
                3. ALWAYS end with route JSON on a new line
                
                Format your response as:
                [Your detailed, friendly message with route information]
                
                Example detailed response:
                "Great! I'll help you navigate to SM Mall Makati. 
                
                📍 Route Overview:
                • Starting Point: Your Current Location
                • Destination: SM Mall Makati
                • Estimated Distance: ~8.5 km
                • Estimated Time: 25-30 minutes (depending on traffic)
                
                🛣️ Recommended Route:
                I recommend taking EDSA for the fastest route. Here's what you'll do:
                1. Head towards EDSA via the nearest access point
                2. Take EDSA Southbound
                3. Exit at Ayala Avenue
                4. Turn right onto Makati Avenue
                5. SM Mall Makati will be on your right
                
                💡 Travel Tips:
                • Best time: Avoid rush hours (7-9 AM, 5-7 PM)
                • Alternative: You can take Buendia Avenue if EDSA is congested
                • Parking: SM has basement parking - entrance on Makati Avenue
                
                Ready to start navigation?"
                
                ROUTE_JSON:
                {"origin": "current location", "destination": "SM Mall Makati", "waypoints": []}
                
                CRITICAL: For ANY navigation/route request, you MUST include the ROUTE_JSON line.
                If user says "Take me to X" or "Navigate to Y" or "Go to Z", you MUST include ROUTE_JSON.
                If the user's request is unclear, ask clarifying questions but still include ROUTE_JSON if you can infer the destination.
                If they're just chatting (no navigation intent), respond naturally without the JSON.
                
                Always be conversational, informative, and helpful. Think like a local guide who knows the area well.
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
                text = formattedText,
                isUser = false,
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
                text = errorText,
                isUser = false
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
            waypointsArray?.let {
                for (i in 0 until it.length()) {
                    val waypoint = it.getString(i).trim()
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
    
    private fun loadConversationHistory() {
        try {
            val json = sharedPrefs.getString(CHAT_HISTORY_KEY, null) ?: return
            val jsonArray = JSONArray(json)
            conversationHistory.clear()
            
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val message = ChatMessage(
                    id = obj.getString("id"),
                    text = obj.getString("text"),
                    isUser = obj.getBoolean("isUser"),
                    timestamp = obj.getLong("timestamp"),
                    routeRequest = if (obj.has("routeRequest") && !obj.isNull("routeRequest")) {
                        val routeObj = obj.getJSONObject("routeRequest")
                        RouteRequest(
                            origin = routeObj.getString("origin"),
                            destination = routeObj.getString("destination"),
                            waypoints = if (routeObj.has("waypoints")) {
                                val wpArray = routeObj.getJSONArray("waypoints")
                                (0 until wpArray.length()).map { wpArray.getString(it) }
                            } else emptyList()
                        )
                    } else null
                )
                conversationHistory.add(message)
            }
            Log.d(TAG, "Loaded ${conversationHistory.size} messages from history")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading conversation history", e)
            conversationHistory.clear()
        }
    }
    
    private fun saveConversationHistory() {
        try {
            val jsonArray = JSONArray()
            for (msg in conversationHistory) {
                val obj = JSONObject()
                obj.put("id", msg.id)
                obj.put("text", msg.text)
                obj.put("isUser", msg.isUser)
                obj.put("timestamp", msg.timestamp)
                
                if (msg.routeRequest != null) {
                    val routeObj = JSONObject()
                    routeObj.put("origin", msg.routeRequest.origin)
                    routeObj.put("destination", msg.routeRequest.destination)
                    val waypointsArray = JSONArray(msg.routeRequest.waypoints)
                    routeObj.put("waypoints", waypointsArray)
                    obj.put("routeRequest", routeObj)
                }
                
                jsonArray.put(obj)
            }
            
            sharedPrefs.edit().putString(CHAT_HISTORY_KEY, jsonArray.toString()).apply()
            Log.d(TAG, "Saved ${conversationHistory.size} messages to history")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving conversation history", e)
        }
    }
    
    fun getConversationHistory(): List<ChatMessage> {
        return conversationHistory.toList()
    }
    
    fun clearHistory() {
        conversationHistory.clear()
        sharedPrefs.edit().remove(CHAT_HISTORY_KEY).apply()
        Log.d(TAG, "Cleared conversation history")
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
