package com.nextcs.aurora.social

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class CarpoolListing(
    val id: String = "",
    val driverId: String = "",
    val driverName: String = "",
    val driverEmail: String = "",
    val driverPhone: String = "",
    val driverRating: Double = 0.0,
    val origin: String = "",
    val destination: String = "",
    val departureDate: String = "", // YYYY-MM-DD format
    val departureTime: String = "", // HH:mm format
    val departureTimestamp: Long = 0,
    val availableSeats: Int = 0,
    val pricePerSeat: Double = 0.0,
    val vehicleModel: String = "",
    val vehiclePlate: String = "",
    val vehicleColor: String = "",
    val preferences: String = "",
    val amenities: List<String> = emptyList(), // AC, Music, Pet-friendly, etc.
    val meetingPoint: String = "",
    val route: String = "", // Preferred route
    val status: String = "active", // active, completed, cancelled
    val timestamp: Long = 0
)

data class RideRequest(
    val id: String = "",
    val requesterId: String = "",
    val requesterName: String = "",
    val requesterEmail: String = "",
    val requesterPhone: String = "",
    val requesterRating: Double = 0.0,
    val pickupLocation: String = "",
    val pickupAddress: String = "",
    val dropoffLocation: String = "",
    val dropoffAddress: String = "",
    val requestedDate: String = "", // YYYY-MM-DD format
    val requestedTime: String = "", // HH:mm format  
    val requestedTimestamp: Long = 0,
    val passengers: Int = 1,
    val offerPrice: Double = 0.0,
    val luggage: Boolean = false,
    val notes: String = "",
    val preferences: String = "",
    val status: String = "pending", // pending, accepted, declined, completed
    val acceptedBy: String = "", // Driver ID who accepted
    val timestamp: Long = 0
)

data class DrivingStats(
    val safetyScore: Double = 0.0, // 0-100
    val punctualityScore: Double = 0.0, // 0-100
    val totalRides: Int = 0,
    val completedRides: Int = 0,
    val cancelledRides: Int = 0,
    val averageRating: Double = 0.0, // 0-5
    val totalRatings: Int = 0,
    val onTimePercentage: Double = 0.0,
    val responsiveness: Double = 0.0, // 0-100
    val drivingScore: Double = 0.0 // Overall calculated score 0-100
)

data class UserProfile(
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val phoneNumber: String = "",
    val bio: String = "",
    val stats: DrivingStats = DrivingStats()
)

data class FriendRequest(
    val id: String = "",
    val fromUserId: String = "",
    val fromUserName: String = "",
    val fromUserEmail: String = "",
    val toUserId: String = "",
    val toUserName: String = "",
    val status: String = "pending", // pending, accepted, rejected
    val timestamp: Long = 0
)

class SocialFirebaseService(private val context: Context) {
    
    private val TAG = "SocialFirebaseService"
    private val firestore = FirebaseFirestore.getInstance(FirebaseApp.getInstance(), "sfse")
    private val auth = FirebaseAuth.getInstance()
    
    // Collections
    private val usersCollection = firestore.collection("users")
    private val carpoolCollection = firestore.collection("carpools")
    private val rideRequestsCollection = firestore.collection("rideRequests")
    private val friendsCollection = firestore.collection("friends")
    private val friendRequestsCollection = firestore.collection("friendRequests")
    
    /**
     * Get current user ID
     */
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
    
