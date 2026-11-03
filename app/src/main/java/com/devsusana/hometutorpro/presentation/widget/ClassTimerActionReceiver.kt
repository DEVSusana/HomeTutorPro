package com.devsusana.hometutorpro.presentation.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.devsusana.hometutorpro.core.utils.SafeLogger
import com.devsusana.hometutorpro.domain.constants.SessionConstants
import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.ActiveSession
import com.devsusana.hometutorpro.domain.repository.IConfigRepository
import com.devsusana.hometutorpro.domain.usecases.IAddToBalanceUseCase
import com.devsusana.hometutorpro.domain.usecases.ICancelClassEndNotificationUseCase
import com.devsusana.hometutorpro.domain.usecases.IConsolidateActiveSessionUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetActiveSessionUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetCurrentUserUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetStudentByIdUseCase
import com.devsusana.hometutorpro.domain.usecases.ISaveStudentUseCase
import com.devsusana.hometutorpro.domain.usecases.IScheduleClassEndNotificationUseCase
import com.devsusana.hometutorpro.domain.usecases.IStartActiveSessionUseCase
import com.devsusana.hometutorpro.domain.usecases.IStopActiveSessionUseCase
import com.devsusana.hometutorpro.domain.usecases.IUpdateLastClassDateUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Non-exported [BroadcastReceiver] dedicated to handling widget user interactions
 * (Start Class and Finish Class) triggered from home screen [ClassTimerWidget] PendingIntents.
 *
 * Keeping this receiver non-exported (`android:exported="false"`) protects class start
 * (balance charging and timer scheduling) and stop operations from unauthorized explicit
 * broadcasts by third-party applications installed on the device.
 */
