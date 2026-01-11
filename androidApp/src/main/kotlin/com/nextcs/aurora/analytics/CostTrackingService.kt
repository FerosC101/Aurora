package com.nextcs.aurora.analytics

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class TripCost(
    val tripId: String = "",
    val userId: String = "",
    val tollCost: Double = 0.0,           // Currency
    val parkingCost: Double = 0.0,        // Currency
    val fuelConsumption: Double = 0.0,    // Liters
    val fuelCost: Double = 0.0,           // Currency
    val totalCost: Double = 0.0,          // Currency
    val timestamp: Long = System.currentTimeMillis()
)

data class MonthlyCostSummary(
    val month: String,                     // "Jan 2026"
    val totalTripCost: Double,
    val totalTollCost: Double,
    val totalParkingCost: Double,
    val totalFuelCost: Double,
    val totalTrips: Int,
    val averageCostPerTrip: Double
)

class CostTrackingService(private val context: Context) {
    
    private val firestore = FirebaseFirestore.getInstance(FirebaseApp.getInstance(), "sfse")
    private val costsCollection = firestore.collection("costs")
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "aurora_cost_tracking",
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val TAG = "CostTrackingService"
        private const val FUEL_PRICE_KEY = "fuel_price_per_liter"
        private const val DEFAULT_FUEL_PRICE = 1.5 // Default price per liter
        
        // Average fuel consumption by vehicle type (L/100km)
        private const val CAR_FUEL_CONSUMPTION = 7.5
        private const val MOTORCYCLE_FUEL_CONSUMPTION = 3.5
    }
    
    private fun getCurrentUserId(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }
    
    /**
     * Save cost data for a trip
     */
    suspend fun saveTripCost(
        tripId: String,
        tollCost: Double = 0.0,
        parkingCost: Double = 0.0,
        distanceKm: Double,
        vehicleType: String = "driving"
    ): Result<TripCost> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId() ?: return@withContext Result.failure(Exception("User not logged in"))
            
            // Calculate fuel consumption and cost
            val fuelConsumption = calculateFuelConsumption(distanceKm, vehicleType)
            val fuelPrice = getFuelPrice()
            val fuelCost = fuelConsumption * fuelPrice
            
            val cost = TripCost(
                tripId = tripId,
                userId = userId,
                tollCost = tollCost,
                parkingCost = parkingCost,
                fuelConsumption = fuelConsumption,
                fuelCost = fuelCost,
                totalCost = tollCost + parkingCost + fuelCost,
                timestamp = System.currentTimeMillis()
            )
            
            // Save to Firestore
            costsCollection.document(tripId).set(cost).await()
            Log.d(TAG, "Saved cost for trip $tripId (user: $userId)")
            
            Result.success(cost)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving trip cost", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get cost for a specific trip
     */
    suspend fun getTripCost(tripId: String): Result<TripCost?> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId() ?: return@withContext Result.failure(Exception("User not logged in"))
            
            val doc = costsCollection.document(tripId).get().await()
            val cost = doc.toObject(TripCost::class.java)
            
            // Verify it belongs to the current user
            if (cost?.userId == userId) {
                Result.success(cost)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting trip cost", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get all trip costs
     */
    suspend fun getAllCosts(): Result<List<TripCost>> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId() ?: return@withContext Result.failure(Exception("User not logged in"))
            
            val costs = costsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .toObjects(TripCost::class.java)
                .sortedByDescending { it.timestamp }
            
            Log.d(TAG, "Retrieved ${costs.size} costs for user $userId")
            Result.success(costs)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all costs", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get monthly cost summary
     */
    suspend fun getMonthlySummary(): Result<List<MonthlyCostSummary>> = withContext(Dispatchers.IO) {
        try {
            val costs = getAllCosts().getOrNull() ?: emptyList()
            
            // Group by month
            val monthlyGroups = costs.groupBy { cost ->
                val date = java.util.Date(cost.timestamp)
                val format = java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.getDefault())
                format.format(date)
            }
            
            val summaries = monthlyGroups.map { (month, costsList) ->
                MonthlyCostSummary(
                    month = month,
                    totalTripCost = costsList.sumOf { it.totalCost },
                    totalTollCost = costsList.sumOf { it.tollCost },
                    totalParkingCost = costsList.sumOf { it.parkingCost },
                    totalFuelCost = costsList.sumOf { it.fuelCost },
                    totalTrips = costsList.size,
                    averageCostPerTrip = if (costsList.isNotEmpty()) 
                        costsList.sumOf { it.totalCost } / costsList.size 
                        else 0.0
                )
            }
            
            Result.success(summaries.sortedByDescending { it.month })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Calculate fuel consumption based on distance and vehicle type
     */
    private fun calculateFuelConsumption(distanceKm: Double, vehicleType: String): Double {
        val consumptionPer100km = when (vehicleType.lowercase()) {
            "bicycling", "walking", "transit" -> 0.0
            else -> CAR_FUEL_CONSUMPTION
        }
        
        return (distanceKm / 100.0) * consumptionPer100km
    }
    
    /**
     * Get/Set fuel price per liter
     */
    suspend fun getFuelPrice(): Double = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId() ?: return@withContext DEFAULT_FUEL_PRICE
            
            val doc = firestore.collection("userSettings")
                .document(userId)
                .get()
                .await()
            
            doc.getDouble("fuelPrice") ?: DEFAULT_FUEL_PRICE
        } catch (e: Exception) {
            Log.e(TAG, "Error getting fuel price, using default", e)
            DEFAULT_FUEL_PRICE
        }
    }
    
    suspend fun setFuelPrice(pricePerLiter: Double) = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId() ?: return@withContext
            
            firestore.collection("userSettings")
                .document(userId)
                .set(mapOf("fuelPrice" to pricePerLiter))
                .await()
            
            Log.d(TAG, "Fuel price saved: $pricePerLiter")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting fuel price", e)
        }
    }
    
    /**
     * Estimate toll cost based on route (placeholder - would integrate with toll API)
     */
    fun estimateTollCost(distanceKm: Double, hasTolls: Boolean): Double {
        // Placeholder logic - in production, integrate with toll calculation API
        return if (hasTolls) {
            (distanceKm / 10.0) * 0.5 // Rough estimate: $0.50 per 10km on toll roads
        } else {
            0.0
        }
    }
    
}
