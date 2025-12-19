package com.nextcs.aurora.reminder

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class DepartureReminderService(private val context: Context) {
    
    private val workManager = WorkManager.getInstance(context)
    
    /**
     * Schedule a departure reminder
     * @param destination The destination name
     * @param departureTime The time when user should leave (epoch millis)
     * @param travelDuration Estimated travel time in minutes
     * @param reminderMinutesBefore How many minutes before departure to remind
     */
    fun scheduleReminder(
        destination: String,
        departureTime: Long,
        travelDuration: Int,
        reminderMinutesBefore: Int = 10
    ): String {
        val currentTime = System.currentTimeMillis()
        val reminderTime = departureTime - (reminderMinutesBefore * 60 * 1000)
        val delay = reminderTime - currentTime
        
        if (delay < 0) {
            throw IllegalArgumentException("Reminder time must be in the future")
        }
        
        val inputData = Data.Builder()
            .putString(DepartureReminderWorker.KEY_DESTINATION, destination)
            .putLong(DepartureReminderWorker.KEY_DEPARTURE_TIME, departureTime)
            .putInt(DepartureReminderWorker.KEY_TRAVEL_DURATION, travelDuration)
            .build()
        
        val workRequest = OneTimeWorkRequestBuilder<DepartureReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag(TAG_DEPARTURE_REMINDER)
            .build()
        
        workManager.enqueue(workRequest)
        
        return workRequest.id.toString()
    }
    
    /**
     * Cancel a scheduled reminder
     */
    fun cancelReminder(workId: String) {
        workManager.cancelWorkById(java.util.UUID.fromString(workId))
    }
    
    /**
     * Cancel all departure reminders
     */
    fun cancelAllReminders() {
        workManager.cancelAllWorkByTag(TAG_DEPARTURE_REMINDER)
    }
    
    companion object {
        private const val TAG_DEPARTURE_REMINDER = "departure_reminder"
    }
}
