package com.devsusana.hometutorpro.core.utils

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {

    private const val CHANNEL_ID = "CLASS_END_CHANNEL_V2"
    private const val NOTIFICATION_ID = 101

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(com.devsusana.hometutorpro.R.string.notification_channel_name)
            val descriptionText = context.getString(com.devsusana.hometutorpro.R.string.notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: 
                          RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                setSound(soundUri, AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showTestNotification(context: Context) {
        showClassEndNotification(context, "TEST")
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
        
        // Use setExactAndAllowWhileIdle to ensure it triggers even in Doze mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.util.Log.d("NotificationHelper", "Using setExactAndAllowWhileIdle")
            alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } else {
            android.util.Log.d("NotificationHelper", "Using setExact")
            alarmManager.setExact(android.app.AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
        
        android.util.Log.d("NotificationHelper", "Notification scheduled successfully")
        return true // Successfully scheduled
    }

    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = android.content.Intent().apply {
                action = android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                data = android.net.Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        }
    }

    // Simplified: generates a notification immediately to simulate the end of class
    // (Kept for backward compatibility, but scheduleClassEndNotification should be used)
    @SuppressLint("MissingPermission")
    fun showClassEndNotification(context: Context, studentName: String) {
        android.util.Log.d("NotificationHelper", "showClassEndNotification called for: $studentName")
        
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: 
                      RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(context.getString(com.devsusana.hometutorpro.R.string.notification_title))
            .setContentText(context.getString(com.devsusana.hometutorpro.R.string.notification_text, studentName))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setFullScreenIntent(null, true) // Increases chance of showing even if locked
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, builder.build())
        
        android.util.Log.d("NotificationHelper", "Notification displayed with ID: $NOTIFICATION_ID")
    }
}
