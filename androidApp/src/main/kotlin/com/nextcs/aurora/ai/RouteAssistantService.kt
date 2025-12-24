package com.nextcs.aurora.ai

import android.content.Context
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
    
    private val apiKey = getGeminiApiKey()
    private val model = GenerativeModel(
        modelName = "gemini-pro",
        apiKey = apiKey
    )
    
    private val conversationHistory = mutableListOf<ChatMessage>()
    
    private fun getGeminiApiKey(): String {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(
                context.packageName,
                android.content.pm.PackageManager.GET_META_DATA
            )
            appInfo.metaData?.getString("com.google.android.geo.GEMINI_API_KEY") ?: ""
        } catch (e: Exception) {
            ""
        }
    }
    
    suspend fun sendMessage(userMessage: String): ChatMessage = withContext(Dispatchers.IO) {
        // Add user message to history
        val userChatMessage = ChatMessage(text = userMessage, isUser = true)
        conversationHistory.add(userChatMessage)
        
        try {
            // Build context-aware prompt
            val systemPrompt = """
                You are Aurora, a friendly navigation assistant. Help users plan their routes.
                
                When users ask for directions or routes:
                1. Extract origin, destination, and any waypoints
                2. Respond in a conversational way
                3. Return route information in JSON format at the end
                
                Format your response as:
                [Your friendly message to the user]
                
                ROUTE_JSON:
                {
                    "origin": "location name",
                    "destination": "location name",
                    "waypoints": ["waypoint1", "waypoint2"]
                }
                
                If the user's request is unclear, ask clarifying questions.
                If they're just chatting, respond naturally without the JSON.
                
                Examples:
                - "Take me to SM Mall" → origin: current location, destination: SM Mall
                - "Go to Makati then BGC" → origin: current, destination: BGC, waypoints: [Makati]
                - "Navigate to Quezon City via EDSA" → include EDSA as preference
            """.trimIndent()
            
            val prompt = "$systemPrompt\n\nUser: $userMessage\n\nAssistant:"
            
            val response = model.generateContent(prompt)
            val aiText = response.text ?: "I'm having trouble understanding. Could you rephrase that?"
            
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
            val errorMessage = ChatMessage(
                text = "I'm having trouble connecting right now. Please try again.",
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
