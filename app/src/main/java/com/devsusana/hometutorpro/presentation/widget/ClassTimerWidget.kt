package com.devsusana.hometutorpro.presentation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.domain.entities.ActiveSession
import com.devsusana.hometutorpro.domain.usecases.IGetActiveSessionUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetNextClassUseCase
import com.devsusana.hometutorpro.domain.usecases.IStartActiveSessionUseCase
import com.devsusana.hometutorpro.domain.usecases.IStopActiveSessionUseCase
import com.devsusana.hometutorpro.domain.usecases.ISaveStudentUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetStudentByIdUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetCurrentUserUseCase
import com.devsusana.hometutorpro.domain.usecases.IScheduleClassEndNotificationUseCase
import com.devsusana.hometutorpro.domain.core.Result
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalTime
import javax.inject.Inject

@AndroidEntryPoint
class ClassTimerWidget : AppWidgetProvider() {

    @Inject
    lateinit var getActiveSessionUseCase: IGetActiveSessionUseCase

    @Inject
    lateinit var getNextClassUseCase: IGetNextClassUseCase

    @Inject
    lateinit var startActiveSessionUseCase: IStartActiveSessionUseCase

    @Inject
    lateinit var stopActiveSessionUseCase: IStopActiveSessionUseCase

    @Inject
    lateinit var saveStudentUseCase: ISaveStudentUseCase

    @Inject
    lateinit var getStudentByIdUseCase: IGetStudentByIdUseCase

    @Inject
    lateinit var getCurrentUserUseCase: IGetCurrentUserUseCase

    @Inject
    lateinit var scheduleClassEndNotificationUseCase: IScheduleClassEndNotificationUseCase

    private val job = SupervisorJob()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + job)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        coroutineScope.launch {
            try {
                val activeSession = getActiveSessionUseCase()
                val nextClass = if (activeSession == null) getNextClassUseCase() else null

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_class_timer)
                    setupWidgetUi(context, views, activeSession, nextClass)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                android.util.Log.e("ClassTimerWidget", "Error updating widget: ", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action == ACTION_START_CLASS || action == ACTION_STOP_CLASS) {
            val pendingResult = goAsync()
            coroutineScope.launch {
                try {
                    if (action == ACTION_START_CLASS) {
                        handleStartClass(context)
                    } else {
                        handleStopClass(context)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ClassTimerWidget", "Error handling widget action: $action", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private suspend fun handleStartClass(context: Context) {
        val user = getCurrentUserUseCase().filterNotNull().firstOrNull() ?: return
        val nextOccurrence = getNextClassUseCase() ?: return
        val studentId = nextOccurrence.student.id
        val student = getStudentByIdUseCase(user.uid, studentId).firstOrNull() ?: return

        // Calculate class duration
        val start = LocalTime.parse(nextOccurrence.startTime)
        val end = LocalTime.parse(nextOccurrence.endTime)
        val durationMinutes = Duration.between(start, end).toMinutes()
        val finalDuration = if (durationMinutes > 0) durationMinutes else 60L

        // Update student balance
        val priceToAdd = (finalDuration / 60.0) * student.pricePerHour
        val updatedStudent = student.copy(pendingBalance = student.pendingBalance + priceToAdd)
        
        val saveResult = saveStudentUseCase(user.uid, updatedStudent)
        if (saveResult is Result.Success<*>) {
            // Save active session
            val session = ActiveSession(
                studentId = student.id,
                studentName = student.name,
                startTimeMillis = System.currentTimeMillis(),
                durationMinutes = finalDuration,
                isOngoing = true
            )
            startActiveSessionUseCase(session)

            // Schedule notification and alarm (which will trigger ClassEndReceiver)
            scheduleClassEndNotificationUseCase(student.name, finalDuration)

            // Force widget UI refresh
            updateWidget(context)
        }
    }

    private fun handleStopClass(context: Context) {
        stopActiveSessionUseCase()
        updateWidget(context)
    }

    private fun setupWidgetUi(
        context: Context,
        views: RemoteViews,
        activeSession: ActiveSession?,
        nextClass: com.devsusana.hometutorpro.domain.entities.CalendarOccurrence?
    ) {
        if (activeSession != null) {
            // Active session state
            views.setViewVisibility(R.id.layout_no_active, View.GONE)
            views.setViewVisibility(R.id.layout_active, View.VISIBLE)

            views.setTextViewText(
                R.id.widget_active_student_text,
                context.getString(R.string.widget_ongoing_class, activeSession.studentName)
            )

            // Setup native chronometer countdown
            val endTime = activeSession.startTimeMillis + activeSession.durationMinutes * 60 * 1000
            views.setChronometerCountDown(R.id.widget_chronometer, true)
            views.setChronometer(R.id.widget_chronometer, endTime, null, true)

            // Stop button pending intent
            val stopIntent = Intent(context, ClassTimerWidget::class.java).apply {
                action = ACTION_STOP_CLASS
            }
            val pendingStopIntent = PendingIntent.getBroadcast(
                context,
                RQ_STOP,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_stop_class, pendingStopIntent)

        } else {
            // No active session state
            views.setViewVisibility(R.id.layout_active, View.GONE)
            views.setViewVisibility(R.id.layout_no_active, View.VISIBLE)

            if (nextClass != null) {
                views.setTextViewText(
                    R.id.widget_next_student_text,
                    context.getString(R.string.widget_next_student, nextClass.student.name)
                )
                views.setTextViewText(
                    R.id.widget_no_active_subtitle,
                    "${nextClass.startTime} - ${nextClass.endTime}"
                )
                views.setViewVisibility(R.id.btn_start_class, View.VISIBLE)

                // Start button pending intent
                val startIntent = Intent(context, ClassTimerWidget::class.java).apply {
                    action = ACTION_START_CLASS
                }
                val pendingStartIntent = PendingIntent.getBroadcast(
                    context,
                    RQ_START,
                    startIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.btn_start_class, pendingStartIntent)
            } else {
                views.setTextViewText(
                    R.id.widget_next_student_text,
                    context.getString(R.string.widget_no_active_class)
                )
                views.setTextViewText(
                    R.id.widget_no_active_subtitle,
                    context.getString(R.string.widget_no_upcoming_class)
                )
                views.setViewVisibility(R.id.btn_start_class, View.GONE)
            }
        }
    }

    companion object {
        const val ACTION_START_CLASS = "com.devsusana.hometutorpro.ACTION_START_CLASS"
        const val ACTION_STOP_CLASS = "com.devsusana.hometutorpro.ACTION_STOP_CLASS"
        
        private const val RQ_START = 201
        private const val RQ_STOP = 202

        fun updateWidget(context: Context) {
            val intent = Intent(context, ClassTimerWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(
                ComponentName(context, ClassTimerWidget::class.java)
            )
            if (ids.isNotEmpty()) {
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                context.sendBroadcast(intent)
            }
        }
    }
}
