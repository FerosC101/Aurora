package org.aurora.maps

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.aurora.config.AppConfig
import org.aurora.navigation.model.*
import org.aurora.traffic.model.Position

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
    
    override suspend fun generateRoutes(origin: String, destination: String): List<NavigationRoute> {
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
                        "&mode=bicycling" +
                        "$avoid" +
                        "&alternatives=true" +
                        "&key=$apiKey"
                
                val response: HttpResponse = httpClient.get(url)
                val jsonText = response.bodyAsText()
                val directionsResponse = json.decodeFromString<DirectionsResponse>(jsonText)
                
                if (directionsResponse.status == "OK" && directionsResponse.routes.isNotEmpty()) {
                    val route = directionsResponse.routes.first()
                    val leg = route.legs.first()
                    val navRoute = convertToNavigationRoute(route, leg, index)
                    routes.add(navRoute)
                }
            }
            
            if (routes.size >= 2) routes.take(3) else emptyList()
        } catch (e: Exception) {
            println("❌ Google Maps API Error: ${e.message}")
            emptyList()
        }
    }
    
    private fun convertToNavigationRoute(route: Route, leg: Leg, index: Int): NavigationRoute {
        val type = when (index) {
            0 -> RouteType.SMART
            1 -> RouteType.REGULAR
            else -> RouteType.CHILL
        }
        
        val waypoints = leg.steps.mapIndexed { _, step ->
            Position(step.start_location.lng.toFloat() * 100f, step.start_location.lat.toFloat() * 100f)
        } + Position(leg.steps.last().end_location.lng.toFloat() * 100f, leg.steps.last().end_location.lat.toFloat() * 100f)
        
        val distanceKm = leg.distance.value / 1000f
        val durationMin = (leg.duration_in_traffic?.value ?: leg.duration.value) / 60
        
        val safetyScore = when (type) {
            RouteType.SMART -> 95
            RouteType.CHILL -> 98
            RouteType.REGULAR -> if (route.warnings.isNotEmpty()) 65 else 75
        }
        
        val trafficLevel = leg.duration_in_traffic?.let { traffic ->
            val ratio = traffic.value.toFloat() / leg.duration.value
            when {
                ratio > 1.5f -> TrafficLevel.HEAVY
                ratio > 1.2f -> TrafficLevel.MODERATE
                ratio > 1.1f -> TrafficLevel.LIGHT
                else -> TrafficLevel.VERY_LIGHT
            }
        } ?: TrafficLevel.LIGHT
        
        val stoplights = generateStoplightsForRoute(waypoints, leg.steps)
        
        return NavigationRoute(
            type = type,
            name = when (type) {
                RouteType.SMART -> "Smart Route"
                RouteType.CHILL -> "Chill Route"
                RouteType.REGULAR -> "Regular Route"
            },
            description = when (type) {
                RouteType.SMART -> route.summary.ifEmpty { "Optimized for speed & safety" }
                RouteType.CHILL -> "Scenic route with beautiful views"
                RouteType.REGULAR -> "Direct route via main roads"
            },
            estimatedTime = durationMin,
            distance = distanceKm,
            safetyScore = safetyScore,
            hazardCount = if (type == RouteType.REGULAR) route.warnings.size.coerceAtLeast(2) else 0,
            trafficLevel = trafficLevel,
            timeSavedVsBaseline = if (type == RouteType.SMART) 4 else if (type == RouteType.CHILL) -4 else 0,
            waypoints = waypoints,
            stoplights = stoplights
        )
    }
    
    private fun generateStoplightsForRoute(waypoints: List<Position>, steps: List<Step>): List<Stoplight> {
        val lights = mutableListOf<Stoplight>()
        var distance = 0f
        
        steps.forEachIndexed { index, step ->
            distance += step.distance.value
            if (distance > 500 && lights.size < 3) {
                lights.add(
                    Stoplight(
                        id = "SL${lights.size + 1}",
                        location = step.html_instructions.replace("<[^>]*>".toRegex(), "").take(50),
                        position = Position(step.end_location.lng.toFloat() * 100f, step.end_location.lat.toFloat() * 100f),
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
