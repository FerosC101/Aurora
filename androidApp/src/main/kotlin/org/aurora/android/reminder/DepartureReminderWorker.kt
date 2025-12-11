package org.aurora.android.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.aurora.android.MainActivity
import org.aurora.android.R

class DepartureReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val destination = inputData.getString(KEY_DESTINATION) ?: "your destination"
        val departureTime = inputData.getLong(KEY_DEPARTURE_TIME, 0L)
        val travelDuration = inputData.getInt(KEY_TRAVEL_DURATION, 0)
        
        showNotification(destination, departureTime, travelDuration)
        
        return Result.success()
    }

    private fun showNotification(destination: String, departureTime: Long, travelDuration: Int) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Departure Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for scheduled departure times"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        // Create intent to launch app when notification is tapped
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build notification
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Time to leave!")
            .setContentText("Start navigation to $destination now. Estimated travel time: $travelDuration minutes")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "departure_reminders"
        const val NOTIFICATION_ID = 1001
        const val KEY_DESTINATION = "destination"
        const val KEY_DEPARTURE_TIME = "departure_time"
        const val KEY_TRAVEL_DURATION = "travel_duration"
    }
}
