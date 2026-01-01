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
    
    init {
        Log.d(TAG, "Initializing RouteAssistantService")
        Log.d(TAG, "API Key length: ${apiKey.length}")
        Log.d(TAG, "API Key starts with: ${apiKey.take(10)}...")
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
                3. Return route information in JSON format at the end
                
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
                {
                    "origin": "location name or 'current location'",
                    "destination": "location name",
                    "waypoints": ["waypoint1", "waypoint2"]
                }
                
                If the user's request is unclear, ask clarifying questions in detail.
                If they're just chatting, respond naturally and helpfully without the JSON.
                
                Always be conversational, informative, and helpful. Think like a local guide who knows the area well.
            """.trimIndent()
            
            val prompt = "$systemPrompt\n\nUser: $userMessage\n\nAssistant:"
            
            Log.d(TAG, "Calling Gemini API...")
            val response = model.generateContent(prompt)
            val aiText = response.text ?: "I'm having trouble understanding. Could you rephrase that?"
            
            Log.d(TAG, "Gemini response received: ${aiText.take(100)}...")
            
            // Parse response for route information
            val routeRequest = parseRouteFromResponse(aiText)
            
            // Clean AI response (remove JSON if present)
            val cleanedText = aiText.split("ROUTE_JSON:")[0].trim()
            
            val aiMessage = ChatMessage(
                text = cleanedText,
                isUser = false,
                routeRequest = routeRequest
            )
            
            conversationHistory.add(aiMessage)
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
            errorMessage
        }
    }
    
    private fun parseRouteFromResponse(response: String): RouteRequest? {
        return try {
            if (!response.contains("ROUTE_JSON:")) return null
            
            val jsonPart = response.split("ROUTE_JSON:")[1].trim()
            val json = JSONObject(jsonPart)
            
            val origin = json.optString("origin", "")
            val destination = json.optString("destination", "")
            val waypointsArray = json.optJSONArray("waypoints")
            
            val waypoints = mutableListOf<String>()
            waypointsArray?.let {
                for (i in 0 until it.length()) {
                    waypoints.add(it.getString(i))
                }
            }
            
            if (origin.isNotEmpty() && destination.isNotEmpty()) {
                RouteRequest(origin, destination, waypoints)
            } else null
            
        } catch (e: Exception) {
            null
        }
    }
    
    fun getConversationHistory(): List<ChatMessage> {
        return conversationHistory.toList()
    }
    
    fun clearHistory() {
        conversationHistory.clear()
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
