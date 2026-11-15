package com.devsusana.hometutorpro.presentation.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.domain.entities.ActiveSession
import com.devsusana.hometutorpro.domain.entities.CalendarOccurrence
import com.devsusana.hometutorpro.domain.entities.ExceptionType
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Encapsulates RemoteViews binding logic for active sessions and idle next classes in the Class Timer Widget.
 */
object WidgetViewBinder {

    /**
     * Binds the brand title click action and the day progress badge in the widget header.
     */
    fun bindHeader(
        context: Context,
        views: RemoteViews,
        daySummary: com.devsusana.hometutorpro.domain.entities.TodayClassesSummary?
    ) {
        val mainIntent = Intent(context, com.devsusana.hometutorpro.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingMainIntent = PendingIntent.getActivity(
            context,
            ClassTimerWidget.RQ_MAIN,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_brand_title, pendingMainIntent)

        if (daySummary != null && daySummary.totalClassesToday > 0) {
            views.setViewVisibility(R.id.widget_day_progress_badge, View.VISIBLE)
            val progressText = if (daySummary.isAllCompleted) {
                context.getString(
                    R.string.widget_day_progress_all_done,
                    daySummary.completedClassesToday,
                    daySummary.totalClassesToday
                )
            } else {
                context.getString(
                    R.string.widget_day_progress,
                    daySummary.currentClassIndex,
                    daySummary.totalClassesToday
                )
            }
            views.setTextViewText(R.id.widget_day_progress_badge, progressText)
        } else {
            views.setViewVisibility(R.id.widget_day_progress_badge, View.GONE)
        }
    }

    /**
     * Binds UI elements for an ongoing active class session.
     */
    fun bindActiveSession(
        context: Context,
        views: RemoteViews,
        activeSession: ActiveSession,
        nextClass: CalendarOccurrence?
    ) {
        views.setTextViewText(R.id.widget_status_badge, context.getString(R.string.widget_active_class_header))

        views.setTextViewText(
            R.id.widget_active_student_text,
            context.getString(R.string.widget_ongoing_class, activeSession.studentName)
        )

        // Formatted start time: Clean hh:mm a or HH:mm, no millis
        val startLocalTime = Instant.ofEpochMilli(activeSession.startTimeMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
        val formattedStart = WidgetTimeFormatter.formatLocalTime(context, startLocalTime)
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

        // Next class preview while active class is running
        if (nextClass != null) {
            val isSameActive = nextClass.student.id == activeSession.studentId &&
                nextClass.date == LocalDate.now() &&
                try {
                    val classStart = WidgetTimeFormatter.parseTimeSafely(nextClass.startTime)
                    val sessionEnd = Instant.ofEpochMilli(activeSession.startTimeMillis)
                        .atZone(ZoneId.systemDefault()).toLocalTime()
                        .plusMinutes(activeSession.durationMinutes)
                    classStart != null && classStart.isBefore(sessionEnd)
                } catch (_: Exception) { false }

            if (!isSameActive) {
                val dateLabel = WidgetTimeFormatter.formatScheduleDate(context, nextClass.date)
                val cleanStartTime = WidgetTimeFormatter.formatTimeDisplay(context, nextClass.startTime)
                views.setViewVisibility(R.id.widget_active_next_class_preview, View.VISIBLE)
                views.setTextViewText(
                    R.id.widget_active_next_class_preview,
                    context.getString(R.string.widget_upcoming_preview, nextClass.student.name, "$dateLabel • $cleanStartTime")
                )
            } else {
                views.setViewVisibility(R.id.widget_active_next_class_preview, View.GONE)
            }
        } else {
            views.setViewVisibility(R.id.widget_active_next_class_preview, View.GONE)
        }

        // Stop button (also bind to chronometer for instant stop on tap)
        val stopIntent = Intent(context, ClassTimerActionReceiver::class.java).apply {
            action = ClassTimerWidget.ACTION_STOP_CLASS
            `package` = context.packageName
        }
        val pendingStopIntent = PendingIntent.getBroadcast(
            context,
            ClassTimerWidget.RQ_STOP,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_stop_class, pendingStopIntent)
        views.setOnClickPendingIntent(R.id.widget_chronometer, pendingStopIntent)
        views.setOnClickPendingIntent(R.id.layout_chronometer_container, pendingStopIntent)
    }

    /**
     * Binds UI elements for the idle state (upcoming next class or empty schedule).
     */
    fun bindIdleNextClass(
        context: Context,
        views: RemoteViews,
        nextClass: CalendarOccurrence?,
        doorCode: String?,
        studentAddress: String?,
        currentTutorId: String?,
        isActionsEnabled: Boolean
    ) {
        views.setTextViewText(R.id.widget_status_badge, context.getString(R.string.widget_next_class_header))

        if (nextClass != null) {
            val isExtra = nextClass.isExtra || nextClass.exception?.type == ExceptionType.EXTRA
            val isRescheduled = nextClass.exception?.type == ExceptionType.RESCHEDULED

            when {
                isExtra -> {
                    views.setViewVisibility(R.id.widget_class_tag_badge, View.VISIBLE)
                    views.setTextViewText(R.id.widget_class_tag_badge, context.getString(R.string.widget_badge_extra))
                    views.setInt(R.id.widget_class_tag_badge, "setBackgroundResource", R.drawable.widget_badge_extra_bg)
                    views.setTextColor(R.id.widget_class_tag_badge, android.graphics.Color.parseColor("#F59E0B"))
                }
                isRescheduled -> {
                    views.setViewVisibility(R.id.widget_class_tag_badge, View.VISIBLE)
                    views.setTextViewText(R.id.widget_class_tag_badge, context.getString(R.string.widget_badge_rescheduled))
                    views.setInt(R.id.widget_class_tag_badge, "setBackgroundResource", R.drawable.widget_badge_rescheduled_bg)
                    views.setTextColor(R.id.widget_class_tag_badge, android.graphics.Color.parseColor("#60A5FA"))
                }
                else -> {
                    views.setViewVisibility(R.id.widget_class_tag_badge, View.GONE)
                }
            }

            views.setTextViewText(
                R.id.widget_next_student_text,
                context.getString(R.string.widget_next_student, nextClass.student.name)
            )

            // Cleanly formatted date & time: e.g. "Today • 04:00 PM - 05:00 PM"
            val dateLabel = WidgetTimeFormatter.formatScheduleDate(context, nextClass.date)
            val cleanStartTime = WidgetTimeFormatter.formatTimeDisplay(context, nextClass.startTime)
            val cleanEndTime = WidgetTimeFormatter.formatTimeDisplay(context, nextClass.endTime)
            views.setTextViewText(
                R.id.widget_no_active_subtitle,
                "$dateLabel • $cleanStartTime - $cleanEndTime"
            )

            // Door code & Direct GPS Navigation Button
            val hasDoorCode = !doorCode.isNullOrBlank()
            val hasAddress = !studentAddress.isNullOrBlank()

            if (hasDoorCode || hasAddress) {
                views.setViewVisibility(R.id.layout_aux_info, View.VISIBLE)

                if (hasDoorCode) {
                    views.setViewVisibility(R.id.widget_door_code_text, View.VISIBLE)
                    views.setTextViewText(
                        R.id.widget_door_code_text,
                        context.getString(R.string.widget_door_code, doorCode)
                    )
                } else {
                    views.setViewVisibility(R.id.widget_door_code_text, View.GONE)
                }

                if (hasAddress) {
                    views.setViewVisibility(R.id.btn_widget_navigate, View.VISIBLE)
                    val navUri = Uri.parse("geo:0,0?q=" + Uri.encode(studentAddress))
                    val navIntent = Intent(Intent.ACTION_VIEW, navUri).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    val pendingNavIntent = PendingIntent.getActivity(
                        context,
                        ClassTimerWidget.RQ_NAVIGATE,
                        navIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.btn_widget_navigate, pendingNavIntent)
                } else {
                    views.setViewVisibility(R.id.btn_widget_navigate, View.GONE)
                }
            } else {
                views.setViewVisibility(R.id.layout_aux_info, View.GONE)
                views.setViewVisibility(R.id.widget_door_code_text, View.GONE)
                views.setViewVisibility(R.id.btn_widget_navigate, View.GONE)
            }

            if (isActionsEnabled) {
                views.setViewVisibility(R.id.btn_start_class, View.VISIBLE)

                // Calculate duration in minutes safely
                val durationMinutes = try {
                    val start = WidgetTimeFormatter.parseTimeSafely(nextClass.startTime)
                    val end = WidgetTimeFormatter.parseTimeSafely(nextClass.endTime)
                    if (start != null && end != null) {
                        Duration.between(start, end).toMinutes().coerceAtLeast(15)
                    } else 60L
                } catch (_: Exception) {
                    60L
                }

                // Start button
                val startIntent = Intent(context, ClassTimerActionReceiver::class.java).apply {
                    action = ClassTimerWidget.ACTION_START_CLASS
                    `package` = context.packageName
                    putExtra(ClassTimerWidget.EXTRA_STUDENT_ID, nextClass.student.id)
                    putExtra(ClassTimerWidget.EXTRA_STUDENT_NAME, nextClass.student.name)
                    putExtra(ClassTimerWidget.EXTRA_DURATION_MINUTES, durationMinutes)
                    if (!currentTutorId.isNullOrBlank()) {
                        putExtra(ClassTimerWidget.EXTRA_TUTOR_ID, currentTutorId)
                    }
                }
                val pendingStartIntent = PendingIntent.getBroadcast(
                    context,
                    ClassTimerWidget.RQ_START,
                    startIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.btn_start_class, pendingStartIntent)
            } else {
                // Actions disabled remotely via kill-switch: hide start button, tapping card opens main app
                views.setViewVisibility(R.id.btn_start_class, View.GONE)
            }
        } else {
            views.setViewVisibility(R.id.widget_class_tag_badge, View.GONE)
            views.setViewVisibility(R.id.layout_aux_info, View.GONE)
            views.setViewVisibility(R.id.widget_door_code_text, View.GONE)
            views.setViewVisibility(R.id.btn_widget_navigate, View.GONE)
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