@AndroidEntryPoint
class ClassTimerActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var getCurrentUserUseCase: IGetCurrentUserUseCase

    @Inject
    lateinit var getStudentByIdUseCase: IGetStudentByIdUseCase

    @Inject
    lateinit var saveStudentUseCase: ISaveStudentUseCase

    @Inject
    lateinit var addToBalanceUseCase: IAddToBalanceUseCase

    @Inject
    lateinit var startActiveSessionUseCase: IStartActiveSessionUseCase

    @Inject
    lateinit var stopActiveSessionUseCase: IStopActiveSessionUseCase

    @Inject
    lateinit var consolidateActiveSessionUseCase: IConsolidateActiveSessionUseCase

    @Inject
    lateinit var getActiveSessionUseCase: IGetActiveSessionUseCase

    @Inject
    lateinit var scheduleClassEndNotificationUseCase: IScheduleClassEndNotificationUseCase

    @Inject
    lateinit var cancelClassEndNotificationUseCase: ICancelClassEndNotificationUseCase

    @Inject
    lateinit var configRepository: IConfigRepository

    @Inject
    lateinit var updateLastClassDateUseCase: IUpdateLastClassDateUseCase

    private val job = SupervisorJob()
    var coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main + job)
        @androidx.annotation.VisibleForTesting set

    /**
     * Receives widget broadcast intents for starting or stopping tutoring sessions.
     *
     * @param context The Context in which the receiver is running.
     * @param intent The Intent being received.
     */
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != ACTION_START_CLASS && action != ACTION_STOP_CLASS) return

        val isActionsEnabled = try {
            configRepository.isWidgetActionsEnabled()
        } catch (_: Exception) {
            true
        }
        if (!isActionsEnabled) {
            SafeLogger.w(TAG, "Widget action rejected: actions are disabled via remote config kill-switch ($action)")
            return
        }

        val pendingResult = goAsync()
        coroutineScope.launch {
            try {
                if (action == ACTION_START_CLASS) {
                    handleStartClass(context, intent)
                } else {
                    handleStopClass(context)
                }
            } catch (e: Exception) {
                SafeLogger.e(TAG, "Error handling widget action: $action", e)
            } finally {
                pendingResult?.finish()
            }
        }
    }

    /**
     * Handles starting a tutoring session from a widget action intent.
     *
     * @param context Application context for broadcasting widget updates.
     * @param intent Intent containing [ClassTimerWidget.EXTRA_STUDENT_ID] and duration payload.
     */
    @androidx.annotation.VisibleForTesting
    internal suspend fun handleStartClass(context: Context, intent: Intent) {
        val explicitTutorId = intent.getStringExtra(ClassTimerWidget.EXTRA_TUTOR_ID)
        val tutorId = if (!explicitTutorId.isNullOrBlank()) {
            explicitTutorId
        } else {
            (getCurrentUserUseCase().value
                ?: kotlinx.coroutines.withTimeoutOrNull(1000L) { getCurrentUserUseCase().filterNotNull().firstOrNull() }
            )?.uid
        } ?: return

        // Rule 6.A (BUSINESS_RULES.md): Start class actions must be strictly bound to explicit student ID
        val explicitStudentId = intent.getStringExtra(ClassTimerWidget.EXTRA_STUDENT_ID)
        if (explicitStudentId.isNullOrBlank()) {
            SafeLogger.w(TAG, "Cannot start class: Missing EXTRA_STUDENT_ID in intent payload")
            return
        }

        val student = getStudentByIdUseCase(tutorId, explicitStudentId).firstOrNull()
        if (student == null) {
            SafeLogger.w(TAG, "Cannot start class: Student $explicitStudentId not found")
            return
        }

        val durationMinutes = intent.getLongExtra(ClassTimerWidget.EXTRA_DURATION_MINUTES, 60L).takeIf { it > 0 } ?: 60L

        // 1. Save active session immediately with isBilled = false.
        // Financial billing is deferred until the session has run for at least MIN_BILLABLE_DURATION_MINUTES (15 min)
        // so that accidental starts or early cancellations do not corrupt student accounting or mark the class completed.
        val session = ActiveSession(
            studentId = student.id,
            studentName = student.name,
            startTimeMillis = System.currentTimeMillis(),
            durationMinutes = durationMinutes,
            isOngoing = true,
            tutorId = tutorId,
            isBilled = false
        )
        startActiveSessionUseCase(session)
        ClassTimerWidget.updateWidget(context)

        // 2. Schedule notification for class end (log warning if scheduling fails, but keep active session intact)
        val scheduled = scheduleClassEndNotificationUseCase(student.name, durationMinutes)
        if (!scheduled) {
            SafeLogger.w(TAG, "Exact alarm permission or notification scheduling failed; continuing active session without notification")
        }
    }

    /**
     * Handles stopping an active tutoring session from a widget action intent.
     *
     * @param context Application context for broadcasting widget updates.
     */
    @androidx.annotation.VisibleForTesting
    internal suspend fun handleStopClass(context: Context) {
        val activeSession = if (::getActiveSessionUseCase.isInitialized) {
            getActiveSessionUseCase()
        } else {
            null
        }
        val consolidationResult = if (::consolidateActiveSessionUseCase.isInitialized) {
            consolidateActiveSessionUseCase(activeSession)
        } else if (activeSession != null && activeSession.isOngoing && !activeSession.isBilled) {
            val elapsedMillis = System.currentTimeMillis() - activeSession.startTimeMillis
            if (elapsedMillis >= SessionConstants.MIN_BILLABLE_DURATION_MILLIS) {
                consolidateSessionCharge(activeSession)
            } else {
                Result.Success(false)
            }
        } else {
            Result.Success(false)
        }

        if (consolidationResult is Result.Error) {
            SafeLogger.e(TAG, "Consolidation failed: ${consolidationResult.error}, retaining session for recovery")
            return
        }

        stopActiveSessionUseCase()
        cancelClassEndNotificationUseCase()
        ClassTimerWidget.updateWidget(context)
    }

    private suspend fun consolidateSessionCharge(activeSession: ActiveSession): Result<Boolean, DomainError> {
        val tutorId = activeSession.tutorId.takeIf { it.isNotBlank() }
            ?: (getCurrentUserUseCase().value
                ?: kotlinx.coroutines.withTimeoutOrNull(1000L) { getCurrentUserUseCase().filterNotNull().firstOrNull() }
            )?.uid
            ?: return Result.Error(DomainError.UserNotFound)

        val student = getStudentByIdUseCase(tutorId, activeSession.studentId).firstOrNull()
            ?: return Result.Error(DomainError.StudentNotFound)
        val priceToAdd = (activeSession.durationMinutes / 60.0) * student.pricePerHour
        return if (priceToAdd > 0) {
            when (val res = addToBalanceUseCase(tutorId, student.id, priceToAdd)) {
                is Result.Success -> Result.Success(true)
                is Result.Error -> Result.Error(res.error)
            }
        } else {
            if (::updateLastClassDateUseCase.isInitialized) {
                when (val res = updateLastClassDateUseCase(tutorId, student.id, System.currentTimeMillis())) {
                    is Result.Success -> Result.Success(true)
                    is Result.Error -> Result.Error(res.error)
                }
            } else {
                when (val res = saveStudentUseCase(tutorId, student.copy(lastClassDate = System.currentTimeMillis()))) {
                    is Result.Success -> Result.Success(true)
                    is Result.Error -> Result.Error(res.error)
                }
            }
        }
    }

    companion object {
        private const val TAG = "ClassTimerActionReceiver"
        const val ACTION_START_CLASS = ClassTimerWidget.ACTION_START_CLASS
        const val ACTION_STOP_CLASS = ClassTimerWidget.ACTION_STOP_CLASS
    }
}
