package com.devsusana.hometutorpro.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.devsusana.hometutorpro.core.utils.NotificationHelper
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver to handle scheduled class end notifications.
 * Uses [goAsync] to perform preference checks in a background coroutine
 * without blocking the receiver's main execution thread.
 */
class ClassEndReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.d("ClassEndReceiver", "onReceive called - alarm triggered!")
        
        val pendingResult = goAsync()
        
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                // Check if notifications are enabled in settings
                val settingsRepository = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    com.devsusana.hometutorpro.di.SettingsRepositoryEntryPoint::class.java
                ).settingsRepository()
                
                val areNotificationsEnabled = settingsRepository.classEndNotificationsFlow.first()
                
                if (!areNotificationsEnabled) {
                    android.util.Log.d("ClassEndReceiver", "Notifications are disabled in settings, skipping.")
                    return@launch
                }

                val studentName = intent.getStringExtra(EXTRA_STUDENT_NAME) 
                    ?: context.getString(com.devsusana.hometutorpro.R.string.student_default_name)
                android.util.Log.d("ClassEndReceiver", "Showing notification for student: $studentName")
                NotificationHelper.showClassEndNotification(context, studentName)
                android.util.Log.d("ClassEndReceiver", "Notification shown successfully")
            } catch (e: Exception) {
                android.util.Log.e("ClassEndReceiver", "Error while processing class end notification", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        /** Intent extra key for passing the student's name to show in the notification. */
        const val EXTRA_STUDENT_NAME = "extra_student_name"
    }
}