    /**
     * Get current user profile and ensure it's saved to Firestore
     */
    suspend fun getCurrentUserProfile(): Result<UserProfile> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))
            val user = auth.currentUser ?: return Result.failure(Exception("User not logged in"))
            
            Log.d(TAG, "Getting profile for user: $userId, display name: ${user.displayName}, email: ${user.email}")
            
            // Check if user already exists in Firestore
            val existingDoc = usersCollection.document(userId).get().await()
            
            if (existingDoc.exists()) {
                // User exists, manually parse it to handle both UserProfile formats
                try {
                    val displayName = existingDoc.getString("fullName") 
                        ?: existingDoc.getString("displayName") 
                        ?: user.displayName
                        ?: user.email?.substringBefore("@") 
                        ?: "User"
                    val email = existingDoc.getString("email") ?: user.email ?: ""
                    val photoUrl = existingDoc.getString("profileImageUrl") 
                        ?: existingDoc.getString("photoUrl") 
                        ?: user.photoUrl?.toString() 
                        ?: ""
                    
                    val existingProfile = UserProfile(
                        userId = userId,
                        displayName = displayName,
                        email = email,
                        photoUrl = photoUrl
                    )
                    Log.d(TAG, "User profile already exists: $existingProfile")
                    return Result.success(existingProfile)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse existing profile, will create new one: ${e.message}")
                }
            }
            
            // Create new profile
            val profile = UserProfile(
                userId = userId,
                displayName = user.displayName ?: user.email?.substringBefore("@") ?: "User",
                email = user.email ?: "",
                photoUrl = user.photoUrl?.toString() ?: ""
            )
            
            // Save/update user profile in Firestore
            usersCollection.document(userId).set(profile).await()
            Log.d(TAG, "User profile saved to Firestore: $profile")
            
            Result.success(profile)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting/saving user profile: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Search users by name or email
     */
    suspend fun searchUsers(query: String): Result<List<UserProfile>> {
        return try {
            val currentUserId = getCurrentUserId()
            
            Log.d(TAG, "==================== SEARCH USERS ====================")
            Log.d(TAG, "Query: '$query'")
            Log.d(TAG, "Current user ID: $currentUserId")
            Log.d(TAG, "Firestore project: ${firestore.app.options.projectId}")
            Log.d(TAG, "Auth user: ${auth.currentUser?.email}")
            
            // Fetch all users as raw documents and convert manually
            val snapshot = usersCollection
                .limit(100) // Reasonable limit for user search
                .get()
                .await()
            
            Log.d(TAG, "✅ Firestore query successful!")
            Log.d(TAG, "Found ${snapshot.documents.size} total documents in users collection")
            
            if (snapshot.documents.isEmpty()) {
                Log.w(TAG, "⚠️ No documents in users collection! Users may not have been saved during registration.")
            }
            
            // Convert documents to UserProfile, handling both formats
            val allUsers = snapshot.documents.mapNotNull { doc ->
                try {
                    Log.d(TAG, "Processing document: ${doc.id}")
                    Log.d(TAG, "  Document data: ${doc.data}")
                    
                    // Try to get the fields we need
                    val userId = doc.getString("userId") ?: doc.id
                    val displayName = doc.getString("fullName") 
                        ?: doc.getString("displayName") 
                        ?: doc.getString("email")?.substringBefore("@") 
                        ?: "User"
                    val email = doc.getString("email") ?: ""
                    val photoUrl = doc.getString("profileImageUrl") ?: doc.getString("photoUrl") ?: ""
                    
                    Log.d(TAG, "✅ Converted user: $userId, name: $displayName, email: $email")
                    
                    UserProfile(
                        userId = userId,
                        displayName = displayName,
                        email = email,
                        photoUrl = photoUrl
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to convert document ${doc.id}: ${e.message}")
                    null
                }
            }
            
            Log.d(TAG, "Successfully converted ${allUsers.size} users from ${snapshot.documents.size} documents")
            Log.d(TAG, "======================================================")
            
            if (query.isBlank()) {
                // Return all users except current user (for debugging/testing)
                val results = allUsers.filter { it.userId != currentUserId }
                Log.d(TAG, "Returning all ${results.size} users (empty query)")
                return Result.success(results)
            }
            
            val queryLower = query.lowercase()
            
            // Filter by name or email (case-insensitive)
            val results = allUsers
                .filter { it.userId != currentUserId }
                .filter { user ->
                    user.displayName.lowercase().contains(queryLower) ||
                    user.email.lowercase().contains(queryLower)
                }
                .take(20)
            
            Log.d(TAG, "Search '$query' found ${results.size} matching users")
            Result.success(results)
        } catch (e: Exception) {
            Log.e(TAG, "Error searching users: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Send friend request
     */
    suspend fun sendFriendRequest(friendUserId: String, friendName: String, friendEmail: String): Result<Unit> {
        return try {
            val currentUserId = getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))
            val currentUserName = auth.currentUser?.displayName ?: auth.currentUser?.email?.substringBefore("@") ?: "User"
            val currentUserEmail = auth.currentUser?.email ?: ""
            
            // Check if already friends
            val existingFriend = friendsCollection
                .document(currentUserId)
                .collection("userFriends")
                .document(friendUserId)
                .get()
                .await()
            
            if (existingFriend.exists()) {
                return Result.failure(Exception("Already friends"))
            }
            
            // Check if request already exists
            val existingRequest = friendRequestsCollection
                .whereEqualTo("fromUserId", currentUserId)
                .whereEqualTo("toUserId", friendUserId)
                .whereEqualTo("status", "pending")
                .get()
                .await()
            
            if (!existingRequest.isEmpty) {
                return Result.failure(Exception("Friend request already sent"))
            }
            
            // Create friend request
            val requestId = friendRequestsCollection.document().id
            val request = FriendRequest(
                id = requestId,
                fromUserId = currentUserId,
                fromUserName = currentUserName,
                fromUserEmail = currentUserEmail,
                toUserId = friendUserId,
                toUserName = friendName,
                status = "pending"
            )
            
            friendRequestsCollection.document(requestId).set(request).await()
            
            // Send notification
            val notificationService = NotificationService(context)
            notificationService.sendNotification(
                toUserId = friendUserId,
                type = "friend_request",
                title = "New Friend Request",
                message = "$currentUserName wants to be your friend",
                actionData = mapOf("requestId" to requestId)
            )
            
            Log.d(TAG, "Friend request sent: $requestId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending friend request", e)
            Result.failure(e)
        }
    }
    
    /**
     * Accept friend request
     */
    suspend fun acceptFriendRequest(requestId: String): Result<Unit> {
        return try {
            val requestDoc = friendRequestsCollection.document(requestId).get().await()
            val request = requestDoc.toObject(FriendRequest::class.java) 
                ?: return Result.failure(Exception("Request not found"))
            
            // Add to both users' friends
            friendsCollection
                .document(request.toUserId)
                .collection("userFriends")
                .document(request.fromUserId)
                .set(mapOf(
                    "userId" to request.fromUserId,
                    "displayName" to request.fromUserName,
                    "email" to request.fromUserEmail,
                    "addedAt" to System.currentTimeMillis()
                ))
                .await()
            
            friendsCollection
                .document(request.fromUserId)
                .collection("userFriends")
                .document(request.toUserId)
                .set(mapOf(
                    "userId" to request.toUserId,
                    "displayName" to request.toUserName,
                    "addedAt" to System.currentTimeMillis()
                ))
                .await()
            
            // Update request status
            friendRequestsCollection.document(requestId)
                .update("status", "accepted")
                .await()
            
            // Send notification
            val notificationService = NotificationService(context)
            notificationService.sendNotification(
                toUserId = request.fromUserId,
                type = "friend_accepted",
                title = "Friend Request Accepted",
                message = "${request.toUserName} accepted your friend request"
            )
            
            Log.d(TAG, "Friend request accepted: $requestId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error accepting friend request", e)
            Result.failure(e)
        }
    }
    
    /**
     * Reject friend request
     */
    suspend fun rejectFriendRequest(requestId: String): Result<Unit> {
        return try {
            friendRequestsCollection.document(requestId)
                .update("status", "rejected")
                .await()
            
            Log.d(TAG, "Friend request rejected: $requestId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error rejecting friend request", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get pending friend requests for current user
     */
    fun observePendingRequests(): Flow<List<FriendRequest>> = callbackFlow {
        val userId = getCurrentUserId()
        if (userId == null) {
            close(Exception("Not logged in"))
            return@callbackFlow
        }
        
        val listener = friendRequestsCollection
            .whereEqualTo("toUserId", userId)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val requests = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(FriendRequest::class.java)
                } ?: emptyList()
                
                trySend(requests)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Add friend (deprecated - use sendFriendRequest instead)
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
            }.sortedBy { it.displayName }
            
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
     * Observe all carpool listings from all users
     */
    fun observeCarpoolListings(): Flow<List<CarpoolListing>> = callbackFlow {
        val currentUserId = getCurrentUserId()
        
        val listener = carpoolCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing carpool listings: ${error.message}", error)
                    return@addSnapshotListener
                }
                
                val listings = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(CarpoolListing::class.java)
                    } catch (e: Exception) {
                        Log.w(TAG, "Skipping carpool listing ${doc.id} - incompatible format: ${e.message}")
                        null
                    }
                }?.sortedByDescending { it.timestamp } ?: emptyList()
                
                Log.d(TAG, "Observed ${listings.size} carpool listings (current user: $currentUserId)")
                trySend(listings)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Get carpool listings (one-time fetch) - shows all listings from all users
     */
    suspend fun getCarpoolListings(): Result<List<CarpoolListing>> {
        return try {
            val currentUserId = getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))
            
            Log.d(TAG, "Fetching carpool listings...")
            val listings = carpoolCollection
                .get()
                .await()
                .toObjects(CarpoolListing::class.java)
                .sortedByDescending { it.timestamp }
            
            Log.d(TAG, "Retrieved ${listings.size} carpool listings for user $currentUserId")
            Result.success(listings)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting carpool listings: ${e.message}", e)
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
     * Observe ride requests (excluding own requests)
     */
    fun observeRideRequests(): Flow<List<RideRequest>> = callbackFlow {
        val currentUserId = getCurrentUserId()
        
        val listener = rideRequestsCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing ride requests: ${error.message}", error)
                    return@addSnapshotListener
                }
                
                val requests = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(RideRequest::class.java)
                    } catch (e: Exception) {
                        Log.w(TAG, "Skipping ride request ${doc.id} - incompatible format: ${e.message}")
                        null
                    }
                }?.filter { it.requesterId != currentUserId } // Exclude own requests
                  ?.sortedByDescending { it.timestamp } ?: emptyList()
                
                trySend(requests)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Get ride requests (one-time fetch, excluding own requests)
     */
    suspend fun getRideRequests(): Result<List<RideRequest>> {
        return try {
            val currentUserId = getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))
            
            Log.d(TAG, "Fetching ride requests...")
            val requests = rideRequestsCollection
                .get()
                .await()
                .toObjects(RideRequest::class.java)
                .filter { it.requesterId != currentUserId } // Exclude own requests
                .sortedByDescending { it.timestamp }
            
            Log.d(TAG, "Retrieved ${requests.size} ride requests for user $currentUserId")
            Result.success(requests)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting ride requests: ${e.message}", e)
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
    
    /**
     * Get user profile by ID
     */
    suspend fun getUserProfile(userId: String): Result<UserProfile> {
        return try {
            val doc = usersCollection.document(userId).get().await()
            
            if (!doc.exists()) {
                return Result.failure(Exception("User not found"))
            }
            
            val displayName = doc.getString("fullName") 
                ?: doc.getString("displayName") 
                ?: "User"
            val email = doc.getString("email") ?: ""
            val photoUrl = doc.getString("profileImageUrl") 
                ?: doc.getString("photoUrl") 
                ?: ""
            val phoneNumber = doc.getString("phoneNumber") ?: ""
            val bio = doc.getString("bio") ?: ""
            
            // Parse stats if available
            val statsMap = doc.get("stats") as? Map<String, Any>
            val stats = if (statsMap != null) {
                DrivingStats(
                    safetyScore = (statsMap["safetyScore"] as? Number)?.toDouble() ?: 0.0,
                    punctualityScore = (statsMap["punctualityScore"] as? Number)?.toDouble() ?: 0.0,
                    totalRides = (statsMap["totalRides"] as? Number)?.toInt() ?: 0,
                    completedRides = (statsMap["completedRides"] as? Number)?.toInt() ?: 0,
                    cancelledRides = (statsMap["cancelledRides"] as? Number)?.toInt() ?: 0,
                    averageRating = (statsMap["averageRating"] as? Number)?.toDouble() ?: 0.0,
                    totalRatings = (statsMap["totalRatings"] as? Number)?.toInt() ?: 0,
                    onTimePercentage = (statsMap["onTimePercentage"] as? Number)?.toDouble() ?: 0.0,
                    responsiveness = (statsMap["responsiveness"] as? Number)?.toDouble() ?: 0.0,
                    drivingScore = (statsMap["drivingScore"] as? Number)?.toDouble() ?: 0.0
                )
            } else {
                DrivingStats()
            }
            
            val profile = UserProfile(
                userId = userId,
                displayName = displayName,
                email = email,
                photoUrl = photoUrl,
                phoneNumber = phoneNumber,
                bio = bio,
                stats = stats
            )
            
            Result.success(profile)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user profile: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get mutual friends between current user and another user
     */
    suspend fun getMutualFriends(friendUserId: String): Result<List<UserProfile>> {
        return try {
            val currentUserId = getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))
            
            // Get current user's friends
            val currentUserFriends = friendsCollection
                .document(currentUserId)
                .collection("userFriends")
                .get()
                .await()
                .documents
                .mapNotNull { it.id }
                .toSet()
            
            // Get target user's friends
            val targetUserFriends = friendsCollection
                .document(friendUserId)
                .collection("userFriends")
                .get()
                .await()
                .documents
                .mapNotNull { it.id }
                .toSet()
            
            // Find mutual friends
            val mutualFriendIds = currentUserFriends.intersect(targetUserFriends)
            
            // Get profiles for mutual friends
            val mutualProfiles = mutualFriendIds.mapNotNull { userId ->
                try {
                    getUserProfile(userId).getOrNull()
                } catch (e: Exception) {
                    null
                }
            }
            
            Result.success(mutualProfiles)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting mutual friends: ${e.message}", e)
            Result.failure(e)
        }
    }
}
