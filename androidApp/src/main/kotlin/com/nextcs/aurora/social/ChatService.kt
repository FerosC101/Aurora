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

enum class MessageStatus {
    SENT,      // Message sent to server
    DELIVERED, // Message delivered to recipient's device
    SEEN       // Message viewed by recipient
}

data class ChatMessage(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val read: Boolean = false,
    val status: String = MessageStatus.SENT.name
)

data class ChatConversation(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val lastMessage: String = "",
    val lastMessageTime: Long = 0,
    val unreadCount: Map<String, Int> = emptyMap(),
    val type: String = "direct", // direct, carpool, ride_request
    val referenceId: String = "" // carpool or ride request ID if applicable
)

class ChatService(private val context: Context) {
    
    private val TAG = "ChatService"
    private val firestore = FirebaseFirestore.getInstance(FirebaseApp.getInstance(), "sfse")
    private val auth = FirebaseAuth.getInstance()
    
    private val chatsCollection = firestore.collection("chats")
    private val messagesCollection = firestore.collection("messages")
    
    private fun getCurrentUserId(): String? = auth.currentUser?.uid
    
    /**
     * Create or get existing chat between users
     */
    suspend fun getOrCreateChat(
        otherUserId: String,
        otherUserName: String,
        type: String = "direct",
        referenceId: String = ""
    ): Result<String> {
        return try {
            val currentUserId = getCurrentUserId() ?: return Result.failure(Exception("Not logged in"))
            val currentUserName = auth.currentUser?.displayName ?: "User"
            
            // Check if chat already exists
            val participants = listOf(currentUserId, otherUserId).sorted()
            val querySnapshot = chatsCollection
                .whereArrayContains("participants", currentUserId)
                .get()
                .await()
            
            val existingChat = querySnapshot.documents.firstOrNull { doc ->
                val docParticipants = (doc.get("participants") as? List<*>)?.map { it.toString() } ?: emptyList()
                docParticipants.sorted() == participants &&
                doc.getString("type") == type &&
                (referenceId.isEmpty() || doc.getString("referenceId") == referenceId)
            }
            
            if (existingChat != null) {
                Log.d(TAG, "Found existing chat: ${existingChat.id}")
                return Result.success(existingChat.id)
            }
            
            // Create new chat
            val chatId = chatsCollection.document().id
            val chat = ChatConversation(
                id = chatId,
                participants = participants,
                participantNames = mapOf(
                    currentUserId to currentUserName,
                    otherUserId to otherUserName
                ),
                type = type,
                referenceId = referenceId,
                unreadCount = mapOf(
                    currentUserId to 0,
                    otherUserId to 0
                )
            )
            
            chatsCollection.document(chatId).set(chat).await()
            Log.d(TAG, "Created new chat: $chatId")
            Result.success(chatId)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating chat", e)
            Result.failure(e)
        }
    }
    
    /**
     * Send a message in a chat
     */
    suspend fun sendMessage(chatId: String, message: String): Result<Unit> {
        return try {
            val currentUserId = getCurrentUserId() ?: return Result.failure(Exception("Not logged in"))
            val currentUserName = auth.currentUser?.displayName ?: "User"
            
            val messageId = messagesCollection.document().id
            val chatMessage = ChatMessage(
                id = messageId,
                chatId = chatId,
                senderId = currentUserId,
                senderName = currentUserName,
                message = message,
                status = MessageStatus.SENT.name
            )
            
            // Save message
            messagesCollection.document(messageId).set(chatMessage).await()
            
            // Update chat last message
            val chatRef = chatsCollection.document(chatId)
            val chatDoc = chatRef.get().await()
            val participants = (chatDoc.get("participants") as? List<*>)?.map { it.toString() } ?: emptyList()
            val unreadCount = (chatDoc.get("unreadCount") as? Map<*, *>) ?: emptyMap<String, Int>()
            
            val updatedUnreadCount = participants.associateWith { userId ->
                if (userId == currentUserId) 0 
                else ((unreadCount[userId] as? Long)?.toInt() ?: 0) + 1
            }
            
            chatRef.update(mapOf(
                "lastMessage" to message,
                "lastMessageTime" to System.currentTimeMillis(),
                "unreadCount" to updatedUnreadCount
            )).await()
            
            Log.d(TAG, "Message sent: $messageId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message", e)
            Result.failure(e)
        }
    }
    
