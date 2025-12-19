package com.nextcs.aurora

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore

class AuroraApplication : Application() {
    
    private lateinit var analytics: FirebaseAnalytics
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Initialize Firestore with settings
        try {
            val firestore = Firebase.firestore
            val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                .build()
            firestore.firestoreSettings = settings
            println("✅ Firestore initialized successfully")
        } catch (e: Exception) {
            println("❌ Firestore initialization error: ${e.message}")
            e.printStackTrace()
        }
        
        // Initialize Analytics
        analytics = Firebase.analytics
        analytics.setAnalyticsCollectionEnabled(true)
        
        println("✅ Firebase initialized successfully")
    }
}
