package com.nextcs.aurora.social

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class FriendLocation(
    val userId: String = "",
    val displayName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = 0,
    val tripId: String? = null,
    val destination: String? = null,
    val eta: String? = null,
    val isSharing: Boolean = false
) {
    fun toLatLng(): LatLng = LatLng(latitude, longitude)
}

data class Friend(
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val addedAt: Long = System.currentTimeMillis()
)

class FriendLocationSharingService(private val context: Context) {
    
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(FirebaseApp.getInstance(), "sfse")
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    
    private val currentUserId: String?
        get() = auth.currentUser?.uid
    
    /**
     * Start sharing location with friends
     */
    suspend fun startSharingLocation(
        location: LatLng,
        tripId: String? = null,
        destination: String? = null,
        eta: String? = null
    ): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))
        
        return try {
            val locationData = hashMapOf(
                "latitude" to location.latitude,
                "longitude" to location.longitude,
                "timestamp" to System.currentTimeMillis(),
                "tripId" to tripId,
                "destination" to destination,
                "eta" to eta,
                "isSharing" to true
            )
            
            firestore.collection("user_locations")
                .document(userId)
                .set(locationData)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update location (during active trip)
     */
    suspend fun updateLocation(
        location: LatLng,
        eta: String? = null
    ): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))
        
        return try {
            val updates = hashMapOf<String, Any>(
                "latitude" to location.latitude,
                "longitude" to location.longitude,
                "timestamp" to System.currentTimeMillis()
            )
            
            eta?.let { updates["eta"] = it }
            
            firestore.collection("user_locations")
                .document(userId)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Stop sharing location
     */
    suspend fun stopSharingLocation(): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))
        
        return try {
            firestore.collection("user_locations")
                .document(userId)
                .update("isSharing", false)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Add a friend by user ID
     */
    suspend fun addFriend(friendUserId: String, friendName: String, friendEmail: String): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))
        
        return try {
            val friendData = hashMapOf(
                "userId" to friendUserId,
                "displayName" to friendName,
                "email" to friendEmail,
                "addedAt" to System.currentTimeMillis()
            )
            
            firestore.collection("friends")
                .document(userId)
                .collection("user_friends")
                .document(friendUserId)
                .set(friendData)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Remove a friend
     */
    suspend fun removeFriend(friendUserId: String): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))
        
        return try {
            firestore.collection("friends")
                .document(userId)
                .collection("user_friends")
                .document(friendUserId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get list of friends
     */
    suspend fun getFriends(): Result<List<Friend>> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))
        
        return try {
            val snapshot = firestore.collection("friends")
                .document(userId)
                .collection("userFriends")
                .get()
                .await()
                
            val friends = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Friend::class.java)
            }
            
            Result.success(friends)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Observe friends' locations in real-time
     */
    fun observeFriendsLocations(): Flow<List<FriendLocation>> = callbackFlow {
        val userId = currentUserId
        if (userId == null) {
            close(Exception("User not logged in"))
            return@callbackFlow
        }
        
        // Listen to friends list changes
        val friendsListener = firestore.collection("friends")
            .document(userId)
            .collection("userFriends")
            .addSnapshotListener { friendsSnapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val friendIds = friendsSnapshot?.documents?.mapNotNull { it.id } ?: emptyList()
                
                // If no friends, send empty list
                if (friendIds.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                // Listen to each friend's location
                val locations = mutableListOf<FriendLocation>()
                friendIds.forEach { friendId ->
                    firestore.collection("user_locations")
                        .document(friendId)
                        .addSnapshotListener { locationSnapshot, locError ->
                            if (locError == null && locationSnapshot != null && locationSnapshot.exists()) {
                                val location = locationSnapshot.toObject(FriendLocation::class.java)
                                if (location?.isSharing == true) {
                                    locations.removeAll { it.userId == friendId }
                                    locations.add(location.copy(userId = friendId))
                                    trySend(locations.toList())
                                }
                            }
                        }
                }
            }
        
        awaitClose {
            friendsListener.remove()
        }
    }
    
    /**
     * Observe a specific friend's location
     */
    fun observeFriendLocation(friendUserId: String): Flow<FriendLocation?> = callbackFlow {
        val listener = firestore.collection("user_locations")
            .document(friendUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val location = snapshot?.toObject(FriendLocation::class.java)
                trySend(location)
            }
        
        awaitClose {
            listener.remove()
        }
    }
}
