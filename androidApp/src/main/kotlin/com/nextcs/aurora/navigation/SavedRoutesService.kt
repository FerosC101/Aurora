package com.nextcs.aurora.navigation

import android.content.Context
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class WaypointData(
    val lat: Double = 0.0,
    val lng: Double = 0.0
)

data class SavedRouteRecord(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val origin: String = "",
    val destination: String = "",
    val distance: Double = 0.0,          // km
    val estimatedTime: Int = 0,        // minutes
    val waypoints: List<WaypointData> = emptyList(),
    val isFavorite: Boolean = false,
    val lastUsed: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toLatLngWaypoints(): List<LatLng> {
        return waypoints.map { LatLng(it.lat, it.lng) }
    }
}

class SavedRoutesService(private val context: Context) {
    
    private val firestore = FirebaseFirestore.getInstance(FirebaseApp.getInstance(), "sfse")
    private val routesCollection = firestore.collection("savedRoutes")
    
    companion object {
        private const val TAG = "SavedRoutesService"
    }
    
    private fun getCurrentUserId(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }
    
    suspend fun saveRoute(
        name: String,
        origin: String,
        destination: String,
        distance: Double,
        estimatedTime: Int,
        waypoints: List<LatLng> = emptyList()
    ): Result<SavedRouteRecord> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId() ?: return@withContext Result.failure(Exception("User not logged in"))
            
            val routeId = "route_${System.currentTimeMillis()}"
            val waypointData = waypoints.map { WaypointData(it.latitude, it.longitude) }
            
            val route = SavedRouteRecord(
                id = routeId,
                userId = userId,
                name = name,
                origin = origin,
                destination = destination,
                distance = distance,
                estimatedTime = estimatedTime,
                waypoints = waypointData,
                isFavorite = false,
                lastUsed = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis()
            )
            
            // Save to Firestore
            routesCollection.document(routeId).set(route).await()
            Log.d(TAG, "Saved route $routeId (user: $userId)")
            
            Result.success(route)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving route", e)
            Result.failure(e)
        }
    }
    
    suspend fun getAllRoutes(): Result<List<SavedRouteRecord>> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId() ?: return@withContext Result.failure(Exception("User not logged in"))
            
            val routes = routesCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .toObjects(SavedRouteRecord::class.java)
                .sortedByDescending { it.lastUsed }
            
            Log.d(TAG, "Retrieved ${routes.size} routes for user $userId")
            Result.success(routes)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting routes", e)
            Result.failure(e)
        }
    }
    
    suspend fun getFavoriteRoutes(): Result<List<SavedRouteRecord>> = withContext(Dispatchers.IO) {
        try {
            val allRoutes = getAllRoutes().getOrNull() ?: emptyList()
            Result.success(allRoutes.filter { it.isFavorite }.sortedByDescending { it.lastUsed })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun toggleFavorite(routeId: String, isFavorite: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId() ?: return@withContext Result.failure(Exception("User not logged in"))
            
            routesCollection.document(routeId)
                .update("isFavorite", isFavorite)
                .await()
            
            Log.d(TAG, "Toggled favorite for route $routeId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling favorite", e)
            Result.failure(e)
        }
    }
    
    suspend fun markRouteAsUsed(routeId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId() ?: return@withContext Result.failure(Exception("User not logged in"))
            
            routesCollection.document(routeId)
                .update("lastUsed", System.currentTimeMillis())
                .await()
            
            Log.d(TAG, "Marked route $routeId as used")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking route as used", e)
            Result.failure(e)
        }
    }
    
    suspend fun deleteRoute(routeId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId() ?: return@withContext Result.failure(Exception("User not logged in"))
            
            routesCollection.document(routeId).delete().await()
            Log.d(TAG, "Deleted route $routeId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting route", e)
            Result.failure(e)
        }
    }
    
    suspend fun clearAllRoutes(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId() ?: return@withContext Result.failure(Exception("User not logged in"))
            
            val routes = routesCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            val batch = firestore.batch()
            routes.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()
            
            Log.d(TAG, "Cleared ${routes.size()} routes for user $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing routes", e)
            Result.failure(e)
        }
    }
}
