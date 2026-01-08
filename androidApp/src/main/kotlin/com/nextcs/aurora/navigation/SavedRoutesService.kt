package com.nextcs.aurora.navigation

import android.content.Context
import android.content.SharedPreferences
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class SavedRouteRecord(
    val id: String,
    val name: String,
    val origin: String,
    val destination: String,
    val distance: Double,          // km
    val estimatedTime: Int,        // minutes
    val waypoints: List<LatLng> = emptyList(),
    val isFavorite: Boolean = false,
    val lastUsed: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)

class SavedRoutesService(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "aurora_saved_routes",
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val ROUTES_KEY = "saved_routes"
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
            val routeId = "route_${System.currentTimeMillis()}"
            val route = SavedRouteRecord(
                id = routeId,
                name = name,
                origin = origin,
                destination = destination,
                distance = distance,
                estimatedTime = estimatedTime,
                waypoints = waypoints,
                isFavorite = false,
                lastUsed = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis()
            )
            
            // Get existing routes
            val routesJson = sharedPreferences.getString(ROUTES_KEY, "[]") ?: "[]"
            val routesArray = JSONArray(routesJson)
            
            // Add new route
            routesArray.put(routeToJson(route))
            
            // Save back
            sharedPreferences.edit().putString(ROUTES_KEY, routesArray.toString()).apply()
            
            Result.success(route)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getAllRoutes(): Result<List<SavedRouteRecord>> = withContext(Dispatchers.IO) {
        try {
            val routesJson = sharedPreferences.getString(ROUTES_KEY, "[]") ?: "[]"
            val routesArray = JSONArray(routesJson)
            val routes = mutableListOf<SavedRouteRecord>()
            
            for (i in 0 until routesArray.length()) {
                val routeJson = routesArray.getJSONObject(i)
                routes.add(jsonToRoute(routeJson))
            }
            
            Result.success(routes.sortedByDescending { it.lastUsed })
        } catch (e: Exception) {
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
            val routes = getAllRoutes().getOrNull() ?: emptyList()
            val updatedRoutes = routes.map { route ->
                if (route.id == routeId) {
                    route.copy(isFavorite = isFavorite)
                } else {
                    route
                }
            }
            
            saveAllRoutes(updatedRoutes)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun markRouteAsUsed(routeId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val routes = getAllRoutes().getOrNull() ?: emptyList()
            val updatedRoutes = routes.map { route ->
                if (route.id == routeId) {
                    route.copy(lastUsed = System.currentTimeMillis())
                } else {
                    route
                }
            }
            
            saveAllRoutes(updatedRoutes)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteRoute(routeId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val routes = getAllRoutes().getOrNull() ?: emptyList()
            val updatedRoutes = routes.filter { it.id != routeId }
            
            saveAllRoutes(updatedRoutes)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun clearAllRoutes(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            sharedPreferences.edit().remove(ROUTES_KEY).apply()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun saveAllRoutes(routes: List<SavedRouteRecord>) {
        val jsonArray = JSONArray()
        for (route in routes) {
            jsonArray.put(routeToJson(route))
        }
        sharedPreferences.edit().putString(ROUTES_KEY, jsonArray.toString()).apply()
    }
    
    private fun routeToJson(route: SavedRouteRecord): JSONObject {
        return JSONObject().apply {
            put("id", route.id)
            put("name", route.name)
            put("origin", route.origin)
            put("destination", route.destination)
            put("distance", route.distance)
            put("estimatedTime", route.estimatedTime)
            put("isFavorite", route.isFavorite)
            put("lastUsed", route.lastUsed)
            put("createdAt", route.createdAt)
            
            // Waypoints array
            val waypointsArray = JSONArray()
            for (waypoint in route.waypoints) {
                val waypointObj = JSONObject()
                waypointObj.put("lat", waypoint.latitude)
                waypointObj.put("lng", waypoint.longitude)
                waypointsArray.put(waypointObj)
            }
            put("waypoints", waypointsArray)
        }
    }
    
    private fun jsonToRoute(json: JSONObject): SavedRouteRecord {
        val waypointsArray = json.optJSONArray("waypoints") ?: JSONArray()
        val waypoints = mutableListOf<LatLng>()
        
        for (i in 0 until waypointsArray.length()) {
            val waypointObj = waypointsArray.getJSONObject(i)
            waypoints.add(LatLng(
                waypointObj.getDouble("lat"),
                waypointObj.getDouble("lng")
            ))
        }
        
        return SavedRouteRecord(
            id = json.getString("id"),
            name = json.getString("name"),
            origin = json.getString("origin"),
            destination = json.getString("destination"),
            distance = json.getDouble("distance"),
            estimatedTime = json.getInt("estimatedTime"),
            waypoints = waypoints,
            isFavorite = json.optBoolean("isFavorite", false),
            lastUsed = json.optLong("lastUsed", 0),
            createdAt = json.optLong("createdAt", System.currentTimeMillis())
        )
    }
}
