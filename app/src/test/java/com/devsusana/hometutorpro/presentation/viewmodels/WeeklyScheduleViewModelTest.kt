package com.devsusana.hometutorpro.presentation.viewmodels

import com.devsusana.hometutorpro.domain.entities.Schedule
import com.devsusana.hometutorpro.domain.entities.Student
import com.devsusana.hometutorpro.domain.entities.StudentSummary
import com.devsusana.hometutorpro.domain.entities.User
import com.devsusana.hometutorpro.domain.usecases.IDeleteScheduleExceptionUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetAllSchedulesUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetCurrentUserUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetAllScheduleExceptionsUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetStudentsUseCase
import com.devsusana.hometutorpro.domain.usecases.ISaveScheduleExceptionUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetStudentByIdUseCase
import com.devsusana.hometutorpro.domain.usecases.IAddToBalanceUseCase
import com.devsusana.hometutorpro.domain.usecases.IGenerateCalendarOccurrencesUseCase
import com.devsusana.hometutorpro.domain.usecases.implementations.CleanupDuplicatesUseCase
import com.devsusana.hometutorpro.presentation.weekly_schedule.WeeklyScheduleViewModel
import com.devsusana.hometutorpro.presentation.weekly_schedule.WeeklyScheduleItem
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Unit tests for [WeeklyScheduleViewModel] verifying correct load of schedule items and state changes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WeeklyScheduleViewModelTest {

    private lateinit var getStudentsUseCase: IGetStudentsUseCase
    private lateinit var getAllSchedulesUseCase: IGetAllSchedulesUseCase
    private lateinit var getCurrentUserUseCase: IGetCurrentUserUseCase
    private lateinit var getAllScheduleExceptionsUseCase: IGetAllScheduleExceptionsUseCase
    private lateinit var saveScheduleExceptionUseCase: ISaveScheduleExceptionUseCase
    private lateinit var deleteScheduleExceptionUseCase: IDeleteScheduleExceptionUseCase
    private lateinit var getStudentByIdUseCase: IGetStudentByIdUseCase
    private lateinit var addToBalanceUseCase: IAddToBalanceUseCase
    private lateinit var generateCalendarOccurrencesUseCase: IGenerateCalendarOccurrencesUseCase
    private lateinit var cleanupDuplicatesUseCase: CleanupDuplicatesUseCase
    private lateinit var application: android.app.Application
    private lateinit var viewModel: WeeklyScheduleViewModel

    private val testDispatcher = StandardTestDispatcher()

    /** Configures the test dispatcher and mocks before each test runs. */
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getStudentsUseCase = mockk()
        getAllSchedulesUseCase = mockk()
        getCurrentUserUseCase = mockk()
        getAllScheduleExceptionsUseCase = mockk()
        saveScheduleExceptionUseCase = mockk()
        deleteScheduleExceptionUseCase = mockk()
        getStudentByIdUseCase = mockk()
        addToBalanceUseCase = mockk()
        generateCalendarOccurrencesUseCase = mockk(relaxed = true)
        cleanupDuplicatesUseCase = mockk(relaxed = true)
        application = mockk(relaxed = true)
    }

    /** Resets the main dispatcher after each test completes. */
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Verifies that the ViewModel loads the weekly schedules correctly from the use cases on initialization. */
    @Test
    fun init_shouldLoadWeeklySchedule() = runTest {
        // Given
        val userId = "user123"
        val user = User(uid = userId, email = "test@test.com", displayName = "Test User")
        val student = Student(id = "student1", name = "Student 1", professorId = userId)
        val schedule = Schedule(
            id = "schedule1",
            studentId = student.id,
            dayOfWeek = DayOfWeek.MONDAY,
            startTime = "10:00",
            endTime = "11:00"
        )

        val studentSummary = StudentSummary(
            id = student.id,
            name = student.name,
            subjects = student.subjects,
            color = student.color,
            pendingBalance = student.pendingBalance,
            pricePerHour = student.pricePerHour,
            isActive = student.isActive,
            lastClassDate = null
        )

        every { getCurrentUserUseCase() } returns MutableStateFlow<User?>(user)
        every { getStudentsUseCase(userId) } returns flowOf(listOf(studentSummary))
        every { getAllSchedulesUseCase(userId) } returns flowOf(listOf(schedule))
        every { getAllScheduleExceptionsUseCase(userId) } returns flowOf(emptyList())
        coEvery { generateCalendarOccurrencesUseCase(any(), any(), any(), any(), any()) } returns listOf(
            com.devsusana.hometutorpro.domain.entities.CalendarOccurrence(
                schedule = schedule,
                student = studentSummary,
                date = LocalDate.now().with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            )
        )

        // When
        val scheduleClassEndNotificationUseCase: com.devsusana.hometutorpro.domain.usecases.IScheduleClassEndNotificationUseCase = mockk(relaxed = true)
        viewModel = WeeklyScheduleViewModel(
            getCurrentUserUseCase,
            getStudentsUseCase,
            getAllSchedulesUseCase,
            getStudentByIdUseCase,
            getAllScheduleExceptionsUseCase,
            saveScheduleExceptionUseCase,
            deleteScheduleExceptionUseCase,
            addToBalanceUseCase,
            generateCalendarOccurrencesUseCase,
            cleanupDuplicatesUseCase,
            scheduleClassEndNotificationUseCase,
            application
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val schedules = viewModel.state.value.schedulesByDay[DayOfWeek.MONDAY]
        assertEquals(1, schedules?.filterIsInstance<WeeklyScheduleItem.Regular>()?.size)
        val regularItem = schedules?.filterIsInstance<WeeklyScheduleItem.Regular>()?.firstOrNull()
        assertEquals(schedule.id, regularItem?.schedule?.id)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `startClass invokes startActiveSessionUseCase and schedules notification`() = runTest {
        val user = User(uid = "user123", email = "test@test.com", displayName = "Test User")
        every { getCurrentUserUseCase() } returns MutableStateFlow<User?>(user)
        every { getStudentsUseCase("user123") } returns flowOf(emptyList())
        every { getAllSchedulesUseCase("user123") } returns flowOf(emptyList())
        every { getAllScheduleExceptionsUseCase("user123") } returns flowOf(emptyList())

        val student = Student(id = "student1", name = "Student 1", professorId = "user123", pricePerHour = 20.0, isActive = true)
        coEvery { getStudentByIdUseCase("user123", "student1") } returns flowOf(student)
        coEvery { addToBalanceUseCase("user123", "student1", any()) } returns com.devsusana.hometutorpro.domain.core.Result.Success(Unit)

        val scheduleNotification: com.devsusana.hometutorpro.domain.usecases.IScheduleClassEndNotificationUseCase = mockk(relaxed = true) {
            every { this@mockk.invoke(any(), any()) } returns true
        }
        val startActiveSession: com.devsusana.hometutorpro.domain.usecases.IStartActiveSessionUseCase = mockk(relaxed = true)

        viewModel = WeeklyScheduleViewModel(
            getCurrentUserUseCase,
            getStudentsUseCase,
            getAllSchedulesUseCase,
            getStudentByIdUseCase,
            getAllScheduleExceptionsUseCase,
            saveScheduleExceptionUseCase,
            deleteScheduleExceptionUseCase,
            addToBalanceUseCase,
            generateCalendarOccurrencesUseCase,
            cleanupDuplicatesUseCase,
            scheduleNotification,
            application,
            startActiveSessionUseCase = startActiveSession
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.startClass("student1", 60)
        testDispatcher.scheduler.advanceUntilIdle()

        verify(exactly = 1) { startActiveSession(match { it.studentId == "student1" && it.tutorId == "user123" && it.durationMinutes == 60L }) }
        verify(exactly = 1) { scheduleNotification("Student 1", 60L) }
    }
}
