package com.nextcs.aurora.social

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class CarpoolListing(
    val id: String = "",
    val driverId: String = "",
    val driverName: String = "",
    val driverEmail: String = "",
    val origin: String = "",
    val destination: String = "",
    val departureTime: Long = 0,
    val availableSeats: Int = 0,
    val pricePerSeat: Double = 0.0,
    val vehicleModel: String = "",
    val preferences: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class RideRequest(
    val id: String = "",
    val requesterId: String = "",
    val requesterName: String = "",
    val requesterEmail: String = "",
    val pickupLocation: String = "",
    val dropoffLocation: String = "",
    val requestedTime: Long = 0,
    val passengers: Int = 1,
    val notes: String = "",
    val status: String = "pending", // pending, accepted, declined
    val timestamp: Long = System.currentTimeMillis()
)

data class UserProfile(
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

class SocialFirebaseService(private val context: Context) {
    
    private val TAG = "SocialFirebaseService"
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    // Collections
    private val usersCollection = firestore.collection("users")
    private val carpoolCollection = firestore.collection("carpools")
    private val rideRequestsCollection = firestore.collection("rideRequests")
    private val friendsCollection = firestore.collection("friends")
    
    /**
     * Get current user ID
     */
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
    
    /**
     * Get current user profile
     */
    suspend fun getCurrentUserProfile(): Result<UserProfile> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))
            val user = auth.currentUser ?: return Result.failure(Exception("User not logged in"))
            
            val profile = UserProfile(
                userId = userId,
                displayName = user.displayName ?: "User",
                email = user.email ?: "",
                photoUrl = user.photoUrl?.toString() ?: ""
            )
            
            // Save/update user profile in Firestore
            usersCollection.document(userId).set(profile).await()
            
            Result.success(profile)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user profile", e)
            Result.failure(e)
        }
    }
    
    /**
     * Search users by name or email
     */
    suspend fun searchUsers(query: String): Result<List<UserProfile>> {
        return try {
            if (query.isBlank()) {
                return Result.success(emptyList())
            }
            
            val currentUserId = getCurrentUserId()
            val queryLower = query.lowercase()
            
            // Search by name
            val nameResults = usersCollection
                .orderBy("displayName")
                .startAt(queryLower)
                .endAt(queryLower + "\uf8ff")
                .limit(20)
                .get()
                .await()
                .toObjects(UserProfile::class.java)
                .filter { it.userId != currentUserId }
            
            // Search by email
            val emailResults = usersCollection
                .orderBy("email")
                .startAt(queryLower)
                .endAt(queryLower + "\uf8ff")
                .limit(20)
                .get()
                .await()
                .toObjects(UserProfile::class.java)
                .filter { it.userId != currentUserId }
            
            // Combine and deduplicate
            val allResults = (nameResults + emailResults).distinctBy { it.userId }
            
            Log.d(TAG, "Found ${allResults.size} users matching '$query'")
            Result.success(allResults)
        } catch (e: Exception) {
            Log.e(TAG, "Error searching users", e)
            Result.failure(e)
        }
    }
    
    /**
     * Add friend
     */
    suspend fun addFriend(friendUserId: String): Result<Unit> {
        return try {
            val currentUserId = getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))
            
            // Add to current user's friends
            friendsCollection
                .document(currentUserId)
                .collection("userFriends")
                .document(friendUserId)
                .set(mapOf(
                    "userId" to friendUserId,
                    "addedAt" to System.currentTimeMillis()
                ))
                .await()
            
            // Add to friend's friends (bidirectional)
            friendsCollection
                .document(friendUserId)
                .collection("userFriends")
                .document(currentUserId)
                .set(mapOf(
                    "userId" to currentUserId,
                    "addedAt" to System.currentTimeMillis()
                ))
                .await()
            
            Log.d(TAG, "Friend added successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding friend", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get friends list
     */
    suspend fun getFriends(): Result<List<UserProfile>> {
        return try {
            val currentUserId = getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))
            
            // Get friend IDs
            val friendIds = friendsCollection
                .document(currentUserId)
                .collection("userFriends")
                .get()
                .await()
                .documents
                .mapNotNull { it.getString("userId") }
            
            if (friendIds.isEmpty()) {
                return Result.success(emptyList())
            }
            
            // Get friend profiles (in batches of 10 due to Firestore 'in' query limit)
            val friends = friendIds.chunked(10).flatMap { batch ->
                usersCollection
                    .whereIn("userId", batch)
                    .get()
                    .await()
                    .toObjects(UserProfile::class.java)
            }
            
            Log.d(TAG, "Retrieved ${friends.size} friends")
            Result.success(friends)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting friends", e)
            Result.failure(e)
        }
    }
    
    /**
     * Remove friend
     */
    suspend fun removeFriend(friendUserId: String): Result<Unit> {
        return try {
            val currentUserId = getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))
            
            // Remove from current user's friends
            friendsCollection
                .document(currentUserId)
                .collection("userFriends")
                .document(friendUserId)
                .delete()
                .await()
            
            // Remove from friend's friends
            friendsCollection
                .document(friendUserId)
                .collection("userFriends")
                .document(currentUserId)
                .delete()
                .await()
            
            Log.d(TAG, "Friend removed successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing friend", e)
            Result.failure(e)
        }
    }
    
    /**
     * Create carpool listing
     */
    suspend fun createCarpoolListing(listing: CarpoolListing): Result<String> {
        return try {
            val currentUserId = getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))
            val userProfile = getCurrentUserProfile().getOrNull() ?: return Result.failure(Exception("Failed to get user profile"))
            
            val listingId = if (listing.id.isNotEmpty()) listing.id else firestore.collection("carpools").document().id
            
            val carpoolData = listing.copy(
                id = listingId,
                driverId = currentUserId,
                driverName = userProfile.displayName,
                driverEmail = userProfile.email,
                timestamp = System.currentTimeMillis()
            )
            
            carpoolCollection
                .document(listingId)
                .set(carpoolData)
                .await()
            
            Log.d(TAG, "Carpool listing created: $listingId")
            Result.success(listingId)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating carpool listing", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get all carpool listings
     */
    fun observeCarpoolListings(): Flow<List<CarpoolListing>> = callbackFlow {
        val listener = carpoolCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing carpool listings", error)
                    return@addSnapshotListener
                }
                
                val listings = snapshot?.toObjects(CarpoolListing::class.java) ?: emptyList()
                trySend(listings)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Get carpool listings (one-time fetch)
     */
    suspend fun getCarpoolListings(): Result<List<CarpoolListing>> {
        return try {
            val listings = carpoolCollection
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(CarpoolListing::class.java)
            
            Log.d(TAG, "Retrieved ${listings.size} carpool listings")
            Result.success(listings)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting carpool listings", e)
            Result.failure(e)
        }
    }
    
    /**
     * Delete carpool listing
     */
    suspend fun deleteCarpoolListing(listingId: String): Result<Unit> {
        return try {
            carpoolCollection.document(listingId).delete().await()
            Log.d(TAG, "Carpool listing deleted: $listingId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting carpool listing", e)
            Result.failure(e)
        }
    }
    
    /**
     * Create ride request
     */
    suspend fun createRideRequest(request: RideRequest): Result<String> {
        return try {
            val currentUserId = getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))
            val userProfile = getCurrentUserProfile().getOrNull() ?: return Result.failure(Exception("Failed to get user profile"))
            
            val requestId = if (request.id.isNotEmpty()) request.id else firestore.collection("rideRequests").document().id
            
            val rideRequestData = request.copy(
                id = requestId,
                requesterId = currentUserId,
                requesterName = userProfile.displayName,
                requesterEmail = userProfile.email,
                timestamp = System.currentTimeMillis()
            )
            
            rideRequestsCollection
                .document(requestId)
                .set(rideRequestData)
                .await()
            
            Log.d(TAG, "Ride request created: $requestId")
            Result.success(requestId)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating ride request", e)
            Result.failure(e)
        }
    }
    
    /**
     * Observe ride requests
     */
    fun observeRideRequests(): Flow<List<RideRequest>> = callbackFlow {
        val listener = rideRequestsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing ride requests", error)
                    return@addSnapshotListener
                }
                
                val requests = snapshot?.toObjects(RideRequest::class.java) ?: emptyList()
                trySend(requests)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Get ride requests (one-time fetch)
     */
    suspend fun getRideRequests(): Result<List<RideRequest>> {
        return try {
            val requests = rideRequestsCollection
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(RideRequest::class.java)
            
            Log.d(TAG, "Retrieved ${requests.size} ride requests")
            Result.success(requests)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting ride requests", e)
            Result.failure(e)
        }
    }
    
    /**
     * Update ride request status
     */
    suspend fun updateRideRequestStatus(requestId: String, status: String): Result<Unit> {
        return try {
            rideRequestsCollection
                .document(requestId)
                .update("status", status)
                .await()
            
            Log.d(TAG, "Ride request status updated: $requestId -> $status")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating ride request status", e)
            Result.failure(e)
        }
    }
    
    /**
     * Delete ride request
     */
    suspend fun deleteRideRequest(requestId: String): Result<Unit> {
        return try {
            rideRequestsCollection.document(requestId).delete().await()
            Log.d(TAG, "Ride request deleted: $requestId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting ride request", e)
            Result.failure(e)
        }
    }
}
