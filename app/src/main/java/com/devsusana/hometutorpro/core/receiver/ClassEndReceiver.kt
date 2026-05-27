package com.devsusana.hometutorpro.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.devsusana.hometutorpro.core.utils.NotificationHelper
import com.devsusana.hometutorpro.di.ApplicationScope
import com.devsusana.hometutorpro.domain.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * BroadcastReceiver to handle scheduled class end notifications.
 * Uses [goAsync] and Hilt injection to check settings and show notifications
 * off the main execution thread.
 */
@AndroidEntryPoint
class ClassEndReceiver : BroadcastReceiver() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.d("ClassEndReceiver", "onReceive called - alarm triggered!")
        
        val pendingResult = goAsync()
        
        applicationScope.launch {
            try {
                // Check if notifications are enabled in settings
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
