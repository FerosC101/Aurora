package com.nextcs.aurora.repository

import kotlinx.coroutines.flow.Flow
import com.nextcs.aurora.database.SavedRouteDao
import com.nextcs.aurora.database.SavedRouteEntity
import com.nextcs.aurora.database.WaypointData

class SavedRoutesRepository(private val dao: SavedRouteDao) {
    
    val allRoutes: Flow<List<SavedRouteEntity>> = dao.getAllRoutes()
    
    val favoriteRoutes: Flow<List<SavedRouteEntity>> = dao.getFavoriteRoutes()
    
    suspend fun getRouteById(routeId: Long): SavedRouteEntity? {
        return dao.getRouteById(routeId)
    }
    
    suspend fun saveRoute(
        name: String,
        origin: String,
        destination: String,
        distance: Double,
        estimatedTime: Int,
        waypoints: List<WaypointData> = emptyList()
    ): Long {
        val route = SavedRouteEntity(
            name = name,
            origin = origin,
            destination = destination,
            distance = distance,
            estimatedTime = estimatedTime,
            waypoints = waypoints,
            createdAt = System.currentTimeMillis()
        )
        return dao.insertRoute(route)
    }
    
    suspend fun updateRoute(route: SavedRouteEntity) {
        dao.updateRoute(route)
    }
    
    suspend fun deleteRoute(route: SavedRouteEntity) {
        dao.deleteRoute(route)
    }
    
    suspend fun deleteRouteById(routeId: Long) {
        dao.deleteRouteById(routeId)
    }
    
    suspend fun toggleFavorite(routeId: Long, isFavorite: Boolean) {
        dao.toggleFavorite(routeId, isFavorite)
    }
    
    suspend fun markRouteAsUsed(routeId: Long) {
        dao.updateLastUsed(routeId, System.currentTimeMillis())
    }
}
