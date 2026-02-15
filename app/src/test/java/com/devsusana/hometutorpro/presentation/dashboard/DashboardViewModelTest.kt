package com.devsusana.hometutorpro.presentation.dashboard

import com.devsusana.hometutorpro.domain.entities.User
import com.devsusana.hometutorpro.domain.entities.Student
import com.devsusana.hometutorpro.domain.entities.Schedule
import com.devsusana.hometutorpro.domain.entities.ScheduleException
import com.devsusana.hometutorpro.domain.usecases.*
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private lateinit var getCurrentUserUseCase: IGetCurrentUserUseCase
    private lateinit var getStudentsUseCase: IGetStudentsUseCase
    private lateinit var getAllSchedulesUseCase: IGetAllSchedulesUseCase
    private lateinit var getScheduleExceptionsUseCase: IGetScheduleExceptionsUseCase
    private lateinit var saveScheduleExceptionUseCase: ISaveScheduleExceptionUseCase
    private lateinit var deleteScheduleExceptionUseCase: IDeleteScheduleExceptionUseCase
    private lateinit var getStudentByIdUseCase: IGetStudentByIdUseCase
    private lateinit var saveStudentUseCase: ISaveStudentUseCase
    
    private lateinit var viewModel: DashboardViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        getCurrentUserUseCase = mockk()
        getStudentsUseCase = mockk()
        getAllSchedulesUseCase = mockk()
        getScheduleExceptionsUseCase = mockk()
        saveScheduleExceptionUseCase = mockk()
        deleteScheduleExceptionUseCase = mockk()
        getStudentByIdUseCase = mockk()
        saveStudentUseCase = mockk()

        val user = User(uid = "user123", email = "test@test.com", displayName = "Test User")
        every { getCurrentUserUseCase() } returns MutableStateFlow(user)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadDashboardData should correctly count only today's pending classes`() = runTest {
        // Given
        val today = LocalDate.now()
        val dayOfWeek = today.dayOfWeek
        
        val student = Student(id = "student1", name = "Student 1", professorId = "user123", isActive = true)
        val students = listOf(student)
        
        // One schedule for today that is in the future
        val timeNow = LocalTime.now()
        val futureTime = timeNow.plusHours(1).toString().substring(0, 5) // HH:mm
        val scheduleToday = Schedule(id = "sch1", studentId = "student1", dayOfWeek = dayOfWeek, startTime = futureTime, endTime = "23:59")
        
        // One schedule for the same day of week but NEXT week (should be excluded by the new logic)
        // Actually, the generator creates occurrences for this week and next week.
        // The fix ensures we only count those where occurrence.date == today.
        
        every { getStudentsUseCase("user123") } returns flowOf(students)
        every { getAllSchedulesUseCase("user123") } returns flowOf(listOf(scheduleToday))
        every { getScheduleExceptionsUseCase("user123", "student1") } returns flowOf(emptyList())

        // When
        viewModel = DashboardViewModel(
            getCurrentUserUseCase,
            getStudentsUseCase,
            getAllSchedulesUseCase,
            getScheduleExceptionsUseCase,
            saveScheduleExceptionUseCase,
            deleteScheduleExceptionUseCase,
            getStudentByIdUseCase,
            saveStudentUseCase,
            mockk(relaxed = true)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        // There should be 2 occurrences in total (this week and next week), but only 1 is for TODAY
        assertEquals(1, viewModel.state.value.todayPendingClassesCount)
    }

    @Test
    fun `loadDashboardData should exclude already passed classes today`() = runTest {
        // Given
        val today = LocalDate.now()
        val dayOfWeek = today.dayOfWeek
        
        val student = Student(id = "student1", name = "Student 1", professorId = "user123", isActive = true)
        
        // One schedule for today that is in the PAST
        val timeNow = LocalTime.now()
        if (timeNow.isAfter(LocalTime.of(0, 30))) {
            val pastTime = timeNow.minusMinutes(30).toString().substring(0, 5)
            val schedulePast = Schedule(id = "sch1", studentId = "student1", dayOfWeek = dayOfWeek, startTime = pastTime, endTime = "23:59")
            
            every { getStudentsUseCase("user123") } returns flowOf(listOf(student))
            every { getAllSchedulesUseCase("user123") } returns flowOf(listOf(schedulePast))
            every { getScheduleExceptionsUseCase("user123", "student1") } returns flowOf(emptyList())

            // When
            viewModel = DashboardViewModel(
                getCurrentUserUseCase,
                getStudentsUseCase,
                getAllSchedulesUseCase,
                getScheduleExceptionsUseCase,
                saveScheduleExceptionUseCase,
                deleteScheduleExceptionUseCase,
                getStudentByIdUseCase,
                saveStudentUseCase,
                mockk(relaxed = true)
            )
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            assertEquals(0, viewModel.state.value.todayPendingClassesCount)
        }
    }
}
