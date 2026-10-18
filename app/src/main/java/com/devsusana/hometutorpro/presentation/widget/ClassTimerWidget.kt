package com.devsusana.hometutorpro.presentation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.text.format.DateFormat
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.devsusana.hometutorpro.MainActivity
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.ActiveSession
import com.devsusana.hometutorpro.domain.entities.CalendarOccurrence
import com.devsusana.hometutorpro.domain.usecases.ICancelClassEndNotificationUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetActiveSessionUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetCurrentUserUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetNextClassUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetStudentByIdUseCase
import com.devsusana.hometutorpro.domain.usecases.ISaveStudentUseCase
import com.devsusana.hometutorpro.domain.usecases.IScheduleClassEndNotificationUseCase
import com.devsusana.hometutorpro.domain.usecases.IStartActiveSessionUseCase
import com.devsusana.hometutorpro.domain.usecases.IStopActiveSessionUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/**
 * AppWidgetProvider for the Class Timer Home Screen Widget.
 *
 * Allows tutors to track ongoing classes with a live countdown chronometer
 * and view or start their next scheduled class directly from the home screen.
 */
@AndroidEntryPoint
class ClassTimerWidget : AppWidgetProvider() {

    @Inject
    lateinit var getActiveSessionUseCase: IGetActiveSessionUseCase

    @Inject
    lateinit var getNextClassUseCase: IGetNextClassUseCase

