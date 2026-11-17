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
import com.devsusana.hometutorpro.domain.repository.IConfigRepository
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

    @Inject
    lateinit var configRepository: IConfigRepository

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

                        val isActionsEnabled = try {
                            configRepository.isWidgetActionsEnabled()
                        } catch (_: Exception) {
                            true
                        }

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
                            totalToday = daySummary?.totalClassesToday ?: 0,
                            isActionsEnabled = isActionsEnabled
                        )

                        for (appWidgetId in appWidgetIds) {
                            if (lastRenderedSnapshots[appWidgetId] == snapshot) {
                                SafeLogger.d(TAG, "Skipping duplicate ACTIVE widget render for id=$appWidgetId")
                                continue
                            }
                            val views = RemoteViews(context.packageName, R.layout.widget_class_timer_active)
                            setupWidgetUi(context, views, activeSession, nextClass, null, daySummary, null, null, isActionsEnabled)
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

                    val isActionsEnabled = try {
                        configRepository.isWidgetActionsEnabled()
                    } catch (_: Exception) {
                        true
                    }

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
                        totalToday = daySummary.totalClassesToday,
                        isActionsEnabled = isActionsEnabled
                    )

                    for (appWidgetId in appWidgetIds) {
                        if (lastRenderedSnapshots[appWidgetId] == snapshot) {
                            SafeLogger.d(TAG, "Skipping duplicate IDLE widget render for id=$appWidgetId")
                            continue
                        }
                        val views = RemoteViews(context.packageName, R.layout.widget_class_timer)
                        setupWidgetUi(context, views, null, nextClass, doorCode, daySummary, studentAddress, currentUser?.uid, isActionsEnabled)
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

    @androidx.annotation.VisibleForTesting
    internal fun setupWidgetUi(
        context: Context,
        views: RemoteViews,
        activeSession: ActiveSession?,
        nextClass: CalendarOccurrence?,
        doorCode: String? = null,
        daySummary: TodayClassesSummary? = null,
        studentAddress: String? = null,
        currentTutorId: String? = null,
        isActionsEnabled: Boolean = try {
            configRepository.isWidgetActionsEnabled()
        } catch (_: Exception) {
            true
        }
    ) {
        WidgetViewBinder.bindHeader(context, views, daySummary)

        if (activeSession != null) {
            WidgetViewBinder.bindActiveSession(
                context = context,
                views = views,
                activeSession = activeSession,
                nextClass = nextClass
            )
        } else {
            WidgetViewBinder.bindIdleNextClass(
                context = context,
                views = views,
                nextClass = nextClass,
                doorCode = doorCode,
                studentAddress = studentAddress,
                currentTutorId = currentTutorId,
                isActionsEnabled = isActionsEnabled
            )
        }
    }

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

        internal const val RQ_MAIN = 200
        internal const val RQ_START = 201
        internal const val RQ_STOP = 202
        internal const val RQ_NAVIGATE = 203

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

        fun parseTimeSafely(timeStr: String): LocalTime? = WidgetTimeFormatter.parseTimeSafely(timeStr)

        fun formatLocalTime(context: Context, time: LocalTime): String = WidgetTimeFormatter.formatLocalTime(context, time)

        fun formatTimeDisplay(context: Context, rawTime: String): String = WidgetTimeFormatter.formatTimeDisplay(context, rawTime)

        fun formatScheduleDate(context: Context, date: LocalDate): String = WidgetTimeFormatter.formatScheduleDate(context, date)
    }
}
