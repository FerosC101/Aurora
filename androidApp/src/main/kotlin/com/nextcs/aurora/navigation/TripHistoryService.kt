package com.nextcs.aurora.navigation

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TripRecord(
    val id: String,
    val origin: String,
    val destination: String,
    val distance: Int,          // meters
    val duration: Int,          // seconds
    val timestamp: Long,        // milliseconds since epoch
    val hazardsEncountered: Int,
    val safetyScore: Int,
    val routeType: String       // "Smart", "Chill", "Regular"
)

data class Analytics(
    val totalTrips: Int,
    val totalDistance: Double,  // km
    val totalDuration: Double,  // hours
    val hazardsAvoided: Int,
    val averageSafetyScore: Int,
    val timeSavedThisMonth: Double, // hours
    val totalTimeSaved: Double  // hours
)

class TripHistoryService(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "aurora_trip_history",
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val TRIPS_KEY = "trips"
        private const val TIME_SAVED_KEY = "time_saved"
        private const val HAZARDS_AVOIDED_KEY = "hazards_avoided"
    }
    
    suspend fun saveTrip(
        origin: String,
        destination: String,
        routeInfo: RouteInfo,
        hazards: List<DetectedHazard>,
        safetyScore: Int,
        routeType: String
    ) = withContext(Dispatchers.IO) {
        try {
            val tripId = "trip_${System.currentTimeMillis()}"
            val trip = TripRecord(
                id = tripId,
                origin = origin,
                destination = destination,
                distance = routeInfo.distance,
                duration = routeInfo.duration,
                timestamp = System.currentTimeMillis(),
                hazardsEncountered = hazards.size,
                safetyScore = safetyScore,
                routeType = routeType
            )
            
            // Get existing trips
            val tripsJson = sharedPreferences.getString(TRIPS_KEY, "[]") ?: "[]"
            val tripsArray = JSONArray(tripsJson)
            
            // Add new trip
            tripsArray.put(tripToJson(trip))
            
            // Save back
            sharedPreferences.edit().putString(TRIPS_KEY, tripsArray.toString()).apply()
            
            // Update cumulative stats
            val hazardsAvoided = sharedPreferences.getInt(HAZARDS_AVOIDED_KEY, 0)
            sharedPreferences.edit()
                .putInt(HAZARDS_AVOIDED_KEY, hazardsAvoided + hazards.size)
                .apply()
            
            Result.success(trip)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getAllTrips(): Result<List<TripRecord>> = withContext(Dispatchers.IO) {
        try {
            val tripsJson = sharedPreferences.getString(TRIPS_KEY, "[]") ?: "[]"
            val tripsArray = JSONArray(tripsJson)
            val trips = mutableListOf<TripRecord>()
            
            for (i in 0 until tripsArray.length()) {
                val tripJson = tripsArray.getJSONObject(i)
                trips.add(jsonToTrip(tripJson))
            }
            
            Result.success(trips.sortedByDescending { it.timestamp })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getAnalytics(): Result<Analytics> = withContext(Dispatchers.IO) {
        try {
            val allTrips = getAllTrips().getOrNull() ?: emptyList()
            
            val totalTrips = allTrips.size
            val totalDistance = allTrips.sumOf { it.distance } / 1000.0
            val totalDuration = allTrips.sumOf { it.duration } / 3600.0
            val hazardsAvoided = allTrips.sumOf { it.hazardsEncountered }
            val averageSafetyScore = if (allTrips.isNotEmpty()) {
                allTrips.map { it.safetyScore }.average().toInt()
            } else 0
            
            // Calculate time saved (comparing routes)
            val totalTimeSaved = calculateTimeSaved(allTrips)
            val timeSavedThisMonth = calculateTimeSavedThisMonth(allTrips)
            
            Result.success(
                Analytics(
                    totalTrips = totalTrips,
                    totalDistance = totalDistance,
                    totalDuration = totalDuration,
                    hazardsAvoided = hazardsAvoided,
                    averageSafetyScore = averageSafetyScore,
                    timeSavedThisMonth = timeSavedThisMonth,
                    totalTimeSaved = totalTimeSaved
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getMonthlyStats(): Result<List<Pair<String, Int>>> = withContext(Dispatchers.IO) {
        try {
            val allTrips = getAllTrips().getOrNull() ?: emptyList()
            val dateFormat = SimpleDateFormat("MMM", Locale.getDefault())
            
            val monthlyTripCount = allTrips.groupBy { trip ->
                dateFormat.format(Date(trip.timestamp))
            }.mapValues { it.value.size }
            
            val result = monthlyTripCount.toList().sortedBy { it.first }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun clearAllTrips() = withContext(Dispatchers.IO) {
        sharedPreferences.edit()
            .remove(TRIPS_KEY)
            .remove(TIME_SAVED_KEY)
            .remove(HAZARDS_AVOIDED_KEY)
            .apply()
    }
    
    private fun calculateTimeSaved(trips: List<TripRecord>): Double {
        // Estimate: Smart routes typically save 10-15% vs regular routes
        return trips.filter { it.routeType == "Smart" }
            .sumOf { (it.duration * 0.12) / 3600.0 }  // 12% time savings
    }
    
    private fun calculateTimeSavedThisMonth(trips: List<TripRecord>): Double {
        val now = System.currentTimeMillis()
        val monthAgo = now - (30 * 24 * 60 * 60 * 1000)
        
        return trips.filter { it.timestamp > monthAgo && it.routeType == "Smart" }
            .sumOf { (it.duration * 0.12) / 3600.0 }
    }
    
    private fun tripToJson(trip: TripRecord): JSONObject {
        return JSONObject().apply {
            put("id", trip.id)
            put("origin", trip.origin)
            put("destination", trip.destination)
            put("distance", trip.distance)
            put("duration", trip.duration)
            put("timestamp", trip.timestamp)
            put("hazards", trip.hazardsEncountered)
            put("safety_score", trip.safetyScore)
            put("route_type", trip.routeType)
        }
    }
    
    private fun jsonToTrip(json: JSONObject): TripRecord {
        return TripRecord(
            id = json.getString("id"),
            origin = json.getString("origin"),
            destination = json.getString("destination"),
            distance = json.getInt("distance"),
            duration = json.getInt("duration"),
            timestamp = json.getLong("timestamp"),
            hazardsEncountered = json.getInt("hazards"),
            safetyScore = json.getInt("safety_score"),
            routeType = json.getString("route_type")
        )
    }
}
