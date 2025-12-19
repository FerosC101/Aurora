package com.nextcs.aurora.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "saved_routes")
@TypeConverters(WaypointListConverter::class)
data class SavedRouteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val origin: String,
    val destination: String,
    val distance: Double, // km
    val estimatedTime: Int, // minutes
    val waypoints: List<WaypointData> = emptyList(),
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsed: Long = 0
)

data class WaypointData(
    val name: String,
    val latitude: Double,
    val longitude: Double
)

class WaypointListConverter {
    private val gson = Gson()
    
    @TypeConverter
    fun fromWaypointList(waypoints: List<WaypointData>): String {
        return gson.toJson(waypoints)
    }
    
    @TypeConverter
    fun toWaypointList(json: String): List<WaypointData> {
        val type = object : TypeToken<List<WaypointData>>() {}.type
        return gson.fromJson(json, type)
    }
}
