package com.nextcs.aurora.services

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.nextcs.aurora.MainActivity
import com.nextcs.aurora.R

class NavigationForegroundService : Service() {
    
    companion object {
        const val CHANNEL_ID = "navigation_channel"
        const val NOTIFICATION_ID = 1
        
        const val ACTION_START_NAVIGATION = "START_NAVIGATION"
        const val ACTION_STOP_NAVIGATION = "STOP_NAVIGATION"
        const val ACTION_UPDATE_LOCATION = "UPDATE_LOCATION"
        
        const val EXTRA_DESTINATION = "destination"
        const val EXTRA_ETA = "eta"
        const val EXTRA_DISTANCE = "distance"
        
        private var isRunning = false
        
        fun isServiceRunning(): Boolean = isRunning
    }
    
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var notificationManager: NotificationManager
    
    private var currentDestination: String = ""
    private var currentETA: String = ""
    private var currentDistance: String = ""
    
    override fun onCreate() {
        super.onCreate()
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        createNotificationChannel()
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    // Broadcast location update
                    val intent = Intent(ACTION_UPDATE_LOCATION).apply {
                        putExtra("latitude", location.latitude)
                        putExtra("longitude", location.longitude)
                    }
                    sendBroadcast(intent)
                }
            }
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_NAVIGATION -> {
                currentDestination = intent.getStringExtra(EXTRA_DESTINATION) ?: "Unknown"
                currentETA = intent.getStringExtra(EXTRA_ETA) ?: "--"
                currentDistance = intent.getStringExtra(EXTRA_DISTANCE) ?: "--"
                
                startForeground(NOTIFICATION_ID, createNotification())
                startLocationUpdates()
                isRunning = true
            }
            ACTION_STOP_NAVIGATION -> {
                stopLocationUpdates()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                isRunning = false
            }
            else -> {
                // Update notification
                currentETA = intent?.getStringExtra(EXTRA_ETA) ?: currentETA
                currentDistance = intent?.getStringExtra(EXTRA_DISTANCE) ?: currentDistance
                notificationManager.notify(NOTIFICATION_ID, createNotification())
            }
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Navigation",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing navigation notifications"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val stopIntent = Intent(this, NavigationForegroundService::class.java).apply {
            action = ACTION_STOP_NAVIGATION
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Navigating to $currentDestination")
            .setContentText("$currentDistance • ETA: $currentETA")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop",
                stopPendingIntent
            )
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L // Update every 5 seconds
        ).apply {
            setMinUpdateIntervalMillis(2000L)
            setWaitForAccurateLocation(false)
        }.build()
        
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            mainLooper
        )
    }
    
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        isRunning = false
    }
}
