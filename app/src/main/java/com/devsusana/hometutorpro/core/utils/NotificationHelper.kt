package com.devsusana.hometutorpro.core.utils

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {

    private const val CHANNEL_ID = "CLASS_END_CHANNEL"
    private const val NOTIFICATION_ID = 101

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(com.devsusana.hometutorpro.R.string.notification_channel_name)
            val descriptionText = context.getString(com.devsusana.hometutorpro.R.string.notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // Schedule a notification for when the class ends
    // Returns true if scheduled successfully, false if permission is needed
    @SuppressLint("ScheduleExactAlarm")
    fun scheduleClassEndNotification(context: Context, studentName: String, durationMinutes: Long): Boolean {
        android.util.Log.d("NotificationHelper", "Scheduling notification for $studentName in $durationMinutes minutes")
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        
        // On Android 12+, check if we can schedule exact alarms
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                android.util.Log.w("NotificationHelper", "Exact alarm permission not granted")
                return false // Permission not granted
            }
        }
        
        val intent = android.content.Intent(context, com.devsusana.hometutorpro.core.receiver.ClassEndReceiver::class.java).apply {
            putExtra(com.devsusana.hometutorpro.core.receiver.ClassEndReceiver.EXTRA_STUDENT_NAME, studentName)
        }
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            context,
            NOTIFICATION_ID,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000)
        android.util.Log.d("NotificationHelper", "Trigger time: $triggerTime (in ${durationMinutes} minutes)")
        
        // On Android 12+ (API 31+), use setExact since we verified permission above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            android.util.Log.d("NotificationHelper", "Using setExact (Android 12+)")
            alarmManager.setExact(android.app.AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } else {
            // For Android 11 and below, setExact works without special permission
            android.util.Log.d("NotificationHelper", "Using setExact (Android 11 and below)")
            alarmManager.setExact(android.app.AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
        
        android.util.Log.d("NotificationHelper", "Notification scheduled successfully")
        return true // Successfully scheduled
    }

    // Simplified: generates a notification immediately to simulate the end of class
    // (Kept for backward compatibility, but scheduleClassEndNotification should be used)
    @SuppressLint("MissingPermission")
    fun showClassEndNotification(context: Context, studentName: String) {
        android.util.Log.d("NotificationHelper", "showClassEndNotification called for: $studentName")
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(context.getString(com.devsusana.hometutorpro.R.string.notification_title))
            .setContentText(context.getString(com.devsusana.hometutorpro.R.string.notification_text, studentName))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, builder.build())
        
        android.util.Log.d("NotificationHelper", "Notification displayed with ID: $NOTIFICATION_ID")
    }
}
