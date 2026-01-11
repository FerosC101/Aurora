package com.nextcs.aurora.navigation

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class VehicleProfile(
    val userId: String = "",
    val vehicleType: String = "driving"
)

/**
 * Service for managing vehicle profile preferences
 * Uses Firestore to store the user's preferred vehicle type
 */
class VehicleProfileService(private val context: Context) {
    
    private val firestore = FirebaseFirestore.getInstance(FirebaseApp.getInstance(), "sfse")
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "VehicleProfileService"
    
    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
    
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
    suspend fun getVehicleType(): String = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId() ?: return@withContext DEFAULT_VEHICLE_TYPE
            
            val doc = firestore.collection("vehicleProfiles")
                .document(userId)
                .get()
                .await()
            
            val profile = doc.toObject(VehicleProfile::class.java)
            profile?.vehicleType ?: DEFAULT_VEHICLE_TYPE
        } catch (e: Exception) {
            Log.e(TAG, "Error getting vehicle type", e)
            DEFAULT_VEHICLE_TYPE
        }
    }
    
    /**
     * Set the vehicle type
     * @param vehicleType Vehicle type (driving, bicycling, walking, transit)
     * @return Result indicating success or failure
     */
    suspend fun setVehicleType(vehicleType: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Validate vehicle type
            if (vehicleType !in listOf(TYPE_DRIVING, TYPE_BICYCLING, TYPE_WALKING, TYPE_TRANSIT)) {
                return@withContext Result.failure(IllegalArgumentException("Invalid vehicle type: $vehicleType"))
            }
            
            val userId = getCurrentUserId() ?: return@withContext Result.failure(Exception("User not logged in"))
            
            val profile = VehicleProfile(
                userId = userId,
                vehicleType = vehicleType
            )
            
            firestore.collection("vehicleProfiles")
                .document(userId)
                .set(profile)
                .await()
            
            Log.d(TAG, "Vehicle type saved: $vehicleType")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting vehicle type", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get display name for vehicle type
     * @param vehicleType Vehicle type code
     * @return Human-readable vehicle type name
     */
    fun getVehicleDisplayName(vehicleType: String): String {
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
    suspend fun resetToDefault() = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId() ?: return@withContext
            
            firestore.collection("vehicleProfiles")
                .document(userId)
                .delete()
                .await()
            
            Log.d(TAG, "Vehicle profile reset to default")
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting vehicle profile", e)
        }
    }
}
