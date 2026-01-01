package com.nextcs.aurora.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

class LocationService(private val context: Context) {
    
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun getLastKnownLocation(): LatLng? {
        if (!hasLocationPermission()) return null
        
        try {
            val gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            
            val bestLocation = when {
                gpsLocation != null && networkLocation != null -> {
                    if (gpsLocation.time > networkLocation.time) gpsLocation else networkLocation
                }
                gpsLocation != null -> gpsLocation
                networkLocation != null -> networkLocation
                else -> null
            }
            
            return bestLocation?.let { LatLng(it.latitude, it.longitude) }
        } catch (e: SecurityException) {
            return null
        }
    }
    
    fun getCurrentLocationFlow(): Flow<LatLng> = callbackFlow {
        if (!hasLocationPermission()) {
            close()
            return@callbackFlow
        }
        
        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(LatLng(location.latitude, location.longitude))
            }
            
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }
        
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L, // 1 second
                10f,   // 10 meters
                locationListener
            )
            
            // Also request from network for faster initial fix
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                1000L,
                10f,
                locationListener
            )
        } catch (e: SecurityException) {
            close()
        }
        
        awaitClose {
            try {
                locationManager.removeUpdates(locationListener)
            } catch (e: SecurityException) {
                // Ignore
            }
        }
    }
    
    fun formatLocationToAddress(latLng: LatLng): String {
        // Simple formatting - in production, use Geocoder for reverse geocoding
        return "Lat: ${String.format("%.4f", latLng.latitude)}, Lng: ${String.format("%.4f", latLng.longitude)}"
    }
    
    suspend fun reverseGeocode(latLng: LatLng): String {
        if (!Geocoder.isPresent()) {
            return formatLocationToAddress(latLng)
        }
        
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Use new async API for Android 13+
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(
                        latLng.latitude,
                        latLng.longitude,
                        1,
                        object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: List<Address>) {
                                val address = addresses.firstOrNull()
                                val result = if (address != null) {
                                    buildAddressString(address)
                                } else {
                                    formatLocationToAddress(latLng)
                                }
                                continuation.resume(result)
                            }
                            
                            override fun onError(errorMessage: String?) {
                                continuation.resume(formatLocationToAddress(latLng))
                            }
                        }
                    )
                }
            } else {
                // Use synchronous API for older versions
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                val address = addresses?.firstOrNull()
                if (address != null) {
                    buildAddressString(address)
                } else {
                    formatLocationToAddress(latLng)
                }
            }
        } catch (e: Exception) {
            formatLocationToAddress(latLng)
        }
    }
    
    private fun buildAddressString(address: Address): String {
        val parts = mutableListOf<String>()
        
        // Add street address
        if (address.thoroughfare != null) {
            val street = if (address.subThoroughfare != null) {
                "${address.subThoroughfare} ${address.thoroughfare}"
            } else {
                address.thoroughfare
            }
            parts.add(street)
        }
        
        // Add locality (city)
        if (address.locality != null) {
            parts.add(address.locality)
        }
        
        // Add admin area (state/province)
        if (address.adminArea != null) {
            parts.add(address.adminArea)
        }
        
        // Add country
        if (address.countryName != null) {
            parts.add(address.countryName)
        }
        
        return if (parts.isNotEmpty()) {
            parts.joinToString(", ")
        } else {
            address.getAddressLine(0) ?: "Unknown Address"
        }
    }
}
