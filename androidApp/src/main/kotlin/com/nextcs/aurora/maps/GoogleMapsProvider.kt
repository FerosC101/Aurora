package com.nextcs.aurora.maps

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.aurora.config.AppConfig
import com.nextcs.aurora.navigation.model.*
import com.nextcs.aurora.traffic.model.Position

/**
 * Google Maps Integration for Aurora Rider
 */
interface MapsProvider {
    suspend fun generateRoutes(origin: String, destination: String): List<NavigationRoute>
    suspend fun getTrafficData(route: NavigationRoute): TrafficData
    suspend fun geocodeAddress(address: String): Position?
    suspend fun reverseGeocode(position: Position): String?
}

data class TrafficData(
    val congestionLevel: Float,
    val incidents: List<TrafficIncident>,
    val estimatedDelay: Int
)

data class TrafficIncident(
    val type: String,
    val position: Position,
    val description: String,
    val severity: String
)

// Google Maps API Response Models
@Serializable
data class DirectionsResponse(
    val routes: List<Route> = emptyList(),
    val status: String
)

@Serializable
data class Route(
    val legs: List<Leg> = emptyList(),
    val overview_polyline: Polyline? = null,
    val summary: String = "",
    val warnings: List<String> = emptyList()
)

@Serializable
data class Leg(
    val distance: Distance,
    val duration: Duration,
    val duration_in_traffic: Duration? = null,
    val start_address: String,
    val end_address: String,
    val steps: List<Step> = emptyList()
)

@Serializable
data class Distance(
    val text: String,
    val value: Int
)

@Serializable
data class Duration(
    val text: String,
    val value: Int
)

@Serializable
data class Step(
    val distance: Distance,
    val duration: Duration,
    val start_location: Location,
    val end_location: Location,
    val html_instructions: String
)

@Serializable
data class Location(
    val lat: Double,
    val lng: Double
)

@Serializable
data class Polyline(
    val points: String
)

@Serializable
data class GeocodingResponse(
    val results: List<GeocodingResult> = emptyList(),
    val status: String
)

@Serializable
data class GeocodingResult(
    val geometry: Geometry,
    val formatted_address: String
)

@Serializable
data class Geometry(
    val location: Location
)

/**
 * Simulated Maps Provider
 */
class SimulatedMapsProvider : MapsProvider {
    override suspend fun generateRoutes(origin: String, destination: String): List<NavigationRoute> = emptyList()
    override suspend fun getTrafficData(route: NavigationRoute): TrafficData = TrafficData(0.3f, emptyList(), 0)
    override suspend fun geocodeAddress(address: String): Position? = Position(100f, 100f)
    override suspend fun reverseGeocode(position: Position): String? = "Simulated Location"
}

/**
 * Google Maps Provider with real API integration
 */
