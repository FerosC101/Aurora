package com.nextcs.aurora.navigation

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TripRecord(
    val id: String = "",
    val userId: String = "",
    val origin: String = "",
    val destination: String = "",
    val distance: Int = 0,          // meters
    val duration: Int = 0,          // seconds
    val timestamp: Long = 0L,        // milliseconds since epoch
    val hazardsEncountered: Int = 0,
    val safetyScore: Int = 0,
    val routeType: String = "",      // "Smart", "Chill", "Regular"
    // Driving behavior metrics
    val harshBrakingCount: Int = 0,
    val rapidAccelerationCount: Int = 0,
    val speedingIncidents: Int = 0,
    val smoothDrivingScore: Int = 100  // 0-100, 100 being perfect
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
    
    private val TAG = "TripHistoryService"
    private val firestore = FirebaseFirestore.getInstance(FirebaseApp.getInstance(), "sfse")
    private val auth = FirebaseAuth.getInstance()
    private val tripsCollection = firestore.collection("trips")
    
    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
    
    suspend fun saveTrip(
        origin: String,
        destination: String,
        routeInfo: RouteInfo,
        hazards: List<DetectedHazard>,
        safetyScore: Int,
        routeType: String,
        harshBrakingCount: Int = 0,
        rapidAccelerationCount: Int = 0,
        speedingIncidents: Int = 0
    ) = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId() ?: return@withContext Result.failure<TripRecord>(Exception("User not logged in"))
            
            val tripId = firestore.collection("trips").document().id
            val trip = TripRecord(
                id = tripId,
                userId = userId,
                origin = origin,
                destination = destination,
                distance = routeInfo.distance,
                duration = routeInfo.duration,
                timestamp = System.currentTimeMillis(),
                hazardsEncountered = hazards.size,
                safetyScore = safetyScore,
                routeType = routeType,
                harshBrakingCount = harshBrakingCount,
                rapidAccelerationCount = rapidAccelerationCount,
                speedingIncidents = speedingIncidents,
                smoothDrivingScore = safetyScore
            )
            
            // Save to Firestore
            tripsCollection.document(tripId).set(trip).await()
            
            Log.d(TAG, "Trip saved successfully for user $userId")
            Result.success(trip)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving trip", e)
            Result.failure(e)
        }
    }
    
    suspend fun getAllTrips(): Result<List<TripRecord>> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId() ?: return@withContext Result.failure(Exception("User not logged in"))
            
            val trips = tripsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .toObjects(TripRecord::class.java)
                .sortedByDescending { it.timestamp }
            
            Log.d(TAG, "Retrieved ${trips.size} trips for user $userId")
            Result.success(trips)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting trips", e)
            Result.failure(e)
        }
    }
    
    suspend fun getAnalytics(): Result<Analytics> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId() ?: return@withContext Result.failure(Exception("User not logged in"))
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
            
            Log.d(TAG, "Analytics calculated for user $userId: $totalTrips trips")
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
            Log.e(TAG, "Error getting analytics", e)
            Result.failure(e)
        }
    }
    
    suspend fun getMonthlyStats(): Result<List<Pair<String, Int>>> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId() ?: return@withContext Result.failure(Exception("User not logged in"))
            val allTrips = getAllTrips().getOrNull() ?: emptyList()
            val dateFormat = SimpleDateFormat("MMM", Locale.getDefault())
            
            val monthlyTripCount = allTrips.groupBy { trip ->
                dateFormat.format(Date(trip.timestamp))
            }.mapValues { it.value.size }
            
            val result = monthlyTripCount.toList().sortedBy { it.first }
            Log.d(TAG, "Monthly stats calculated for user $userId")
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting monthly stats", e)
            Result.failure(e)
        }
    }
    
    suspend fun clearAllTrips() = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId() ?: return@withContext
            
            // Delete all trips for this user
            val trips = tripsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            val batch = firestore.batch()
            trips.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()
            
            Log.d(TAG, "Cleared all trips for user $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing trips", e)
        }
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
}
