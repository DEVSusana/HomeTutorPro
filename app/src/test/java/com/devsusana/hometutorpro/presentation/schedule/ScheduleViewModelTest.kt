package com.devsusana.hometutorpro.presentation.schedule

import androidx.lifecycle.SavedStateHandle
import com.devsusana.hometutorpro.domain.entities.Schedule
import com.devsusana.hometutorpro.domain.entities.User
import com.devsusana.hometutorpro.domain.usecases.IDeleteScheduleUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetCurrentUserUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetSchedulesUseCase
import com.devsusana.hometutorpro.domain.usecases.ISaveScheduleUseCase
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
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModelTest {

    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var getSchedulesUseCase: IGetSchedulesUseCase
    private lateinit var saveScheduleUseCase: ISaveScheduleUseCase
    private lateinit var deleteScheduleUseCase: IDeleteScheduleUseCase
    private lateinit var getCurrentUserUseCase: IGetCurrentUserUseCase
    private lateinit var viewModel: ScheduleViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        savedStateHandle = mockk(relaxed = true)
        getSchedulesUseCase = mockk()
        saveScheduleUseCase = mockk()
        deleteScheduleUseCase = mockk()
        getCurrentUserUseCase = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init should load schedules when user is logged in`() = runTest {
        // Given
        val userId = "user123"
        val studentId = "student1"
        val user = User(uid = userId, email = "test@test.com", displayName = "Test User")
        val schedule = Schedule(id = "1", studentId = studentId, dayOfWeek = DayOfWeek.MONDAY)

        every { savedStateHandle.get<String>("studentId") } returns studentId
        every { getCurrentUserUseCase() } returns MutableStateFlow<User?>(user)
        every { getSchedulesUseCase(userId, studentId) } returns flowOf(listOf(schedule))

        // When
        viewModel = ScheduleViewModel(
            savedStateHandle,
            getSchedulesUseCase,
            saveScheduleUseCase,
            deleteScheduleUseCase,
            getCurrentUserUseCase
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(1, viewModel.state.value.schedules.size)
        assertEquals(schedule, viewModel.state.value.schedules.first())
    }
}
