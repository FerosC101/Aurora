package com.nextcs.aurora.navigation

import android.content.Context
import android.content.SharedPreferences

/**
 * Service for managing vehicle profile preferences
 * Uses SharedPreferences to store the user's preferred vehicle type
 */
class VehicleProfileService(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "aurora_vehicle_profile",
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val KEY_VEHICLE_TYPE = "vehicle_type"
        private const val DEFAULT_VEHICLE_TYPE = "driving"
        
        // Supported vehicle types for Google Directions API
        const val TYPE_DRIVING = "driving"
        const val TYPE_BICYCLING = "bicycling"
        const val TYPE_WALKING = "walking"
        const val TYPE_TRANSIT = "transit"
    }
    
    /**
     * Get the current vehicle type
     * @return Vehicle type (driving, bicycling, walking, transit)
     */
    fun getVehicleType(): String {
        return prefs.getString(KEY_VEHICLE_TYPE, DEFAULT_VEHICLE_TYPE) ?: DEFAULT_VEHICLE_TYPE
    }
    
    /**
     * Set the vehicle type
     * @param vehicleType Vehicle type (driving, bicycling, walking, transit)
     * @return Result indicating success or failure
     */
    fun setVehicleType(vehicleType: String): Result<Unit> {
        return try {
            // Validate vehicle type
            if (vehicleType !in listOf(TYPE_DRIVING, TYPE_BICYCLING, TYPE_WALKING, TYPE_TRANSIT)) {
                return Result.failure(IllegalArgumentException("Invalid vehicle type: $vehicleType"))
            }
            
            prefs.edit().apply {
                putString(KEY_VEHICLE_TYPE, vehicleType)
                apply()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get display name for vehicle type
     * @param vehicleType Vehicle type code
     * @return Human-readable vehicle type name
     */
    fun getVehicleDisplayName(vehicleType: String = getVehicleType()): String {
        return when (vehicleType) {
            TYPE_DRIVING -> "Car"
            TYPE_BICYCLING -> "Bicycle"
            TYPE_WALKING -> "Walking"
            TYPE_TRANSIT -> "Transit"
            else -> "Unknown"
        }
    }
    
    /**
     * Reset to default vehicle type
     */
    fun resetToDefault() {
        prefs.edit().clear().apply()
    }
}
