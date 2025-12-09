package org.aurora.android.database

import org.aurora.android.navigation.model.NavigationRoute
import org.aurora.android.navigation.model.RouteType
import java.sql.Connection
import java.sql.ResultSet
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Repository for managing trip history in database
 */
class TripHistoryRepository(private val connection: Connection) {
    
    init {
        createTripHistoryTable()
    }
    
    private fun createTripHistoryTable() {
        val statement = connection.createStatement()
        statement.executeUpdate("""
            CREATE TABLE IF NOT EXISTS trip_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                origin TEXT NOT NULL,
                destination TEXT NOT NULL,
                route_type TEXT NOT NULL,
                route_name TEXT NOT NULL,
                distance REAL NOT NULL,
                estimated_time INTEGER NOT NULL,
                actual_time INTEGER NOT NULL,
                safety_score INTEGER NOT NULL,
                hazards_avoided INTEGER NOT NULL,
                time_saved INTEGER NOT NULL,
                completed_at TEXT NOT NULL,
                FOREIGN KEY (user_id) REFERENCES users(id)
            )
        """)
    }
    
    /**
     * Save completed trip to history
     */
    fun saveTrip(
        userId: Int,
        origin: String,
        destination: String,
        route: NavigationRoute,
        actualTime: Int,
        hazardsAvoided: Int,
        timeSaved: Int
    ): Boolean {
        return try {
            val statement = connection.prepareStatement("""
                INSERT INTO trip_history 
                (user_id, origin, destination, route_type, route_name, distance, 
                 estimated_time, actual_time, safety_score, hazards_avoided, 
                 time_saved, completed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """)
            
            statement.setInt(1, userId)
            statement.setString(2, origin)
            statement.setString(3, destination)
            statement.setString(4, route.type.name)
            statement.setString(5, route.name)
            statement.setDouble(6, route.distance.toDouble())
            statement.setInt(7, route.estimatedTime)
            statement.setInt(8, actualTime)
            statement.setInt(9, route.safetyScore)
            statement.setInt(10, hazardsAvoided)
            statement.setInt(11, timeSaved)
            statement.setString(12, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            
            statement.executeUpdate() > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Get trip history for user
     */
    fun getTripHistory(userId: Int, limit: Int = 20): List<TripRecord> {
        val trips = mutableListOf<TripRecord>()
        try {
            val statement = connection.prepareStatement("""
                SELECT * FROM trip_history 
                WHERE user_id = ? 
                ORDER BY completed_at DESC 
                LIMIT ?
            """)
            statement.setInt(1, userId)
            statement.setInt(2, limit)
            
            val resultSet = statement.executeQuery()
            while (resultSet.next()) {
                trips.add(resultSet.toTripRecord())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return trips
    }
    
    /**
     * Get user statistics
     */
    fun getUserStatistics(userId: Int): UserStatistics {
        return try {
            val statement = connection.prepareStatement("""
                SELECT 
                    COUNT(*) as total_trips,
                    SUM(distance) as total_distance,
                    SUM(actual_time) as total_time,
                    SUM(hazards_avoided) as total_hazards_avoided,
                    SUM(time_saved) as total_time_saved,
                    AVG(safety_score) as avg_safety_score
                FROM trip_history
                WHERE user_id = ?
            """)
            statement.setInt(1, userId)
            
            val resultSet = statement.executeQuery()
            if (resultSet.next()) {
                UserStatistics(
                    totalTrips = resultSet.getInt("total_trips"),
                    totalDistance = resultSet.getDouble("total_distance").toFloat(),
                    totalTime = resultSet.getInt("total_time"),
                    totalHazardsAvoided = resultSet.getInt("total_hazards_avoided"),
                    totalTimeSaved = resultSet.getInt("total_time_saved"),
                    avgSafetyScore = resultSet.getDouble("avg_safety_score").toInt()
                )
            } else {
                UserStatistics()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            UserStatistics()
        }
    }
    
    /**
     * Get route type preferences (most used routes)
     */
    fun getRouteTypeStats(userId: Int): Map<RouteType, Int> {
        val stats = mutableMapOf<RouteType, Int>()
        try {
            val statement = connection.prepareStatement("""
                SELECT route_type, COUNT(*) as count
                FROM trip_history
                WHERE user_id = ?
                GROUP BY route_type
            """)
            statement.setInt(1, userId)
            
            val resultSet = statement.executeQuery()
            while (resultSet.next()) {
                val routeType = RouteType.valueOf(resultSet.getString("route_type"))
                val count = resultSet.getInt("count")
                stats[routeType] = count
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return stats
    }
    
    private fun ResultSet.toTripRecord(): TripRecord {
        return TripRecord(
            id = getInt("id"),
            userId = getInt("user_id"),
            origin = getString("origin"),
            destination = getString("destination"),
            routeType = RouteType.valueOf(getString("route_type")),
            routeName = getString("route_name"),
            distance = getDouble("distance").toFloat(),
            estimatedTime = getInt("estimated_time"),
            actualTime = getInt("actual_time"),
            safetyScore = getInt("safety_score"),
            hazardsAvoided = getInt("hazards_avoided"),
            timeSaved = getInt("time_saved"),
            completedAt = getString("completed_at")
        )
    }
}

/**
 * Single trip record from history
 */
data class TripRecord(
    val id: Int,
    val userId: Int,
    val origin: String,
    val destination: String,
    val routeType: RouteType,
    val routeName: String,
    val distance: Float,
    val estimatedTime: Int,
    val actualTime: Int,
    val safetyScore: Int,
    val hazardsAvoided: Int,
    val timeSaved: Int,
    val completedAt: String
)

/**
 * Aggregated user statistics
 */
data class UserStatistics(
    val totalTrips: Int = 0,
    val totalDistance: Float = 0f,
    val totalTime: Int = 0,
    val totalHazardsAvoided: Int = 0,
    val totalTimeSaved: Int = 0,
    val avgSafetyScore: Int = 0
)
