package com.nextcs.aurora.reminders

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
import com.nextcs.aurora.R
import java.util.concurrent.TimeUnit

data class DepartureReminder(
    val id: String,
    val eventName: String,
    val destination: String,
    val eventTime: Long,              // Event start time in millis
    val travelTimeMinutes: Int,       // Estimated travel time
    val bufferMinutes: Int = 10,      // Extra time buffer
    val reminderEnabled: Boolean = true
)

class DepartureReminderService(private val context: Context) {
    
    companion object {
        private const val CHANNEL_ID = "departure_reminders"
        private const val CHANNEL_NAME = "Departure Reminders"
        const val NOTIFICATION_ID = 1001
    }
    
    init {
        createNotificationChannel()
    }
    
    /**
     * Schedule a departure reminder
     */
    fun scheduleDepartureReminder(
        reminder: DepartureReminder
    ) {
        val departureTime = calculateDepartureTime(
            reminder.eventTime,
            reminder.travelTimeMinutes,
            reminder.bufferMinutes
        )
        
        val currentTime = System.currentTimeMillis()
        val delayMillis = (departureTime - currentTime).coerceAtLeast(0)
        
        // Create work request
        val workData = workDataOf(
            "reminderId" to reminder.id,
            "eventName" to reminder.eventName,
            "destination" to reminder.destination,
            "travelTime" to reminder.travelTimeMinutes
        )
        
        val workRequest = OneTimeWorkRequestBuilder<DepartureReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(workData)
            .addTag("departure_reminder_${reminder.id}")
            .build()
        
        WorkManager.getInstance(context).enqueueUniqueWork(
            "departure_${reminder.id}",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
    
    /**
     * Calculate when user should depart
     */
    private fun calculateDepartureTime(
        eventTime: Long,
        travelTimeMinutes: Int,
        bufferMinutes: Int
    ): Long {
        val totalMinutes = travelTimeMinutes + bufferMinutes
        return eventTime - (totalMinutes * 60 * 1000)
    }
    
    /**
     * Cancel a reminder
     */
    fun cancelReminder(reminderId: String) {
        WorkManager.getInstance(context).cancelUniqueWork("departure_$reminderId")
    }
    
    /**
     * Show departure notification
     */
    fun showDepartureNotification(
        eventName: String,
        destination: String,
        travelTime: Int
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
            .setContentTitle("Time to Leave! 🚗")
            .setContentText("$eventName at $destination - $travelTime min travel time")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Leave now to arrive on time at $eventName.\n\nDestination: $destination\nTravel time: $travelTime minutes")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
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
                description = "Notifications for when you should leave for events"
                enableLights(true)
                enableVibration(true)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

/**
 * WorkManager Worker for departure reminders
 */
class DepartureReminderWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {
    
    override fun doWork(): Result {
        val eventName = inputData.getString("eventName") ?: return Result.failure()
        val destination = inputData.getString("destination") ?: return Result.failure()
        val travelTime = inputData.getInt("travelTime", 0)
        
        val reminderService = DepartureReminderService(applicationContext)
        reminderService.showDepartureNotification(eventName, destination, travelTime)
        
        return Result.success()
    }
}
