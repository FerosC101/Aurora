package com.nextcs.aurora.location

import android.content.Context
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Service for Google Places Autocomplete API
 * Provides location search and autocomplete suggestions
 */
class PlacesAutocompleteService(private val context: Context) {
    private val placesClient: PlacesClient
    private var sessionToken: AutocompleteSessionToken = AutocompleteSessionToken.newInstance()

    init {
        // Initialize Places SDK
        if (!Places.isInitialized()) {
            val apiKey = try {
                val appInfo = context.packageManager.getApplicationInfo(
                    context.packageName,
                    android.content.pm.PackageManager.GET_META_DATA
                )
                appInfo.metaData?.getString("com.google.android.geo.API_KEY") ?: ""
            } catch (e: Exception) {
                Log.e("PlacesAutocomplete", "Failed to get API key", e)
                ""
            }
            Places.initialize(context, apiKey)
        }
        placesClient = Places.createClient(context)
    }

    /**
     * Search for location predictions based on query
     */
    suspend fun searchLocations(query: String): List<PlacePrediction> = suspendCancellableCoroutine { continuation ->
        if (query.isBlank()) {
            continuation.resume(emptyList())
            return@suspendCancellableCoroutine
        }

        val request = FindAutocompletePredictionsRequest.builder()
            .setSessionToken(sessionToken)
            .setQuery(query)
            .build()

        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                val predictions = response.autocompletePredictions.map { prediction ->
                    PlacePrediction(
                        placeId = prediction.placeId,
                        primaryText = prediction.getPrimaryText(null).toString(),
                        secondaryText = prediction.getSecondaryText(null).toString(),
                        fullText = prediction.getFullText(null).toString()
                    )
                }
                Log.d("PlacesAutocomplete", "Found ${predictions.size} predictions for '$query'")
                continuation.resume(predictions)
            }
            .addOnFailureListener { exception ->
                Log.e("PlacesAutocomplete", "Error searching locations", exception)
                continuation.resumeWithException(exception)
            }
    }

    /**
     * Get detailed place information including coordinates
     */
    suspend fun getPlaceDetails(placeId: String): PlaceDetails? = suspendCancellableCoroutine { continuation ->
        val placeFields = listOf(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG
        )

        val request = FetchPlaceRequest.builder(placeId, placeFields).build()

        placesClient.fetchPlace(request)
            .addOnSuccessListener { response ->
                val place = response.place
                val details = PlaceDetails(
                    placeId = place.id ?: "",
                    name = place.name ?: "",
                    address = place.address ?: "",
                    latLng = place.latLng
                )
                Log.d("PlacesAutocomplete", "Got place details: ${details.name} at ${details.latLng}")
                
                // Generate new session token for next search
                sessionToken = AutocompleteSessionToken.newInstance()
                
                continuation.resume(details)
            }
            .addOnFailureListener { exception ->
                Log.e("PlacesAutocomplete", "Error fetching place details", exception)
                continuation.resume(null)
            }
    }

    /**
     * Search for a location and get its coordinates in one call
     */
    suspend fun searchAndGetCoordinates(query: String): Pair<LatLng, String>? {
        return try {
            val predictions = searchLocations(query)
            if (predictions.isEmpty()) {
                Log.d("PlacesAutocomplete", "No predictions found for '$query'")
                return null
            }

            // Get details for first result
            val firstPrediction = predictions.first()
            val details = getPlaceDetails(firstPrediction.placeId)
            
            if (details?.latLng != null) {
                Pair(details.latLng, details.address.ifEmpty { details.name })
            } else {
                Log.d("PlacesAutocomplete", "No coordinates found for ${firstPrediction.fullText}")
                null
            }
        } catch (e: Exception) {
            Log.e("PlacesAutocomplete", "Error in searchAndGetCoordinates", e)
            null
        }
    }
}

/**
 * Represents a place prediction from autocomplete
 */
data class PlacePrediction(
    val placeId: String,
    val primaryText: String,      // e.g., "Manila City Hall"
    val secondaryText: String,    // e.g., "Manila, Metro Manila, Philippines"
    val fullText: String           // Complete text
)

/**
 * Detailed place information with coordinates
 */
data class PlaceDetails(
    val placeId: String,
    val name: String,
    val address: String,
    val latLng: LatLng?
)
