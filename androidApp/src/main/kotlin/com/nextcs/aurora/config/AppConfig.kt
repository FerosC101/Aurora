package org.aurora.config

/**
 * Application configuration
 * Store API keys and external service configurations
 */
object AppConfig {
    // Google Maps Configuration
    var GOOGLE_MAPS_API_KEY: String = "" // Set this in Main.kt or via environment variable
    
    // Maps SDK type
    enum class MapSDK {
        JAVASCRIPT,  // For web-based maps
        NATIVE,      // For native platform maps
        SIMULATION   // Use simulated maps (default)
    }
    
    var MAP_SDK: MapSDK = MapSDK.SIMULATION
    
    // Location Configuration
    var USE_REAL_GPS: Boolean = false  // Enable real GPS tracking
    var SIMULATION_SPEED: Float = 1.0f // 1.0 = real-time, 10.0 = 10x speed
    
    // Traffic Configuration
    var USE_LIVE_TRAFFIC: Boolean = false
    var TRAFFIC_API_KEY: String = ""
    
    // Stoplight Configuration
    var USE_REAL_STOPLIGHT_DATA: Boolean = false
    var STOPLIGHT_API_ENDPOINT: String = ""
    
    // Feature Flags
    var ENABLE_AURORA_SHIELD: Boolean = true
    var ENABLE_TRIP_HISTORY: Boolean = true
    var ENABLE_STATISTICS: Boolean = true
    var ENABLE_AUDIO_ALERTS: Boolean = true
    
    /**
     * Initialize configuration from environment or settings
     */
    fun initialize(
        googleMapsKey: String? = null,
        useRealGPS: Boolean = false,
        useLiveTraffic: Boolean = false
    ) {
        googleMapsKey?.let { GOOGLE_MAPS_API_KEY = it }
        USE_REAL_GPS = useRealGPS
        USE_LIVE_TRAFFIC = useLiveTraffic
        
        // Set map SDK based on API key availability
        MAP_SDK = when {
            GOOGLE_MAPS_API_KEY.isNotEmpty() -> MapSDK.JAVASCRIPT
            else -> MapSDK.SIMULATION
        }
    }
}
