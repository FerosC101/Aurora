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

data class ChatMessage(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val read: Boolean = false
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
                message = message
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
        val listener = messagesCollection
            .whereEqualTo("chatId", chatId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)
                } ?: emptyList()
                
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
        
        val listener = chatsCollection
            .whereArrayContains("participants", userId)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val chats = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatConversation::class.java)
                } ?: emptyList()
                
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
}
