package com.devsusana.hometutorpro.presentation.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.devsusana.hometutorpro.domain.entities.ActiveSession
import com.devsusana.hometutorpro.domain.usecases.ICancelClassEndNotificationUseCase
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
    lateinit var getNextClassUseCase: IGetNextClassUseCase

    @Inject
    lateinit var getStudentByIdUseCase: IGetStudentByIdUseCase

    @Inject
    lateinit var saveStudentUseCase: ISaveStudentUseCase

    @Inject
    lateinit var startActiveSessionUseCase: IStartActiveSessionUseCase

    @Inject
    lateinit var stopActiveSessionUseCase: IStopActiveSessionUseCase

    @Inject
    lateinit var scheduleClassEndNotificationUseCase: IScheduleClassEndNotificationUseCase

    @Inject
    lateinit var cancelClassEndNotificationUseCase: ICancelClassEndNotificationUseCase

    private val job = SupervisorJob()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + job)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != ACTION_START_CLASS && action != ACTION_STOP_CLASS) return

        val pendingResult = goAsync()
        coroutineScope.launch {
            try {
                if (action == ACTION_START_CLASS) {
                    handleStartClass(context)
                } else {
                    handleStopClass(context)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling widget action: $action", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleStartClass(context: Context) {
        val user = getCurrentUserUseCase().filterNotNull().firstOrNull() ?: return
        val nextOccurrence = getNextClassUseCase() ?: return
        val studentId = nextOccurrence.student.id
        val student = getStudentByIdUseCase(user.uid, studentId).firstOrNull() ?: return

        // Calculate class duration safely
        val durationMinutes = try {
            val start = ClassTimerWidget.parseTimeSafely(nextOccurrence.startTime)
            val end = ClassTimerWidget.parseTimeSafely(nextOccurrence.endTime)
            if (start != null && end != null) {
                java.time.Duration.between(start, end).toMinutes().coerceAtLeast(15)
            } else 60L
        } catch (_: Exception) {
            60L
        }

        // Charge the student pending balance
        val priceToAdd = (durationMinutes / 60.0) * student.pricePerHour
        val updatedStudent = student.copy(
            pendingBalance = student.pendingBalance + priceToAdd,
            lastClassDate = System.currentTimeMillis()
        )
        saveStudentUseCase(user.uid, updatedStudent)

        // Save active session
        val session = ActiveSession(
            studentId = student.id,
            studentName = student.name,
            startTimeMillis = System.currentTimeMillis(),
            durationMinutes = durationMinutes,
            isOngoing = true,
            tutorId = user.uid
        )
        startActiveSessionUseCase(session)

        // Schedule notification for class end (with rollback if scheduling fails)
        val scheduled = scheduleClassEndNotificationUseCase(student.name, durationMinutes)
        if (!scheduled) {
            stopActiveSessionUseCase()
        }

        ClassTimerWidget.updateWidget(context)
    }

    private suspend fun handleStopClass(context: Context) {
        stopActiveSessionUseCase()
        cancelClassEndNotificationUseCase()
        ClassTimerWidget.updateWidget(context)
    }

    companion object {
        private const val TAG = "ClassTimerActionReceiver"
        const val ACTION_START_CLASS = ClassTimerWidget.ACTION_START_CLASS
        const val ACTION_STOP_CLASS = ClassTimerWidget.ACTION_STOP_CLASS
    }
}
