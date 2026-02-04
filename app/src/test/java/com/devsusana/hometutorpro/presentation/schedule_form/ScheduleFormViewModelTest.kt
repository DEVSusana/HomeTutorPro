package com.devsusana.hometutorpro.presentation.schedule_form

import androidx.lifecycle.SavedStateHandle
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.Schedule
import com.devsusana.hometutorpro.domain.entities.User
import com.devsusana.hometutorpro.domain.usecases.IGetCurrentUserUseCase
import com.devsusana.hometutorpro.domain.usecases.ISaveScheduleUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleFormViewModelTest {

    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var saveScheduleUseCase: ISaveScheduleUseCase
    private lateinit var getCurrentUserUseCase: IGetCurrentUserUseCase
    private lateinit var viewModel: ScheduleFormViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        savedStateHandle = mockk(relaxed = true)
        saveScheduleUseCase = mockk()
        getCurrentUserUseCase = mockk<IGetCurrentUserUseCase>()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onScheduleChange should update state`() = runTest {
        // Given
        val schedule = Schedule(id = "1", studentId = "student1", dayOfWeek = DayOfWeek.MONDAY)
        val userStateFlow = MutableStateFlow<User?>(null)
        every { savedStateHandle.get<String>("studentId") } returns "student1"
        every { getCurrentUserUseCase() } returns userStateFlow
        
        viewModel = ScheduleFormViewModel(savedStateHandle, saveScheduleUseCase, getCurrentUserUseCase)

        // When
        viewModel.onScheduleChange(schedule)

        // Then
        assertEquals(schedule, viewModel.state.value.schedule)
    }

    @Test
    fun `saveSchedule should call use case and update state on success`() = runTest {
        // Given
        val userId = "user123"
        val studentId = "student1"
        val user = User(uid = userId, email = "test@test.com", displayName = "Test User")
        val schedule = Schedule(id = "1", studentId = studentId, dayOfWeek = DayOfWeek.MONDAY)

        every { savedStateHandle.get<String>("studentId") } returns studentId
        
        val userStateFlow = MutableStateFlow<User?>(user)
        every { getCurrentUserUseCase() } returns userStateFlow
        
        coEvery { saveScheduleUseCase(userId, studentId, any()) } returns Result.Success(Unit)

        viewModel = ScheduleFormViewModel(savedStateHandle, saveScheduleUseCase, getCurrentUserUseCase)
        viewModel.onScheduleChange(schedule)

        // When
        viewModel.saveSchedule()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { saveScheduleUseCase(userId, studentId, schedule) }
        assertTrue(viewModel.state.value.isSaved)
    }
}