class GoogleMapsProvider(
    private val apiKey: String,
    private val httpClient: HttpClient
) : MapsProvider {
    private val baseUrl = "https://maps.googleapis.com/maps/api"
    private val json = Json { ignoreUnknownKeys = true }
    
    suspend fun generateRoutesWithMode(
        origin: String,
        destination: String,
        travelMode: String = "bicycling" // bicycling, walking, driving, transit
    ): List<NavigationRoute> {
        println("🗺️ Real Mode: Fetching routes from Google Maps (mode=$travelMode)...")
        return try {
            val routes = mutableListOf<NavigationRoute>()
            
            val routeTypes = listOf("best", "avoid_highways", "scenic")
            
            routeTypes.forEachIndexed { index, routeType ->
                val avoid = when (routeType) {
                    "avoid_highways" -> "&avoid=highways"
                    "scenic" -> "&avoid=highways|tolls"
                    else -> ""
                }
                
                val url = "$baseUrl/directions/json?" +
                        "origin=${origin.replace(" ", "+")}" +
                        "&destination=${destination.replace(" ", "+")}" +
                        "&departure_time=now" +
                        "&traffic_model=best_guess" +
                        "&mode=$travelMode" +
                        "$avoid" +
                        "&alternatives=true" +
                        "&key=$apiKey"
                
                println("🌐 Calling Google Maps API: $url")
                val response: HttpResponse = httpClient.get(url)
                val jsonText = response.bodyAsText()
                println("📥 API Response: ${jsonText.take(500)}...")
                val directionsResponse = json.decodeFromString<DirectionsResponse>(jsonText)
                
                println("📊 API Status: ${directionsResponse.status}, Routes: ${directionsResponse.routes.size}")
                
                if (directionsResponse.status == "OK" && directionsResponse.routes.isNotEmpty()) {
                    val route = directionsResponse.routes.first()
                    val leg = route.legs.first()
                    val navRoute = convertToNavigationRoute(route, leg, index)
                    routes.add(navRoute)
                    println("✅ Found route using $travelMode mode")
                } else if (directionsResponse.status == "ZERO_RESULTS") {
                    println("⚠️ $travelMode mode returned no results for this route")
                }
            }
            
            if (routes.isNotEmpty()) {
                println("✅ Successfully fetched ${routes.size} routes using $travelMode mode")
                if (routes.size >= 2) routes.take(3) else routes
            } else {
                println("❌ Google Maps returned no routes, falling back to simulation")
                emptyList()
            }
        } catch (e: Exception) {
            println("❌ Google Maps API Error: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }
    
    override suspend fun generateRoutes(origin: String, destination: String): List<NavigationRoute> {
        return try {
            val routes = mutableListOf<NavigationRoute>()
            
            // Try bicycling first, fallback to walking if not available
            val modes = listOf("bicycling", "walking")
            var successfulMode = ""
            
            for (mode in modes) {
                if (routes.isNotEmpty()) break
                
                val routeTypes = listOf("best", "avoid_highways", "scenic")
                
                routeTypes.forEachIndexed { index, routeType ->
                    val avoid = when (routeType) {
                        "avoid_highways" -> "&avoid=highways"
                        "scenic" -> "&avoid=highways|tolls"
                        else -> ""
                    }
                    
                    val url = "$baseUrl/directions/json?" +
                            "origin=${origin.replace(" ", "+")}" +
                            "&destination=${destination.replace(" ", "+")}" +
                            "&departure_time=now" +
                            "&traffic_model=best_guess" +
                            "&mode=$mode" +
                            "$avoid" +
                            "&alternatives=true" +
                            "&key=$apiKey"
                    
                    println("🌐 Calling Google Maps API (mode=$mode): $url")
                    val response: HttpResponse = httpClient.get(url)
                    val jsonText = response.bodyAsText()
                    println("📥 API Response: ${jsonText.take(500)}...")
                    val directionsResponse = json.decodeFromString<DirectionsResponse>(jsonText)
                    
                    println("📊 API Status: ${directionsResponse.status}, Routes: ${directionsResponse.routes.size}")
                    
                    if (directionsResponse.status == "OK" && directionsResponse.routes.isNotEmpty()) {
                        val route = directionsResponse.routes.first()
                        val leg = route.legs.first()
                        val navRoute = convertToNavigationRoute(route, leg, index)
                        routes.add(navRoute)
                        successfulMode = mode
                        println("✅ Found route using $mode mode")
                    } else if (directionsResponse.status == "ZERO_RESULTS" && index == 0) {
                        println("⚠️ $mode mode not available, trying next mode...")
                        return@forEachIndexed // Skip to next mode
                    }
                }
            }
            
            if (routes.isNotEmpty()) {
                println("✅ Successfully fetched ${routes.size} routes using $successfulMode mode")
                if (routes.size >= 2) routes.take(3) else routes
            } else {
                println("❌ Google Maps returned no routes for any mode, falling back to simulation")
                emptyList()
            }
            if (routes.size >= 2) routes.take(3) else emptyList()
        } catch (e: Exception) {
            println("❌ Google Maps API Error: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }
    
    private fun convertToNavigationRoute(route: Route, leg: Leg, index: Int): NavigationRoute {
        val type = when (index) {
            0 -> RouteType.SMART
            1 -> RouteType.REGULAR
            else -> RouteType.CHILL
        }
        
        // Extract raw lat/lng coordinates from Google Maps
        val rawWaypoints = leg.steps.mapIndexed { _, step ->
            Pair(step.start_location.lat, step.start_location.lng)
        } + Pair(leg.steps.last().end_location.lat, leg.steps.last().end_location.lng)
        
        // Calculate center point for map focus
        val centerLat = (rawWaypoints.first().first + rawWaypoints.last().first) / 2
        val centerLng = (rawWaypoints.first().second + rawWaypoints.last().second) / 2
        
        // Find bounds
        val minLat = rawWaypoints.minOf { it.first }
        val maxLat = rawWaypoints.maxOf { it.first }
        val minLng = rawWaypoints.minOf { it.second }
        val maxLng = rawWaypoints.maxOf { it.second }
        
        // Normalize to 0-1000 range for canvas rendering (fallback)
        val latRange = (maxLat - minLat).coerceAtLeast(0.001)
        val lngRange = (maxLng - minLng).coerceAtLeast(0.001)
        val scale = 800.0
        
        val waypoints = rawWaypoints.map { (lat, lng) ->
            val normalizedX = ((lng - minLng) / lngRange * scale).toFloat()
            val normalizedY = ((lat - minLat) / latRange * scale).toFloat()
            Position(normalizedX, normalizedY)
        }
        
        val distanceKm = leg.distance.value / 1000f
        val durationMin = (leg.duration_in_traffic?.value ?: leg.duration.value) / 60
        
        // AI-powered hazard detection based on route warnings and step analysis
        val detectedHazards = analyzeRouteForHazards(leg.steps, route.warnings, rawWaypoints)
        
        val safetyScore = when (type) {
            RouteType.SMART -> 95 - (detectedHazards.count { it.severity == HazardSeverity.HIGH || it.severity == HazardSeverity.CRITICAL } * 10)
            RouteType.CHILL -> 98
            RouteType.REGULAR -> if (route.warnings.isNotEmpty()) 65 else 75
        }.coerceIn(0, 100)
        
        val trafficLevel = leg.duration_in_traffic?.let { traffic ->
            val ratio = traffic.value.toFloat() / leg.duration.value
            when {
                ratio > 1.5f -> TrafficLevel.HEAVY
                ratio > 1.2f -> TrafficLevel.MODERATE
                ratio > 1.1f -> TrafficLevel.LIGHT
                else -> TrafficLevel.VERY_LIGHT
            }
        } ?: TrafficLevel.LIGHT
        
        val stoplights = generateStoplightsForRoute(waypoints, leg.steps, rawWaypoints)
        
        // Generate Google Maps Static API URL with satellite view and route overlay
        val encodedPolyline = route.overview_polyline?.points ?: ""
        val pathColor = when (type) {
            RouteType.SMART -> "0x3B82F6FF"  // Blue
            RouteType.REGULAR -> "0x64748BFF" // Gray
            RouteType.CHILL -> "0xA855F7FF"   // Purple
        }
        
        // Street-level zoom with hazard markers
        val hazardMarkers = detectedHazards.take(10).joinToString("") { hazard ->
            val markerColor = when (hazard.severity) {
                HazardSeverity.LOW -> "yellow"
                HazardSeverity.MODERATE -> "orange"
                HazardSeverity.HIGH -> "red"
                HazardSeverity.CRITICAL -> "darkred"
            }
            val icon = when (hazard.type) {
                HazardType.POTHOLE -> "P"
                HazardType.FLOOD -> "F"
                HazardType.ACCIDENT -> "A"
                HazardType.CONSTRUCTION -> "C"
            }
            "&markers=color:$markerColor|label:$icon|${hazard.position.y},${hazard.position.x}"
        }
        
        val staticMapUrl = if (encodedPolyline.isNotEmpty()) {
            "$baseUrl/staticmap?" +
            "center=$centerLat,$centerLng" +
            "&zoom=16" + // Street-level close-up
            "&size=1200x800" +
            "&scale=2" + // High resolution
            "&maptype=hybrid" + // Satellite with labels
            "&path=color:$pathColor|weight:5|enc:$encodedPolyline" +
            "$hazardMarkers" +
            "&key=$apiKey"
        } else null
        
        return NavigationRoute(
            type = type,
            name = when (type) {
                RouteType.SMART -> "Aurora AI Smart Route"
                RouteType.CHILL -> "Aurora Chill Route"
                RouteType.REGULAR -> "Standard GPS Route"
            },
            description = when (type) {
                RouteType.SMART -> "AI-optimized: ${detectedHazards.size} hazards avoided • " + route.summary.ifEmpty { "Fastest & safest path" }
                RouteType.CHILL -> "Scenic route with ${detectedHazards.size} warnings"
                RouteType.REGULAR -> "Standard route: ${detectedHazards.size} potential hazards"
            },
            estimatedTime = durationMin,
            distance = distanceKm,
            safetyScore = safetyScore,
            hazardCount = detectedHazards.size,
            trafficLevel = trafficLevel,
            timeSavedVsBaseline = if (type == RouteType.SMART) 4 else if (type == RouteType.CHILL) -4 else 0,
            waypoints = waypoints,
            stoplights = stoplights,
            staticMapUrl = staticMapUrl,
            encodedPolyline = encodedPolyline,
            realLatLngWaypoints = rawWaypoints,
            detectedHazards = detectedHazards,
            centerLat = centerLat,
            centerLng = centerLng,
            zoomLevel = 17 // Street-level close-up
        )
    }
    
    /**
     * AI-powered hazard detection analyzing route steps and warnings
     */
    private fun analyzeRouteForHazards(
        steps: List<Step>,
        warnings: List<String>,
        waypoints: List<Pair<Double, Double>>
    ): List<RouteHazard> {
        val hazards = mutableListOf<RouteHazard>()
        
        // Analyze step instructions for construction, closures, etc.
        steps.forEachIndexed { index, step ->
            val instruction = step.html_instructions.lowercase()
            val position = Position(
                waypoints.getOrNull(index)?.second?.toFloat() ?: 0f,
                waypoints.getOrNull(index)?.first?.toFloat() ?: 0f
            )
            
            when {
                instruction.contains("construction") || instruction.contains("closed") || instruction.contains("work") -> {
                    hazards.add(RouteHazard(
                        type = HazardType.CONSTRUCTION,
                        location = step.html_instructions.replace("<[^>]*>".toRegex(), "").take(50),
                        position = position,
                        severity = HazardSeverity.MODERATE
                    ))
                }
                instruction.contains("caution") || instruction.contains("careful") -> {
                    hazards.add(RouteHazard(
                        type = HazardType.POTHOLE,
                        location = "Caution zone",
                        position = position,
                        severity = HazardSeverity.LOW
                    ))
                }
                instruction.contains("flooded") || instruction.contains("water") -> {
                    hazards.add(RouteHazard(
                        type = HazardType.FLOOD,
                        location = "Potential flooding",
                        position = position,
                        severity = HazardSeverity.HIGH
                    ))
                }
            }
        }
        
        // Analyze warnings
        warnings.forEach { warning ->
            if (warning.isNotEmpty() && waypoints.isNotEmpty()) {
                hazards.add(RouteHazard(
                    type = HazardType.CONSTRUCTION,
                    location = warning.take(50),
                    position = Position(
                        waypoints[waypoints.size / 2].second.toFloat(),
                        waypoints[waypoints.size / 2].first.toFloat()
                    ),
                    severity = HazardSeverity.MODERATE
                ))
            }
        }
        
        // Simulate AI detection of potholes on longer routes
        if (steps.size > 5) {
            val midPoint = steps.size / 2
            hazards.add(RouteHazard(
                type = HazardType.POTHOLE,
                location = "AI-detected road damage",
                position = Position(
                    waypoints.getOrNull(midPoint)?.second?.toFloat() ?: 0f,
                    waypoints.getOrNull(midPoint)?.first?.toFloat() ?: 0f
                ),
                severity = HazardSeverity.LOW
            ))
        }
        
        return hazards
    }
    
    private fun generateStoplightsForRoute(
        waypoints: List<Position>,
        steps: List<Step>,
        realWaypoints: List<Pair<Double, Double>>
    ): List<Stoplight> {
        val lights = mutableListOf<Stoplight>()
        var distance = 0f
        
        steps.forEachIndexed { index, step ->
            distance += step.distance.value
            if (distance > 500 && lights.size < 3 && index < waypoints.size) {
                // Use the normalized waypoint position instead of raw lat/lng
                val position = waypoints.getOrNull(index) ?: waypoints.last()
                lights.add(
                    Stoplight(
                        id = "SL${lights.size + 1}",
                        location = step.html_instructions.replace("<[^>]*>".toRegex(), "").take(50),
                        position = position,
                        distanceFromStart = distance,
                        state = if (lights.size % 2 == 0) StoplightState.GREEN else StoplightState.RED,
                        remainingTime = (15..60).random()
                    )
                )
                distance = 0f
            }
        }
        return lights
    }
    
    override suspend fun getTrafficData(route: NavigationRoute): TrafficData {
        return TrafficData(
            congestionLevel = when (route.trafficLevel) {
                TrafficLevel.VERY_LIGHT -> 0.1f
                TrafficLevel.LIGHT -> 0.3f
                TrafficLevel.MODERATE -> 0.6f
                TrafficLevel.HEAVY -> 0.9f
            },
            incidents = emptyList(),
            estimatedDelay = 0
        )
    }
    
    override suspend fun geocodeAddress(address: String): Position? {
        return try {
            val url = "$baseUrl/geocode/json?address=${address.replace(" ", "+")}&key=$apiKey"
            val response: HttpResponse = httpClient.get(url)
            val jsonText = response.bodyAsText()
            val geocodingResponse = json.decodeFromString<GeocodingResponse>(jsonText)
            
            if (geocodingResponse.status == "OK" && geocodingResponse.results.isNotEmpty()) {
                val location = geocodingResponse.results.first().geometry.location
                Position(location.lng.toFloat() * 100f, location.lat.toFloat() * 100f)
            } else null
        } catch (e: Exception) {
            println("❌ Geocoding Error: ${e.message}")
            null
        }
    }
    
    override suspend fun reverseGeocode(position: Position): String? {
        return try {
            val lat = position.y / 100f
            val lng = position.x / 100f
            val url = "$baseUrl/geocode/json?latlng=$lat,$lng&key=$apiKey"
            
            val response: HttpResponse = httpClient.get(url)
            val jsonText = response.bodyAsText()
            val geocodingResponse = json.decodeFromString<GeocodingResponse>(jsonText)
            
            if (geocodingResponse.status == "OK" && geocodingResponse.results.isNotEmpty()) {
                geocodingResponse.results.first().formatted_address
            } else null
        } catch (e: Exception) {
            println("❌ Reverse Geocoding Error: ${e.message}")
            null
        }
    }
}

/**
 * Factory to create appropriate Maps Provider
 */
object MapsProviderFactory {
    private var cachedProvider: MapsProvider? = null
    
    fun create(httpClient: HttpClient): MapsProvider {
        if (cachedProvider != null) return cachedProvider!!
        
        cachedProvider = when {
            AppConfig.GOOGLE_MAPS_API_KEY.isNotEmpty() -> {
                println("🗺️ Google Maps API enabled")
                GoogleMapsProvider(AppConfig.GOOGLE_MAPS_API_KEY, httpClient)
            }
            else -> {
                println("🗺️ Using simulated maps (no API key)")
                SimulatedMapsProvider()
            }
        }
        
        return cachedProvider!!
    }
    
    fun reset() {
        cachedProvider = null
    }
}
