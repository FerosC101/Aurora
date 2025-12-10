package org.aurora.android.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedRouteDao {
    @Query("SELECT * FROM saved_routes ORDER BY isFavorite DESC, lastUsed DESC, createdAt DESC")
    fun getAllRoutes(): Flow<List<SavedRouteEntity>>
    
    @Query("SELECT * FROM saved_routes WHERE isFavorite = 1 ORDER BY lastUsed DESC")
    fun getFavoriteRoutes(): Flow<List<SavedRouteEntity>>
    
    @Query("SELECT * FROM saved_routes WHERE id = :routeId")
    suspend fun getRouteById(routeId: Long): SavedRouteEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: SavedRouteEntity): Long
    
    @Update
    suspend fun updateRoute(route: SavedRouteEntity)
    
    @Delete
    suspend fun deleteRoute(route: SavedRouteEntity)
    
    @Query("UPDATE saved_routes SET isFavorite = :isFavorite WHERE id = :routeId")
    suspend fun toggleFavorite(routeId: Long, isFavorite: Boolean)
    
    @Query("UPDATE saved_routes SET lastUsed = :timestamp WHERE id = :routeId")
    suspend fun updateLastUsed(routeId: Long, timestamp: Long)
    
    @Query("DELETE FROM saved_routes WHERE id = :routeId")
    suspend fun deleteRouteById(routeId: Long)
}
