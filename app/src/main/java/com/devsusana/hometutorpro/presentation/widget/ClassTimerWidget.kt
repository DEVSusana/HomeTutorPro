package com.devsusana.hometutorpro.presentation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.text.format.DateFormat
import android.view.View
import android.widget.RemoteViews
import com.devsusana.hometutorpro.MainActivity
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.core.utils.SafeLogger
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.ActiveSession
import com.devsusana.hometutorpro.domain.entities.CalendarOccurrence
import com.devsusana.hometutorpro.domain.entities.ExceptionType
import com.devsusana.hometutorpro.domain.entities.TodayClassesSummary
import com.devsusana.hometutorpro.domain.usecases.ICancelClassEndNotificationUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetActiveSessionUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetCurrentUserUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetNextClassUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetStudentByIdUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetTodayClassesSummaryUseCase
import com.devsusana.hometutorpro.domain.usecases.ISaveStudentUseCase
import com.devsusana.hometutorpro.domain.usecases.IScheduleClassEndNotificationUseCase
import com.devsusana.hometutorpro.domain.usecases.IStartActiveSessionUseCase
import com.devsusana.hometutorpro.domain.usecases.IStopActiveSessionUseCase
import com.devsusana.hometutorpro.domain.utils.DoorCodeExtractor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
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

    @Inject
    lateinit var getStudentByIdUseCase: IGetStudentByIdUseCase

    @Inject
    lateinit var getTodayClassesSummaryUseCase: IGetTodayClassesSummaryUseCase

    private val job = SupervisorJob()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + job)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        // Cancel any pending/in-flight update job to prevent stale out-of-order state flickers
        activeUpdateJob?.cancel()
        val updateJob = coroutineScope.launch {
            try {
                updateMutex.withLock {
                    ensureActive()
                    // Synchronous read of active session from repository
                    val activeSession = getActiveSessionUseCase()

                    // If a class is actively running, render it immediately in a single atomic pass
                    // using the dedicated active XML layout (R.layout.widget_class_timer_active).
                    if (activeSession != null) {
                        val hasTutor = getCurrentUserUseCase().value != null || activeSession.tutorId.isNotBlank()
                        val nextClass = if (hasTutor) getNextClassUseCase() else null
                        val daySummary = if (hasTutor) getTodayClassesSummaryUseCase() else null
                        ensureActive()

                        val snapshot = WidgetRenderSnapshot(
                            isActive = true,
                            activeStudentId = activeSession.studentId,
                            activeStartTimeMillis = activeSession.startTimeMillis,
                            nextStudentId = nextClass?.student?.id,
                            nextStartTime = nextClass?.startTime,
                            nextDate = nextClass?.date?.toString(),
                            doorCode = null,
                            studentAddress = null,
                            completedToday = daySummary?.completedClassesToday ?: 0,
                            totalToday = daySummary?.totalClassesToday ?: 0
                        )

                        for (appWidgetId in appWidgetIds) {
                            if (lastRenderedSnapshots[appWidgetId] == snapshot) {
                                SafeLogger.d(TAG, "Skipping duplicate ACTIVE widget render for id=$appWidgetId")
                                continue
                            }
                            val views = RemoteViews(context.packageName, R.layout.widget_class_timer_active)
                            setupWidgetUi(context, views, activeSession, nextClass, null, daySummary, null)
                            appWidgetManager.updateAppWidget(appWidgetId, views)
                            lastRenderedSnapshots[appWidgetId] = snapshot
                        }
                        return@withLock
                    }

                    // Idle state: fetch upcoming class and door code using R.layout.widget_class_timer
                    val currentUser = getCurrentUserUseCase().value
                        ?: kotlinx.coroutines.withTimeoutOrNull(1000L) { getCurrentUserUseCase().filterNotNull().firstOrNull() }
                    val nextClass = if (currentUser != null) getNextClassUseCase() else null
                    val daySummary = if (currentUser != null) getTodayClassesSummaryUseCase() else TodayClassesSummary()
                    val student = if (currentUser != null && nextClass != null) {
                        getStudentByIdUseCase(currentUser.uid, nextClass.student.id).firstOrNull()
                    } else null
                    val doorCode = DoorCodeExtractor.extractDoorCode(student?.address, student?.notes)
                    val studentAddress = student?.address?.trim()?.takeIf { it.isNotEmpty() }
                    ensureActive()

                    val snapshot = WidgetRenderSnapshot(
                        isActive = false,
                        activeStudentId = null,
                        activeStartTimeMillis = 0L,
                        nextStudentId = nextClass?.student?.id,
                        nextStartTime = nextClass?.startTime,
                        nextDate = nextClass?.date?.toString(),
                        doorCode = doorCode,
                        studentAddress = studentAddress,
                        completedToday = daySummary.completedClassesToday,
                        totalToday = daySummary.totalClassesToday
                    )

                    for (appWidgetId in appWidgetIds) {
                        if (lastRenderedSnapshots[appWidgetId] == snapshot) {
                            SafeLogger.d(TAG, "Skipping duplicate IDLE widget render for id=$appWidgetId")
                            continue
                        }
                        val views = RemoteViews(context.packageName, R.layout.widget_class_timer)
                        setupWidgetUi(context, views, null, nextClass, doorCode, daySummary, studentAddress, currentUser?.uid)
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                        lastRenderedSnapshots[appWidgetId] = snapshot
                    }
                }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    SafeLogger.e(TAG, "Error updating widget", e)
                }
            } finally {
                pendingResult.finish()
            }
        }
        activeUpdateJob = updateJob
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        for (id in appWidgetIds) {
            lastRenderedSnapshots.remove(id)
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        lastRenderedSnapshots.clear()
        job.cancel()
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
    }

    private fun setupWidgetUi(
        context: Context,
        views: RemoteViews,
        activeSession: ActiveSession?,
        nextClass: CalendarOccurrence?,
        doorCode: String? = null,
        daySummary: TodayClassesSummary? = null,
        studentAddress: String? = null,
        currentTutorId: String? = null
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

        // Proposal 3: Day progress badge in header (e.g. "2/4 hoy" or "4/4 completadas")
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

        if (activeSession != null) {
            // ----------------------------------------------------
            // Active Session State (R.layout.widget_class_timer_active)
            // ----------------------------------------------------
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

            // Next class preview while active class is running
            if (nextClass != null) {
                val isSameActive = nextClass.student.id == activeSession.studentId &&
                    nextClass.date == LocalDate.now() &&
                    try {
                        val classStart = parseTimeSafely(nextClass.startTime)
                        val sessionEnd = Instant.ofEpochMilli(activeSession.startTimeMillis)
                            .atZone(ZoneId.systemDefault()).toLocalTime()
                            .plusMinutes(activeSession.durationMinutes)
                        classStart != null && classStart.isBefore(sessionEnd)
                    } catch (_: Exception) { false }

                if (!isSameActive) {
                    val dateLabel = formatScheduleDate(context, nextClass.date)
                    val cleanStartTime = formatTimeDisplay(context, nextClass.startTime)
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
            views.setOnClickPendingIntent(R.id.widget_chronometer, pendingStopIntent)
            views.setOnClickPendingIntent(R.id.layout_chronometer_container, pendingStopIntent)

        } else {
            // ----------------------------------------------------
            // Idle State: Next Class or No Classes (R.layout.widget_class_timer)
            // ----------------------------------------------------
            views.setTextViewText(R.id.widget_status_badge, context.getString(R.string.widget_next_class_header))

            if (nextClass != null) {
                // Proposal 2: Extra or Rescheduled Badge
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
                val dateLabel = formatScheduleDate(context, nextClass.date)
                val cleanStartTime = formatTimeDisplay(context, nextClass.startTime)
                val cleanEndTime = formatTimeDisplay(context, nextClass.endTime)
                views.setTextViewText(
                    R.id.widget_no_active_subtitle,
                    "$dateLabel • $cleanStartTime - $cleanEndTime"
                )

                // Proposal 4: Door code & Direct GPS Navigation Button
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
                            RQ_NAVIGATE,
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

                views.setViewVisibility(R.id.btn_start_class, View.VISIBLE)

                // Calculate duration in minutes safely
                val durationMinutes = try {
                    val start = parseTimeSafely(nextClass.startTime)
                    val end = parseTimeSafely(nextClass.endTime)
                    if (start != null && end != null) {
                        Duration.between(start, end).toMinutes().coerceAtLeast(15)
                    } else 60L
                } catch (_: Exception) {
                    60L
                }

                // Start button
                val startIntent = Intent(context, ClassTimerActionReceiver::class.java).apply {
                    action = ACTION_START_CLASS
                    `package` = context.packageName
                    putExtra(EXTRA_STUDENT_ID, nextClass.student.id)
                    putExtra(EXTRA_STUDENT_NAME, nextClass.student.name)
                    putExtra(EXTRA_DURATION_MINUTES, durationMinutes)
                    if (!currentTutorId.isNullOrBlank()) {
                        putExtra(EXTRA_TUTOR_ID, currentTutorId)
                    }
                }
                val pendingStartIntent = PendingIntent.getBroadcast(
                    context,
                    RQ_START,
                    startIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.btn_start_class, pendingStartIntent)
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

    private data class WidgetRenderSnapshot(
        val isActive: Boolean,
        val activeStudentId: String?,
        val activeStartTimeMillis: Long,
        val nextStudentId: String?,
        val nextStartTime: String?,
        val nextDate: String?,
        val doorCode: String?,
        val studentAddress: String? = null,
        val completedToday: Int,
        val totalToday: Int
    )

    companion object {
        private const val TAG = "ClassTimerWidget"
        private var activeUpdateJob: kotlinx.coroutines.Job? = null
        private val updateMutex = Mutex()
        private val lastRenderedSnapshots = ConcurrentHashMap<Int, WidgetRenderSnapshot>()

        const val ACTION_START_CLASS = "com.devsusana.hometutorpro.ACTION_START_CLASS"
        const val ACTION_STOP_CLASS = "com.devsusana.hometutorpro.ACTION_STOP_CLASS"

        const val EXTRA_STUDENT_ID = "com.devsusana.hometutorpro.EXTRA_STUDENT_ID"
        const val EXTRA_STUDENT_NAME = "com.devsusana.hometutorpro.EXTRA_STUDENT_NAME"
        const val EXTRA_DURATION_MINUTES = "com.devsusana.hometutorpro.EXTRA_DURATION_MINUTES"
        const val EXTRA_TUTOR_ID = "com.devsusana.hometutorpro.EXTRA_TUTOR_ID"

        private const val RQ_MAIN = 200
        private const val RQ_START = 201
        private const val RQ_STOP = 202
        private const val RQ_NAVIGATE = 203

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
