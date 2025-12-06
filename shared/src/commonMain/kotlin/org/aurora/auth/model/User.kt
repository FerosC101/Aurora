package org.aurora.auth.model

// Room imports temporarily disabled - Android support removed
// import androidx.room.Entity
// import androidx.room.PrimaryKey

// @Entity(tableName = "users")
data class User(
    // @PrimaryKey(autoGenerate = true) 
    val id: Long = 0,
    val fullName: String,
    val email: String,
    val passwordHash: String,
    val createdAt: Long = System.currentTimeMillis()
)