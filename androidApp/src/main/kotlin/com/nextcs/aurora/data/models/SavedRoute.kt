package com.nextcs.aurora.data.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class SavedRoute(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val routeName: String = "",
    val origin: RouteLocation = RouteLocation(),
    val destination: RouteLocation = RouteLocation(),
    val waypoints: List<RouteLocation> = emptyList(),
    val distance: Double = 0.0,
    val duration: Long = 0,
    val isFavorite: Boolean = false,
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
)

data class RouteLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = ""
)
