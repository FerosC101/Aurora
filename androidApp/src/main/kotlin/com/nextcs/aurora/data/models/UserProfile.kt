package com.nextcs.aurora.data.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class UserProfile(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val dateOfBirth: String = "", // Format: "YYYY-MM-DD"
    val gender: String = "", // "Male", "Female", "Other", "Prefer not to say"
    val address: String = "",
    val city: String = "",
    val country: String = "",
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = "",
    val profileImageUrl: String = "",
    val isPremium: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val locationSharingEnabled: Boolean = false,
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
)
