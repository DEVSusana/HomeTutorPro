package com.devsusana.hometutorpro.presentation.widget

import android.content.Context
import android.content.Intent
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.ActiveSession
import com.devsusana.hometutorpro.domain.entities.Student
import com.devsusana.hometutorpro.domain.entities.User
import com.devsusana.hometutorpro.domain.usecases.IAddToBalanceUseCase
import com.devsusana.hometutorpro.domain.usecases.ICancelClassEndNotificationUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetActiveSessionUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetCurrentUserUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetStudentByIdUseCase
import com.devsusana.hometutorpro.domain.usecases.ISaveStudentUseCase
import com.devsusana.hometutorpro.domain.usecases.IScheduleClassEndNotificationUseCase
import com.devsusana.hometutorpro.domain.usecases.IStartActiveSessionUseCase
import com.devsusana.hometutorpro.domain.usecases.IStopActiveSessionUseCase
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Robolectric unit tests for [ClassTimerActionReceiver] validating Rule 6 of BUSINESS_RULES.md:
 * - Immutable student binding on start class actions.
 * - 15-minute consolidation grace period before charging student balance.
 * - Idempotency of stop class actions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [33])
class ClassTimerActionReceiverRobolectricTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private val getCurrentUserUseCase: IGetCurrentUserUseCase = mockk()
    private val getStudentByIdUseCase: IGetStudentByIdUseCase = mockk()
    private val saveStudentUseCase: ISaveStudentUseCase = mockk()
    private val addToBalanceUseCase: IAddToBalanceUseCase = mockk()
    private val startActiveSessionUseCase: IStartActiveSessionUseCase = mockk(relaxed = true)
    private val stopActiveSessionUseCase: IStopActiveSessionUseCase = mockk(relaxed = true)
    private val getActiveSessionUseCase: IGetActiveSessionUseCase = mockk()
    private val scheduleClassEndNotificationUseCase: IScheduleClassEndNotificationUseCase = mockk()
    private val cancelClassEndNotificationUseCase: ICancelClassEndNotificationUseCase = mockk(relaxed = true)
    private val updateLastClassDateUseCase: com.devsusana.hometutorpro.domain.usecases.IUpdateLastClassDateUseCase = mockk(relaxed = true)
    private val consolidateActiveSessionUseCase: com.devsusana.hometutorpro.domain.usecases.IConsolidateActiveSessionUseCase = mockk()

    private lateinit var receiver: ClassTimerActionReceiver
    private lateinit var context: Context

    private val testUser = User(uid = "user123", email = "tutor@test.com", displayName = "Tutor")
    private val testStudentPablo = Student(
        id = "student_pablo",
        professorId = "user123",
        name = "Pablo",
        pricePerHour = 15.0,
        pendingBalance = 0.0
    )

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = kotlinx.coroutines.test.TestScope(testDispatcher)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        hiltRule.inject()
        context = RuntimeEnvironment.getApplication()

        receiver = ClassTimerActionReceiver().apply {
            this.coroutineScope = testScope
            this.getCurrentUserUseCase = this@ClassTimerActionReceiverRobolectricTest.getCurrentUserUseCase
            this.getStudentByIdUseCase = this@ClassTimerActionReceiverRobolectricTest.getStudentByIdUseCase
            this.saveStudentUseCase = this@ClassTimerActionReceiverRobolectricTest.saveStudentUseCase
            this.addToBalanceUseCase = this@ClassTimerActionReceiverRobolectricTest.addToBalanceUseCase
            this.startActiveSessionUseCase = this@ClassTimerActionReceiverRobolectricTest.startActiveSessionUseCase
            this.stopActiveSessionUseCase = this@ClassTimerActionReceiverRobolectricTest.stopActiveSessionUseCase
            this.getActiveSessionUseCase = this@ClassTimerActionReceiverRobolectricTest.getActiveSessionUseCase
            this.scheduleClassEndNotificationUseCase = this@ClassTimerActionReceiverRobolectricTest.scheduleClassEndNotificationUseCase
            this.cancelClassEndNotificationUseCase = this@ClassTimerActionReceiverRobolectricTest.cancelClassEndNotificationUseCase
            this.updateLastClassDateUseCase = this@ClassTimerActionReceiverRobolectricTest.updateLastClassDateUseCase
        }

        every { getCurrentUserUseCase() } returns MutableStateFlow(testUser)
        every { getStudentByIdUseCase("user123", "student_pablo") } returns flowOf(testStudentPablo)
        every { getActiveSessionUseCase() } returns null
        coEvery { addToBalanceUseCase("user123", "student_pablo", any()) } returns Result.Success(Unit)
        coEvery { scheduleClassEndNotificationUseCase(any(), any()) } returns true
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `start class with explicit studentId starts session unbilled without immediate balance charge`() = runTest {
        val intent = Intent(ClassTimerActionReceiver.ACTION_START_CLASS).apply {
            putExtra(ClassTimerWidget.EXTRA_STUDENT_ID, "student_pablo")
            putExtra(ClassTimerWidget.EXTRA_DURATION_MINUTES, 60L)
        }

        receiver.handleStartClass(context, intent)

        coVerify {
            startActiveSessionUseCase(match {
                it.studentId == "student_pablo" && !it.isBilled && it.isOngoing
            })
        }
        coVerify(exactly = 0) {
            addToBalanceUseCase(any(), any(), any())
        }
    }

    @Test
    fun `start class with explicit tutorId binds tutorId on active session`() = runTest {
        val intent = Intent(ClassTimerActionReceiver.ACTION_START_CLASS).apply {
            putExtra(ClassTimerWidget.EXTRA_STUDENT_ID, "student_pablo")
            putExtra(ClassTimerWidget.EXTRA_DURATION_MINUTES, 60L)
            putExtra(ClassTimerWidget.EXTRA_TUTOR_ID, "user123")
        }

        receiver.handleStartClass(context, intent)

        coVerify {
            startActiveSessionUseCase(match {
                it.studentId == "student_pablo" && it.tutorId == "user123" && !it.isBilled
            })
        }
    }

    @Test
    fun `start class without explicit studentId aborts safely and does not start session`() = runTest {
        val intent = Intent(ClassTimerActionReceiver.ACTION_START_CLASS)
        // No EXTRA_STUDENT_ID provided

        receiver.handleStartClass(context, intent)

        coVerify(exactly = 0) {
            startActiveSessionUseCase(any())
            addToBalanceUseCase(any(), any(), any())
        }
    }

    @Test
    fun `stop class before 15 minutes does not charge balance and stops active session`() = runTest {
        val recentSession = ActiveSession(
            studentId = "student_pablo",
            studentName = "Pablo",
            startTimeMillis = System.currentTimeMillis() - (2 * 60 * 1000L), // 2 minutes ago (< 15 min)
            durationMinutes = 60L,
            isOngoing = true,
            tutorId = "user123",
            isBilled = false
        )
        every { getActiveSessionUseCase() } returns recentSession

        receiver.handleStopClass(context)

        coVerify(exactly = 0) {
            addToBalanceUseCase(any(), any(), any())
        }
        coVerify {
            stopActiveSessionUseCase()
            cancelClassEndNotificationUseCase()
        }
    }

    @Test
    fun `stop class after 15 minutes consolidates balance charge for exact student`() = runTest {
        val matureSession = ActiveSession(
            studentId = "student_pablo",
            studentName = "Pablo",
            startTimeMillis = System.currentTimeMillis() - (16 * 60 * 1000L), // 16 minutes ago (>= 15 min)
            durationMinutes = 60L,
            isOngoing = true,
            tutorId = "user123",
            isBilled = false
        )
        every { getActiveSessionUseCase() } returns matureSession

        receiver.handleStopClass(context)

        coVerify(exactly = 1) {
            addToBalanceUseCase("user123", "student_pablo", 15.0)
        }
        coVerify {
            stopActiveSessionUseCase()
            cancelClassEndNotificationUseCase()
        }
    }

    @Test
    fun `stop class for already billed session does not double charge`() = runTest {
        val alreadyBilledSession = ActiveSession(
            studentId = "student_pablo",
            studentName = "Pablo",
            startTimeMillis = System.currentTimeMillis() - (30 * 60 * 1000L),
            durationMinutes = 60L,
            isOngoing = true,
            tutorId = "user123",
            isBilled = true
        )
        every { getActiveSessionUseCase() } returns alreadyBilledSession

        receiver.handleStopClass(context)

        coVerify(exactly = 0) {
            addToBalanceUseCase(any(), any(), any())
        }
        coVerify {
            stopActiveSessionUseCase()
            cancelClassEndNotificationUseCase()
        }
    }

    @Test
    fun `start class keeps active session even if notification scheduling fails`() = runTest {
        coEvery { scheduleClassEndNotificationUseCase(any(), any()) } returns false

        val intent = Intent(ClassTimerActionReceiver.ACTION_START_CLASS).apply {
            putExtra(ClassTimerWidget.EXTRA_STUDENT_ID, "student_pablo")
            putExtra(ClassTimerWidget.EXTRA_DURATION_MINUTES, 60L)
        }

        receiver.handleStartClass(context, intent)

        coVerify {
            startActiveSessionUseCase(any())
        }
        coVerify(exactly = 0) {
            stopActiveSessionUseCase()
        }
    }

    @Test
    fun `stop class preserves session when consolidation returns Result Error`() = runTest {
        receiver.consolidateActiveSessionUseCase = consolidateActiveSessionUseCase
        val matureSession = ActiveSession(
            studentId = "student_pablo",
            studentName = "Pablo",
            startTimeMillis = System.currentTimeMillis() - (16 * 60 * 1000L),
            durationMinutes = 60L,
            isOngoing = true,
            tutorId = "user123",
            isBilled = false
        )
        every { getActiveSessionUseCase() } returns matureSession
        coEvery { consolidateActiveSessionUseCase(any()) } returns com.devsusana.hometutorpro.domain.core.Result.Error(com.devsusana.hometutorpro.domain.core.DomainError.NetworkError)

        receiver.handleStopClass(context)

        coVerify(exactly = 0) {
            stopActiveSessionUseCase()
            cancelClassEndNotificationUseCase()
        }
    }
}
