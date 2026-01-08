package com.nextcs.aurora.analytics

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class TripCost(
    val tripId: String,
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
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "aurora_cost_tracking",
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val COSTS_KEY = "trip_costs"
        private const val FUEL_PRICE_KEY = "fuel_price_per_liter"
        private const val DEFAULT_FUEL_PRICE = 1.5 // Default price per liter
        
        // Average fuel consumption by vehicle type (L/100km)
        private const val CAR_FUEL_CONSUMPTION = 7.5
        private const val MOTORCYCLE_FUEL_CONSUMPTION = 3.5
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
            // Calculate fuel consumption and cost
            val fuelConsumption = calculateFuelConsumption(distanceKm, vehicleType)
            val fuelPrice = getFuelPrice()
            val fuelCost = fuelConsumption * fuelPrice
            
            val cost = TripCost(
                tripId = tripId,
                tollCost = tollCost,
                parkingCost = parkingCost,
                fuelConsumption = fuelConsumption,
                fuelCost = fuelCost,
                totalCost = tollCost + parkingCost + fuelCost,
                timestamp = System.currentTimeMillis()
            )
            
            // Get existing costs
            val costsJson = prefs.getString(COSTS_KEY, "[]") ?: "[]"
            val costsArray = JSONArray(costsJson)
            
            // Add new cost
            costsArray.put(costToJson(cost))
            
            // Save
            prefs.edit().putString(COSTS_KEY, costsArray.toString()).apply()
            
            Result.success(cost)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get cost for a specific trip
     */
    suspend fun getTripCost(tripId: String): Result<TripCost?> = withContext(Dispatchers.IO) {
        try {
            val costsJson = prefs.getString(COSTS_KEY, "[]") ?: "[]"
            val costsArray = JSONArray(costsJson)
            
            for (i in 0 until costsArray.length()) {
                val costObj = costsArray.getJSONObject(i)
                if (costObj.getString("tripId") == tripId) {
                    return@withContext Result.success(jsonToCost(costObj))
                }
            }
            
            Result.success(null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get all trip costs
     */
    suspend fun getAllCosts(): Result<List<TripCost>> = withContext(Dispatchers.IO) {
        try {
            val costsJson = prefs.getString(COSTS_KEY, "[]") ?: "[]"
            val costsArray = JSONArray(costsJson)
            
            val costs = mutableListOf<TripCost>()
            for (i in 0 until costsArray.length()) {
                costs.add(jsonToCost(costsArray.getJSONObject(i)))
            }
            
            Result.success(costs.sortedByDescending { it.timestamp })
        } catch (e: Exception) {
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
    fun getFuelPrice(): Double {
        return prefs.getFloat(FUEL_PRICE_KEY, DEFAULT_FUEL_PRICE.toFloat()).toDouble()
    }
    
    fun setFuelPrice(pricePerLiter: Double) {
        prefs.edit().putFloat(FUEL_PRICE_KEY, pricePerLiter.toFloat()).apply()
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
    
    private fun costToJson(cost: TripCost): JSONObject {
        return JSONObject().apply {
            put("tripId", cost.tripId)
            put("tollCost", cost.tollCost)
            put("parkingCost", cost.parkingCost)
            put("fuelConsumption", cost.fuelConsumption)
            put("fuelCost", cost.fuelCost)
            put("totalCost", cost.totalCost)
            put("timestamp", cost.timestamp)
        }
    }
    
    private fun jsonToCost(json: JSONObject): TripCost {
        return TripCost(
            tripId = json.getString("tripId"),
            tollCost = json.getDouble("tollCost"),
            parkingCost = json.getDouble("parkingCost"),
            fuelConsumption = json.getDouble("fuelConsumption"),
            fuelCost = json.getDouble("fuelCost"),
            totalCost = json.getDouble("totalCost"),
            timestamp = json.getLong("timestamp")
        )
    }
}
