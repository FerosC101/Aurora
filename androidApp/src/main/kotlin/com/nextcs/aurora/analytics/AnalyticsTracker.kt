package com.nextcs.aurora.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

class AnalyticsTracker {
    
    private val analytics: FirebaseAnalytics = Firebase.analytics
    
    /**
     * Track navigation started
     */
    fun trackNavigationStarted(
        origin: String,
        destination: String,
        distance: Double,
        estimatedDuration: Long
    ) {
        val params = Bundle().apply {
            putString("origin", origin)
            putString("destination", destination)
            putDouble("distance_km", distance / 1000.0)
            putLong("estimated_duration_min", estimatedDuration / 60)
        }
        analytics.logEvent("navigation_started", params)
    }
    
    /**
     * Track navigation completed
     */
    fun trackNavigationCompleted(
        distance: Double,
        actualDuration: Long,
        hazardsEncountered: Int
    ) {
        val params = Bundle().apply {
            putDouble("distance_km", distance / 1000.0)
            putLong("actual_duration_min", actualDuration / 60)
            putInt("hazards_encountered", hazardsEncountered)
        }
        analytics.logEvent("navigation_completed", params)
    }
    
    /**
     * Track route saved
     */
    fun trackRouteSaved(routeName: String, isFavorite: Boolean) {
        val params = Bundle().apply {
            putString("route_name", routeName)
            putBoolean("is_favorite", isFavorite)
        }
        analytics.logEvent("route_saved", params)
    }
    
    /**
     * Track AI recommendation used
     */
    fun trackAIRecommendationUsed(recommendationType: String) {
        val params = Bundle().apply {
            putString("recommendation_type", recommendationType)
        }
        analytics.logEvent("ai_recommendation_used", params)
    }
    
    /**
     * Track hazard detected
     */
    fun trackHazardDetected(hazardType: String, severity: String) {
        val params = Bundle().apply {
            putString("hazard_type", hazardType)
            putString("severity", severity)
        }
        analytics.logEvent("hazard_detected", params)
    }
    
    /**
     * Set user properties
     */
    fun setUserProperties(userId: String, isPremium: Boolean) {
        analytics.setUserId(userId)
        analytics.setUserProperty("premium_user", isPremium.toString())
    }
}
