package com.devsusana.hometutorpro.presentation.weekly_schedule

import com.devsusana.hometutorpro.domain.entities.Schedule
import com.devsusana.hometutorpro.domain.entities.ScheduleException
import com.devsusana.hometutorpro.domain.entities.Student
import com.devsusana.hometutorpro.domain.entities.User
import com.devsusana.hometutorpro.domain.usecases.IDeleteScheduleExceptionUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetAllSchedulesUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetCurrentUserUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetScheduleExceptionsUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetStudentsUseCase
import com.devsusana.hometutorpro.domain.usecases.ISaveScheduleExceptionUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetStudentByIdUseCase
import com.devsusana.hometutorpro.domain.usecases.ISaveStudentUseCase
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek

@OptIn(ExperimentalCoroutinesApi::class)
class WeeklyScheduleViewModelTest {

    private lateinit var getStudentsUseCase: IGetStudentsUseCase
    private lateinit var getAllSchedulesUseCase: IGetAllSchedulesUseCase
    private lateinit var getCurrentUserUseCase: IGetCurrentUserUseCase
    private lateinit var getScheduleExceptionsUseCase: IGetScheduleExceptionsUseCase
    private lateinit var saveScheduleExceptionUseCase: ISaveScheduleExceptionUseCase
    private lateinit var deleteScheduleExceptionUseCase: IDeleteScheduleExceptionUseCase
    private lateinit var getStudentByIdUseCase: IGetStudentByIdUseCase
    private lateinit var saveStudentUseCase: ISaveStudentUseCase
    private lateinit var viewModel: WeeklyScheduleViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getStudentsUseCase = mockk()
        getAllSchedulesUseCase = mockk()
        getCurrentUserUseCase = mockk()
        getScheduleExceptionsUseCase = mockk()
        saveScheduleExceptionUseCase = mockk()
        deleteScheduleExceptionUseCase = mockk()
        getStudentByIdUseCase = mockk()
        saveStudentUseCase = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init should load weekly schedule`() = runTest {
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

        every { getCurrentUserUseCase() } returns MutableStateFlow<User?>(user)
        every { getStudentsUseCase(userId) } returns flowOf(listOf(student))
        every { getAllSchedulesUseCase(userId) } returns flowOf(listOf(schedule))
        every { getScheduleExceptionsUseCase(userId, student.id) } returns flowOf(emptyList())

        // When
        viewModel = WeeklyScheduleViewModel(
            getCurrentUserUseCase,
            getStudentsUseCase,
            getAllSchedulesUseCase,
            getStudentByIdUseCase,
            getScheduleExceptionsUseCase,
            saveScheduleExceptionUseCase,
            deleteScheduleExceptionUseCase,
            saveStudentUseCase
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val schedules = viewModel.state.value.schedulesByDay[DayOfWeek.MONDAY]
        println("Schedules for Monday: $schedules")
        assertEquals(1, schedules?.filterIsInstance<WeeklyScheduleItem.Regular>()?.size)
        val regularItem = schedules?.filterIsInstance<WeeklyScheduleItem.Regular>()?.firstOrNull()
        assertEquals(schedule.id, regularItem?.schedule?.id)
        assertFalse(viewModel.state.value.isLoading)
    }
}
