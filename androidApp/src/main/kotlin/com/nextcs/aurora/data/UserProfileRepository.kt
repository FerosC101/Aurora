package com.nextcs.aurora.data

import com.nextcs.aurora.data.models.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

class UserProfileRepository {
    
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(FirebaseApp.getInstance(), "sfse")
    private val usersCollection = db.collection("users")
    
    init {
        println("🔵 UserProfileRepository initialized with Firestore app: ${db.app.name}")
    }
    
    /**
     * Create or update user profile
     */
    suspend fun saveUserProfile(profile: UserProfile): Result<Unit> {
        return try {
            println("🔵 Firestore: Attempting to save profile for user: ${profile.userId}")
            println("🔵 Firestore: Collection path: users/${profile.userId}")
            println("🔵 Firestore: Database name: ${db.app.options.projectId}")
            
            // Try without timeout first to see the actual error
            usersCollection.document(profile.userId).set(profile).await()
            
            println("✅ Firestore: Profile saved successfully!")
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ Firestore save error: ${e.javaClass.simpleName}: ${e.message}")
            println("❌ Full error: ${e}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * Get user profile by userId
     */
    suspend fun getUserProfile(userId: String): Result<UserProfile?> {
        return try {
            val snapshot = usersCollection.document(userId).get().await()
            val profile = snapshot.toObject(UserProfile::class.java)
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update specific fields in user profile
     */
    suspend fun updateUserProfile(userId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            usersCollection.document(userId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete user profile
     */
    suspend fun deleteUserProfile(userId: String): Result<Unit> {
        return try {
            usersCollection.document(userId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
