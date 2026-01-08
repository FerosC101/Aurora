package com.nextcs.aurora.alerts

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.google.android.gms.maps.model.LatLng
import com.nextcs.aurora.R
import com.nextcs.aurora.navigation.DirectionsService
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

data class ActiveRoute(
    val destination: LatLng,
    val currentDurationMinutes: Int,
    val origin: LatLng,
    val vehicleMode: String = "driving"
)

class RouteChangeAlertService(private val context: Context) {
    
    companion object {
        private const val CHANNEL_ID = "route_alerts"
        private const val CHANNEL_NAME = "Route Change Alerts"
        const val NOTIFICATION_ID = 1002
        private const val TIME_SAVE_THRESHOLD_MINUTES = 10
    }
    
    private val directionsService = DirectionsService(context)
    
    init {
        createNotificationChannel()
    }
    
    /**
     * Start monitoring for better routes
     */
    fun startRouteMonitoring(route: ActiveRoute) {
        val workData = workDataOf(
            "destLat" to route.destination.latitude,
            "destLng" to route.destination.longitude,
            "originLat" to route.origin.latitude,
            "originLng" to route.origin.longitude,
            "currentDuration" to route.currentDurationMinutes,
            "vehicleMode" to route.vehicleMode
        )
        
        // Check every 15 minutes during navigation
        val workRequest = PeriodicWorkRequestBuilder<RouteMonitorWorker>(
            15, TimeUnit.MINUTES
        )
            .setInputData(workData)
            .addTag("route_monitoring")
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "route_monitoring",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }
    
    /**
     * Stop monitoring
     */
    fun stopRouteMonitoring() {
        WorkManager.getInstance(context).cancelUniqueWork("route_monitoring")
    }
    
    /**
     * Check for faster alternative routes
     */
    suspend fun checkForFasterRoute(
        origin: LatLng,
        destination: LatLng,
        currentDurationMinutes: Int,
        vehicleMode: String
    ): FasterRouteInfo? {
        val result = directionsService.getDirections(origin, destination, vehicleMode = vehicleMode)
        
        result.onSuccess { routeInfo ->
            val newDurationMinutes = routeInfo.duration / 60
            
            val timeSaved = currentDurationMinutes - newDurationMinutes
            
            if (timeSaved >= TIME_SAVE_THRESHOLD_MINUTES) {
                return FasterRouteInfo(
                    timeSavedMinutes = timeSaved,
                    newDurationMinutes = newDurationMinutes
                )
            }
        }
        
        return null
    }
    
    /**
     * Show faster route notification
     */
    fun showFasterRouteNotification(
        timeSaved: Int,
        newDuration: Int
    ) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Faster Route Available! ⚡")
            .setContentText("Save $timeSaved minutes - New ETA: $newDuration min")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("A faster route is now available due to traffic changes.\n\nTime saved: $timeSaved minutes\nNew duration: $newDuration minutes\n\nTap to switch routes.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .addAction(
                R.mipmap.ic_launcher,
                "Switch Route",
                null // TODO: Add PendingIntent to switch route in app
            )
            .build()
        
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when faster routes become available"
                enableLights(true)
                enableVibration(true)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

data class FasterRouteInfo(
    val timeSavedMinutes: Int,
    val newDurationMinutes: Int
)

/**
 * WorkManager Worker for route monitoring
 */
class RouteMonitorWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {
    
    override fun doWork(): Result = runBlocking {
        val destLat = inputData.getDouble("destLat", 0.0)
        val destLng = inputData.getDouble("destLng", 0.0)
        val originLat = inputData.getDouble("originLat", 0.0)
        val originLng = inputData.getDouble("originLng", 0.0)
        val currentDuration = inputData.getInt("currentDuration", 0)
        val vehicleMode = inputData.getString("vehicleMode") ?: "driving"
        
        if (destLat == 0.0 || destLng == 0.0) {
            return@runBlocking Result.failure()
        }
        
        val routeService = RouteChangeAlertService(applicationContext)
        val fasterRoute = routeService.checkForFasterRoute(
            origin = LatLng(originLat, originLng),
            destination = LatLng(destLat, destLng),
            currentDurationMinutes = currentDuration,
            vehicleMode = vehicleMode
        )
        
        if (fasterRoute != null) {
            routeService.showFasterRouteNotification(
                fasterRoute.timeSavedMinutes,
                fasterRoute.newDurationMinutes
            )
        }
        
        Result.success()
    }
}
