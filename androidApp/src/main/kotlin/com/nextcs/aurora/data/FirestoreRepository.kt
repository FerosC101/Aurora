package com.nextcs.aurora.data

import com.nextcs.aurora.data.models.SavedRoute
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
    
    private val db: FirebaseFirestore = Firebase.firestore
    private val routesCollection = db.collection("routes")
    
    /**
     * Save a new route to Firestore
     */
    suspend fun saveRoute(route: SavedRoute): Result<String> {
        return try {
            val docRef = routesCollection.add(route).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get all routes for a user
     */
    suspend fun getUserRoutes(userId: String): Result<List<SavedRoute>> {
        return try {
            val snapshot = routesCollection
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val routes = snapshot.documents.mapNotNull { it.toObject<SavedRoute>() }
            Result.success(routes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get user routes as a real-time Flow
     */
    fun getUserRoutesFlow(userId: String): Flow<List<SavedRoute>> = callbackFlow {
        val listener = routesCollection
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val routes = snapshot?.documents?.mapNotNull { 
                    it.toObject<SavedRoute>() 
                } ?: emptyList()
                
                trySend(routes)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Delete a route
     */
    suspend fun deleteRoute(routeId: String): Result<Unit> {
        return try {
            routesCollection.document(routeId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update route favorite status
     */
    suspend fun toggleFavorite(routeId: String, isFavorite: Boolean): Result<Unit> {
        return try {
            routesCollection.document(routeId)
                .update("isFavorite", isFavorite)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get favorite routes
     */
    suspend fun getFavoriteRoutes(userId: String): Result<List<SavedRoute>> {
        return try {
            val snapshot = routesCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("isFavorite", true)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val routes = snapshot.documents.mapNotNull { it.toObject<SavedRoute>() }
            Result.success(routes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
