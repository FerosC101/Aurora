package com.nextcs.aurora.social

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class Notification(
    val id: String = "",
    val userId: String = "",
    val type: String = "", // friend_request, message, carpool_response, ride_response
    val title: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val read: Boolean = false,
    val actionData: Map<String, String> = emptyMap() // Additional data for actions
)

class NotificationService(private val context: Context) {
    
    private val TAG = "NotificationService"
    private val firestore = FirebaseFirestore.getInstance(FirebaseApp.getInstance(), "sfse")
    private val auth = FirebaseAuth.getInstance()
    
    private val notificationsCollection = firestore.collection("notifications")
    
    private fun getCurrentUserId(): String? = auth.currentUser?.uid
    
    /**
     * Send a notification to a user
     */
    suspend fun sendNotification(
        toUserId: String,
        type: String,
        title: String,
        message: String,
        actionData: Map<String, String> = emptyMap()
    ): Result<Unit> {
        return try {
            val notificationId = notificationsCollection.document().id
            val notification = Notification(
                id = notificationId,
                userId = toUserId,
                type = type,
                title = title,
                message = message,
                actionData = actionData
            )
            
            notificationsCollection.document(notificationId).set(notification).await()
            Log.d(TAG, "Notification sent: $notificationId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending notification", e)
            Result.failure(e)
        }
    }
    
    /**
     * Observe notifications for current user
     */
    fun observeNotifications(): Flow<List<Notification>> = callbackFlow {
        val userId = getCurrentUserId()
        if (userId == null) {
            close(Exception("Not logged in"))
            return@callbackFlow
        }
        
        val listener = notificationsCollection
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val notifications = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Notification::class.java)
                } ?: emptyList()
                
                trySend(notifications)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Mark notification as read
     */
    suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            notificationsCollection.document(notificationId)
                .update("read", true)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking notification as read", e)
            Result.failure(e)
        }
    }
    
    /**
     * Delete notification
     */
    suspend fun deleteNotification(notificationId: String): Result<Unit> {
        return try {
            notificationsCollection.document(notificationId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting notification", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get unread notification count
     */
    suspend fun getUnreadCount(): Int {
        return try {
            val userId = getCurrentUserId() ?: return 0
            val snapshot = notificationsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("read", false)
                .get()
                .await()
            snapshot.documents.size
        } catch (e: Exception) {
            Log.e(TAG, "Error getting unread count", e)
            0
        }
    }
}
