package org.aurora.android.sensors

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class SpeedData(
    val currentSpeed: Float = 0f, // km/h
    val speedLimit: Int? = null, // km/h
    val isExceeding: Boolean = false,
    val location: Location? = null
)

class SpeedMonitor(private val context: Context) {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    
    private val _speedData = MutableStateFlow(SpeedData())
    val speedData: StateFlow<SpeedData> = _speedData
    
    private var currentSpeedLimit: Int? = null
    
    private val locationListener = LocationListener { location ->
        updateSpeed(location)
    }

    fun setSpeedLimit(limit: Int?) {
        currentSpeedLimit = limit
        updateSpeedData()
    }

    private fun updateSpeed(location: Location) {
        val speedMps = location.speed // meters per second
        val speedKmh = (speedMps * 3.6f).coerceAtLeast(0f) // Convert to km/h
        
        _speedData.value = SpeedData(
            currentSpeed = speedKmh,
            speedLimit = currentSpeedLimit,
            isExceeding = currentSpeedLimit?.let { speedKmh > it } ?: false,
            location = location
        )
    }

    private fun updateSpeedData() {
        _speedData.value = _speedData.value.copy(
            speedLimit = currentSpeedLimit,
            isExceeding = currentSpeedLimit?.let { _speedData.value.currentSpeed > it } ?: false
        )
    }

    fun startMonitoring() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000, // 1 second
                0f, // 0 meters
                locationListener
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopMonitoring() {
        try {
            locationManager.removeUpdates(locationListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _speedData.value = SpeedData()
    }

    fun getCurrentSpeed(): Float = _speedData.value.currentSpeed
    
    fun isExceedingLimit(): Boolean = _speedData.value.isExceeding
}