    /**
     * Observe messages in a chat
     */
    fun observeMessages(chatId: String): Flow<List<ChatMessage>> = callbackFlow {
        // Simple query without orderBy to avoid index requirement
        // Sort client-side instead
        val listener = messagesCollection
            .whereEqualTo("chatId", chatId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Log the error but don't crash - just emit empty list
                    android.util.Log.e("ChatService", "Error observing messages: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)
                }?.sortedBy { it.timestamp } ?: emptyList()
                
                trySend(messages)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Get all chats for current user
     */
    fun observeChats(): Flow<List<ChatConversation>> = callbackFlow {
        val userId = getCurrentUserId()
        if (userId == null) {
            close(Exception("Not logged in"))
            return@callbackFlow
        }
        
        // Simple query without orderBy to avoid index requirement
        // Sort client-side instead
        val listener = chatsCollection
            .whereArrayContains("participants", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Log the error but don't crash - just emit empty list
                    android.util.Log.e("ChatService", "Error observing chats: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val chats = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatConversation::class.java)
                }?.sortedByDescending { it.lastMessageTime } ?: emptyList()
                
                trySend(chats)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Mark messages as read
     */
    suspend fun markAsRead(chatId: String): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("Not logged in"))
            
            val chatRef = chatsCollection.document(chatId)
            val chatDoc = chatRef.get().await()
            val unreadCount = (chatDoc.get("unreadCount") as? Map<*, *>) ?: emptyMap<String, Int>()
            
            val updatedUnreadCount = unreadCount.toMutableMap()
            updatedUnreadCount[userId] = 0
            
            chatRef.update("unreadCount", updatedUnreadCount).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking as read", e)
            Result.failure(e)
        }
    }
    
    /**
     * Update message status (SENT -> DELIVERED -> SEEN)
     */
    suspend fun updateMessageStatus(messageId: String, status: MessageStatus): Result<Unit> {
        return try {
            messagesCollection.document(messageId)
                .update("status", status.name)
                .await()
            
            Log.d(TAG, "Message status updated: $messageId -> $status")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating message status", e)
            Result.failure(e)
        }
    }
    
    /**
     * Mark all messages in a chat as delivered for the current user
     */
    suspend fun markMessagesAsDelivered(chatId: String): Result<Unit> {
        return try {
            val currentUserId = getCurrentUserId() ?: return Result.failure(Exception("Not logged in"))
            
            // Get all messages in chat not sent by current user with SENT status
            val messages = messagesCollection
                .whereEqualTo("chatId", chatId)
                .whereNotEqualTo("senderId", currentUserId)
                .whereEqualTo("status", MessageStatus.SENT.name)
                .get()
                .await()
            
            // Update each message to DELIVERED
            messages.documents.forEach { doc ->
                messagesCollection.document(doc.id)
                    .update("status", MessageStatus.DELIVERED.name)
                    .await()
            }
            
            Log.d(TAG, "Marked ${messages.documents.size} messages as delivered in chat $chatId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking messages as delivered", e)
            Result.failure(e)
        }
    }
    
    /**
     * Mark all messages in a chat as seen for the current user
     */
    suspend fun markMessagesAsSeen(chatId: String): Result<Unit> {
        return try {
            val currentUserId = getCurrentUserId() ?: return Result.failure(Exception("Not logged in"))
            
            // Get all messages in chat not sent by current user
            val messages = messagesCollection
                .whereEqualTo("chatId", chatId)
                .whereNotEqualTo("senderId", currentUserId)
                .get()
                .await()
            
            // Update each message to SEEN and mark as read
            messages.documents.forEach { doc ->
                messagesCollection.document(doc.id)
                    .update(mapOf(
                        "status" to MessageStatus.SEEN.name,
                        "read" to true
                    ))
                    .await()
            }
            
            // Also update chat unread count
            markAsRead(chatId)
            
            Log.d(TAG, "Marked ${messages.documents.size} messages as seen in chat $chatId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking messages as seen", e)
            Result.failure(e)
        }
    }
}
