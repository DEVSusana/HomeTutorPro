package com.devsusana.hometutorpro.core.receiver

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.core.utils.SafeLogger
import com.devsusana.hometutorpro.di.ClassEndReceiverEntryPoint
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.presentation.widget.ClassTimerWidget
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

/**
 * Thin BroadcastReceiver gateway for scheduled class-end alarms.
 *
 * Delegates all business logic (settings check, notification display, session consolidation)
 * to domain use cases via [ClassEndReceiverEntryPoint].
 */
class ClassEndReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ClassEndReceiver"

        /** Intent extra key for passing the student's name to show in the notification. */
        const val EXTRA_STUDENT_NAME = "extra_student_name"
    }

    /**
     * Handles the incoming broadcast intent for the class-end alarm.
     *
     * Consolidates session billing via domain use case and triggers end notification.
     *
     * @param context The Context in which the receiver is running.
     * @param intent The Intent being received.
     */
    override fun onReceive(context: Context, intent: Intent) {
        val studentName = intent.getStringExtra(EXTRA_STUDENT_NAME)
            ?: context.getString(R.string.student_default_name)

        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            ClassEndReceiverEntryPoint::class.java
        )

        val consolidateActiveSessionUseCase = entryPoint.consolidateActiveSessionUseCase()
        val notifyClassEndUseCase = entryPoint.notifyClassEndUseCase()
        val stopActiveSessionUseCase = entryPoint.stopActiveSessionUseCase()
        val applicationScope = entryPoint.applicationScope()

        val pendingResult = goAsync()
        applicationScope.launch {
            try {
                val consolidationResult = consolidateActiveSessionUseCase()
                if (consolidationResult !is Result.Error) {
                    stopActiveSessionUseCase()
                } else {
                    SafeLogger.e(TAG, "Consolidation failed: ${consolidationResult.error}, retaining session for recovery")
                }

                // Trigger standard app widget update broadcast with explicit widget IDs
                ClassTimerWidget.updateWidget(context)

                notifyClassEndUseCase.execute(studentName)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error processing class end notification", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
