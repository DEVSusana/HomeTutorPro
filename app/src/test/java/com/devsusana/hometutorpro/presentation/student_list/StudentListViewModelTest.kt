package com.devsusana.hometutorpro.presentation.student_list

import com.devsusana.hometutorpro.domain.entities.Student
import com.devsusana.hometutorpro.domain.entities.StudentSummary
import com.devsusana.hometutorpro.domain.entities.User
import com.devsusana.hometutorpro.domain.usecases.IGetCurrentUserUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetStudentsUseCase
import com.devsusana.hometutorpro.domain.usecases.ILogoutUseCase
import com.devsusana.hometutorpro.domain.usecases.IToggleStudentActiveUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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

@OptIn(ExperimentalCoroutinesApi::class)
class StudentListViewModelTest {

    private lateinit var getStudentsUseCase: IGetStudentsUseCase
    private lateinit var logoutUseCase: ILogoutUseCase
    private lateinit var toggleStudentActiveUseCase: IToggleStudentActiveUseCase
    private lateinit var getCurrentUserUseCase: IGetCurrentUserUseCase
    private lateinit var viewModel: StudentListViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getStudentsUseCase = mockk()
        logoutUseCase = mockk(relaxed = true)
        toggleStudentActiveUseCase = mockk()
        getCurrentUserUseCase = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init should load students when user is logged in`() = runTest {
        // Given
        val userId = "user123"
        val user = User(uid = userId, email = "test@test.com", displayName = "Test User")
        val students = listOf(
            StudentSummary(id = "1", name = "Student 1", subjects = "Math", color = null, pendingBalance = 0.0, pricePerHour = 20.0, isActive = true, lastClassDate = null),
            StudentSummary(id = "2", name = "Student 2", subjects = "Physics", color = null, pendingBalance = 0.0, pricePerHour = 20.0, isActive = true, lastClassDate = null)
        )

        every { getStudentsUseCase(userId) } returns flowOf(students)
        every { getCurrentUserUseCase() } returns MutableStateFlow(user)

        // When
        viewModel = StudentListViewModel(getStudentsUseCase, logoutUseCase, toggleStudentActiveUseCase, getCurrentUserUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(students, viewModel.state.value.students)
        verify { getStudentsUseCase(userId) }
    }

    @Test
    fun `init should not load students when user is null`() = runTest {
        // Given
        every { getCurrentUserUseCase() } returns MutableStateFlow(null)

        // When
        viewModel = StudentListViewModel(getStudentsUseCase, logoutUseCase, toggleStudentActiveUseCase, getCurrentUserUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(emptyList<StudentSummary>(), viewModel.state.value.students)
        verify(exactly = 0) { getStudentsUseCase(any()) }
    }

    @Test
    fun `logout should call logout use case`() = runTest {
        // Given
        every { getCurrentUserUseCase() } returns MutableStateFlow(null)
        viewModel = StudentListViewModel(getStudentsUseCase, logoutUseCase, toggleStudentActiveUseCase, getCurrentUserUseCase)

        // When
        viewModel.logout()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { logoutUseCase() }
    }

    @Test
    fun `onRequestToggleActive should update state with student`() = runTest {
        // Given
        val student = StudentSummary(id = "1", name = "Test", subjects = "", color = null, pendingBalance = 0.0, pricePerHour = 0.0, isActive = true, lastClassDate = null)
        every { getCurrentUserUseCase() } returns MutableStateFlow(null)
        viewModel = StudentListViewModel(getStudentsUseCase, logoutUseCase, toggleStudentActiveUseCase, getCurrentUserUseCase)

        // When
        viewModel.onRequestToggleActive(student)

        // Then
        assertEquals(student, viewModel.state.value.confirmToggleStudent)
    }

    @Test
    fun `onDismissToggleDialog should clear state`() = runTest {
        // Given
        val student = StudentSummary(id = "1", name = "Test", subjects = "", color = null, pendingBalance = 0.0, pricePerHour = 0.0, isActive = true, lastClassDate = null)
        every { getCurrentUserUseCase() } returns MutableStateFlow(null)
        viewModel = StudentListViewModel(getStudentsUseCase, logoutUseCase, toggleStudentActiveUseCase, getCurrentUserUseCase)
        viewModel.onRequestToggleActive(student)

        // When
        viewModel.onDismissToggleDialog()

        // Then
        assertEquals(null, viewModel.state.value.confirmToggleStudent)
    }

    @Test
    fun `onConfirmToggleActive should call use case and clear state`() = runTest {
        // Given
        val userId = "user123"
        val user = User(uid = userId, email = "test@test.com", displayName = "Test")
        val student = StudentSummary(id = "1", name = "Test", subjects = "", color = null, pendingBalance = 0.0, pricePerHour = 0.0, isActive = true, lastClassDate = null)
        
        every { getCurrentUserUseCase() } returns MutableStateFlow(user)
        every { getStudentsUseCase(userId) } returns flowOf(emptyList())
        coEvery { toggleStudentActiveUseCase(userId, "1") } returns com.devsusana.hometutorpro.domain.core.Result.Success(Unit)
        
        viewModel = StudentListViewModel(getStudentsUseCase, logoutUseCase, toggleStudentActiveUseCase, getCurrentUserUseCase)
        viewModel.onRequestToggleActive(student)

        // When
        viewModel.onConfirmToggleActive()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { toggleStudentActiveUseCase(userId, "1") }
        assertEquals(null, viewModel.state.value.confirmToggleStudent)
    }
}
