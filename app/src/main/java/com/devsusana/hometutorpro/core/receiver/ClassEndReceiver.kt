package com.devsusana.hometutorpro.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.devsusana.hometutorpro.core.utils.NotificationHelper
import com.devsusana.hometutorpro.domain.usecases.IStopActiveSessionUseCase
import com.devsusana.hometutorpro.presentation.widget.ClassTimerWidget
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * BroadcastReceiver to handle scheduled class end notifications.
 */
@AndroidEntryPoint
class ClassEndReceiver : BroadcastReceiver() {

    @Inject
    lateinit var stopActiveSessionUseCase: IStopActiveSessionUseCase

    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.d("ClassEndReceiver", "onReceive called - alarm triggered!")
        
        // Clear active session and update widget
        try {
            stopActiveSessionUseCase()
            ClassTimerWidget.updateWidget(context)
        } catch (e: Exception) {
            android.util.Log.e("ClassEndReceiver", "Error stopping session on receive", e)
        }
        
        // Check if notifications are enabled in settings
        val settingsManager = EntryPointAccessors.fromApplication(
            context.applicationContext,
            com.devsusana.hometutorpro.di.SettingsManagerEntryPoint::class.java
        ).settingsManager()
        
        val areNotificationsEnabled = runBlocking { settingsManager.classEndNotificationsFlow.first() }
        
        if (!areNotificationsEnabled) {
            android.util.Log.d("ClassEndReceiver", "Notifications are disabled in settings, skipping.")
            return
        }

        val studentName = intent.getStringExtra(EXTRA_STUDENT_NAME) ?: context.getString(com.devsusana.hometutorpro.R.string.student_default_name)
        android.util.Log.d("ClassEndReceiver", "Showing notification for student: $studentName")
        NotificationHelper.showClassEndNotification(context, studentName)
        android.util.Log.d("ClassEndReceiver", "Notification shown successfully")
    }

    companion object {
        const val EXTRA_STUDENT_NAME = "extra_student_name"
    }
}