    @Inject
    lateinit var getCurrentUserUseCase: IGetCurrentUserUseCase

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
                val currentUser = getCurrentUserUseCase().filterNotNull().firstOrNull()
                val activeSession = getActiveSessionUseCase()?.takeIf { session ->
                    currentUser != null && (session.tutorId.isEmpty() || session.tutorId == currentUser.uid)
                }
                val nextClass = if (activeSession == null && currentUser != null) getNextClassUseCase() else null

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_class_timer)
                    setupWidgetUi(context, views, activeSession, nextClass)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating widget: ", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
    }

    private fun setupWidgetUi(
        context: Context,
        views: RemoteViews,
        activeSession: ActiveSession?,
        nextClass: CalendarOccurrence?
    ) {
        // Tap on widget header/background opens the main app
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingMainIntent = PendingIntent.getActivity(
            context,
            RQ_MAIN,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_brand_title, pendingMainIntent)

        if (activeSession != null) {
            // ----------------------------------------------------
            // Active Session State
            // ----------------------------------------------------
            views.setViewVisibility(R.id.layout_no_active, View.GONE)
            views.setViewVisibility(R.id.layout_active, View.VISIBLE)
            views.setTextViewText(R.id.widget_status_badge, context.getString(R.string.widget_active_class_header))

            views.setTextViewText(
                R.id.widget_active_student_text,
                context.getString(R.string.widget_ongoing_class, activeSession.studentName)
            )

            // Formatted start time: Clean hh:mm a or HH:mm, no millis
            val startLocalTime = Instant.ofEpochMilli(activeSession.startTimeMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
            val formattedStart = formatLocalTime(context, startLocalTime)
            views.setTextViewText(
                R.id.widget_active_time_info,
                context.getString(R.string.widget_started_at, formattedStart, activeSession.durationMinutes)
            )

            // Setup native countdown chronometer
            val endTimeMillis = activeSession.startTimeMillis + (activeSession.durationMinutes * 60 * 1000)
            val remainingTimeMillis = endTimeMillis - System.currentTimeMillis()
            val baseElapsedRealtime = SystemClock.elapsedRealtime() + remainingTimeMillis

            views.setChronometerCountDown(R.id.widget_chronometer, true)
            views.setChronometer(R.id.widget_chronometer, baseElapsedRealtime, null, true)

            if (remainingTimeMillis <= 0) {
                views.setTextViewText(R.id.widget_chronometer_label, context.getString(R.string.widget_time_completed))
            } else {
                views.setTextViewText(R.id.widget_chronometer_label, context.getString(R.string.widget_time_remaining))
            }

            // Stop button
            val stopIntent = Intent(context, ClassTimerActionReceiver::class.java).apply {
                action = ACTION_STOP_CLASS
                `package` = context.packageName
            }
            val pendingStopIntent = PendingIntent.getBroadcast(
                context,
                RQ_STOP,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_stop_class, pendingStopIntent)

        } else {
            // ----------------------------------------------------
            // Idle State: Next Class or No Classes
            // ----------------------------------------------------
            views.setViewVisibility(R.id.layout_active, View.GONE)
            views.setViewVisibility(R.id.layout_no_active, View.VISIBLE)
            views.setTextViewText(R.id.widget_status_badge, context.getString(R.string.widget_next_class_header))

            if (nextClass != null) {
                views.setTextViewText(
                    R.id.widget_next_student_text,
                    context.getString(R.string.widget_next_student, nextClass.student.name)
                )

                // Cleanly formatted date & time: e.g. "Today • 04:00 PM - 05:00 PM"
                val dateLabel = formatScheduleDate(context, nextClass.date)
                val cleanStartTime = formatTimeDisplay(context, nextClass.startTime)
                val cleanEndTime = formatTimeDisplay(context, nextClass.endTime)
                views.setTextViewText(
                    R.id.widget_no_active_subtitle,
                    "$dateLabel • $cleanStartTime - $cleanEndTime"
                )

                views.setViewVisibility(R.id.btn_start_class, View.VISIBLE)

                // Start button
                val startIntent = Intent(context, ClassTimerActionReceiver::class.java).apply {
                    action = ACTION_START_CLASS
                    `package` = context.packageName
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
        private const val TAG = "ClassTimerWidget"

        const val ACTION_START_CLASS = "com.devsusana.hometutorpro.ACTION_START_CLASS"
        const val ACTION_STOP_CLASS = "com.devsusana.hometutorpro.ACTION_STOP_CLASS"

        private const val RQ_MAIN = 200
        private const val RQ_START = 201
        private const val RQ_STOP = 202

        /**
         * Triggers an update for all active ClassTimerWidget instances.
         */
        fun updateWidget(context: Context) {
            try {
                val intent = Intent(context, ClassTimerWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    `package` = context.packageName
                }
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val ids = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, ClassTimerWidget::class.java)
                )
                if (ids.isNotEmpty()) {
                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                    context.sendBroadcast(intent)
                }
            } catch (_: Throwable) {
                // Ignored in unit testing environments where Android framework / Intent is not mocked
            }
        }

        /**
         * Parses a time string (e.g. "16:00", "04:00 PM", "16:00:00") into a [LocalTime],
         * strictly stripping any accidental milliseconds or extra characters.
         */
        fun parseTimeSafely(timeStr: String): LocalTime? {
            val sanitized = timeStr.trim().substringBefore('.')
            return try {
                LocalTime.parse(sanitized, DateTimeFormatter.ofPattern("H:m"))
            } catch (e: Exception) {
                try {
                    LocalTime.parse(sanitized, DateTimeFormatter.ofPattern("H:m:s"))
                } catch (e2: Exception) {
                    try {
                        LocalTime.parse(sanitized, DateTimeFormatter.ofPattern("h:m a", Locale.US))
                    } catch (e3: Exception) {
                        try {
                            LocalTime.parse(sanitized, DateTimeFormatter.ofPattern("hh:mm a", Locale.US))
                        } catch (e4: Exception) {
                            null
                        }
                    }
                }
            }
        }

        /**
         * Formats a [LocalTime] honoring the user's 12-hour (AM/PM) vs 24-hour preference,
         * ensuring strictly clean format without seconds or milliseconds.
         */
        fun formatLocalTime(context: Context, time: LocalTime): String {
            val is24Hour = DateFormat.is24HourFormat(context)
            val pattern = if (is24Hour) "HH:mm" else "hh:mm a"
            return time.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
        }

        /**
         * Formats a raw time string cleanly into either 12-hour AM/PM or 24-hour format.
         */
        fun formatTimeDisplay(context: Context, rawTime: String): String {
            val parsed = parseTimeSafely(rawTime)
            return if (parsed != null) {
                formatLocalTime(context, parsed)
            } else {
                rawTime.substringBefore('.').trim()
            }
        }

        /**
         * Formats the schedule date cleanly as "Today", "Tomorrow", or "E, d MMM".
         */
        fun formatScheduleDate(context: Context, date: LocalDate): String {
            val today = LocalDate.now()
            return when (date) {
                today -> context.getString(R.string.widget_today)
                today.plusDays(1) -> context.getString(R.string.widget_tomorrow)
                else -> {
                    val pattern = "E, d MMM"
                    date.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
                }
            }
        }
    }
}
