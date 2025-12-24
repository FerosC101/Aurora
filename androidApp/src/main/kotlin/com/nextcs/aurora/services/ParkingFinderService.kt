package com.nextcs.aurora.services

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

data class ParkingSpot(
    val id: String,
    val name: String,
    val location: LatLng,
    val address: String,
    val rating: Double,
    val distanceFromDestination: Double, // meters
    val priceLevel: Int, // 0-4
    val isOpenNow: Boolean,
    val types: List<String>
)

class ParkingFinderService(private val context: Context) {
    
    companion object {
        private const val PLACES_API_BASE = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
        private const val DETAILS_API_BASE = "https://maps.googleapis.com/maps/api/place/details/json"
        private const val SEARCH_RADIUS = 1000 // 1km
    }
    
    private fun getApiKey(): String {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(
                context.packageName,
                android.content.pm.PackageManager.GET_META_DATA
            )
            appInfo.metaData?.getString("com.google.android.geo.API_KEY") ?: ""
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * Find parking spots near destination
     */
    suspend fun findParkingNearDestination(
        destination: LatLng,
        radius: Int = SEARCH_RADIUS
    ): Result<List<ParkingSpot>> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getApiKey()
            if (apiKey.isEmpty()) {
                return@withContext Result.failure(Exception("API key not found"))
            }
            
            val location = "${destination.latitude},${destination.longitude}"
            val url = "$PLACES_API_BASE?location=$location&radius=$radius&type=parking&key=$apiKey"
            
            val response = URL(url).readText()
            val json = JSONObject(response)
            
            if (json.getString("status") != "OK") {
                // Check for ZERO_RESULTS
                if (json.getString("status") == "ZERO_RESULTS") {
                    return@withContext Result.success(emptyList())
                }
                val errorMessage = json.optString("error_message", "Unknown error")
                return@withContext Result.failure(Exception("Places API error: $errorMessage"))
            }
            
            val results = json.getJSONArray("results")
            val parkingSpots = mutableListOf<ParkingSpot>()
            
            for (i in 0 until results.length()) {
                val place = results.getJSONObject(i)
                
                val placeId = place.getString("place_id")
                val name = place.getString("name")
                val address = place.optString("vicinity", "")
                val rating = place.optDouble("rating", 0.0)
                val priceLevel = place.optInt("price_level", 0)
                
                val loc = place.getJSONObject("geometry").getJSONObject("location")
                val parkingLocation = LatLng(loc.getDouble("lat"), loc.getDouble("lng"))
                
                val isOpenNow = place.optJSONObject("opening_hours")
                    ?.optBoolean("open_now", true) ?: true
                
                // Parse types
                val typesArray = place.optJSONArray("types")
                val types = mutableListOf<String>()
                typesArray?.let {
                    for (j in 0 until it.length()) {
                        types.add(it.getString(j))
                    }
                }
                
                // Calculate distance from destination
                val distance = calculateDistance(destination, parkingLocation)
                
                parkingSpots.add(
                    ParkingSpot(
                        id = placeId,
                        name = name,
                        location = parkingLocation,
                        address = address,
                        rating = rating,
                        distanceFromDestination = distance,
                        priceLevel = priceLevel,
                        isOpenNow = isOpenNow,
                        types = types
                    )
                )
            }
            
            // Sort by distance
            Result.success(parkingSpots.sortedBy { it.distanceFromDestination })
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get detailed parking information
     */
    suspend fun getParkingDetails(placeId: String): Result<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getApiKey()
            if (apiKey.isEmpty()) {
                return@withContext Result.failure(Exception("API key not found"))
            }
            
            val url = "$DETAILS_API_BASE?place_id=$placeId&fields=formatted_phone_number,opening_hours,website,photos&key=$apiKey"
            
            val response = URL(url).readText()
            val json = JSONObject(response)
            
            if (json.getString("status") != "OK") {
                return@withContext Result.failure(Exception("Details API error"))
            }
            
            val result = json.getJSONObject("result")
            val details = mutableMapOf<String, Any>()
            
            result.optString("formatted_phone_number")?.let { details["phone"] = it }
            result.optString("website")?.let { details["website"] = it }
            
            Result.success(details)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun calculateDistance(point1: LatLng, point2: LatLng): Double {
        val earthRadius = 6371000.0 // meters
        
        val lat1Rad = Math.toRadians(point1.latitude)
        val lat2Rad = Math.toRadians(point2.latitude)
        val deltaLat = Math.toRadians(point2.latitude - point1.latitude)
        val deltaLng = Math.toRadians(point2.longitude - point1.longitude)
        
        val a = kotlin.math.sin(deltaLat / 2) * kotlin.math.sin(deltaLat / 2) +
                kotlin.math.cos(lat1Rad) * kotlin.math.cos(lat2Rad) *
                kotlin.math.sin(deltaLng / 2) * kotlin.math.sin(deltaLng / 2)
        
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        
        return earthRadius * c
    }
    
    fun formatDistance(meters: Double): String {
        return when {
            meters < 1000 -> "${meters.toInt()}m away"
            else -> "${"%.1f".format(meters / 1000)}km away"
        }
    }
    
    fun getPriceLevelDescription(level: Int): String {
        return when (level) {
            0 -> "Free"
            1 -> "₱"
            2 -> "₱₱"
            3 -> "₱₱₱"
            4 -> "₱₱₱₱"
            else -> "Price unknown"
        }
    }
}
