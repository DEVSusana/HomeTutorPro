package com.devsusana.hometutorpro.presentation.student_list

import com.devsusana.hometutorpro.domain.entities.Student
import com.devsusana.hometutorpro.domain.entities.User
import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.devsusana.hometutorpro.domain.usecases.IGetStudentsUseCase
import com.devsusana.hometutorpro.domain.usecases.ILogoutUseCase
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
    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: StudentListViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getStudentsUseCase = mockk()
        logoutUseCase = mockk(relaxed = true)
        authRepository = mockk()
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
            Student(id = "1", name = "Student 1", professorId = userId),
            Student(id = "2", name = "Student 2", professorId = userId)
        )

        every { authRepository.currentUser } returns MutableStateFlow(user)
        every { getStudentsUseCase(userId) } returns flowOf(students)

        // When
        viewModel = StudentListViewModel(getStudentsUseCase, logoutUseCase, authRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(students, viewModel.state.value.students)
        verify { getStudentsUseCase(userId) }
    }

    @Test
    fun `init should not load students when user is null`() = runTest {
        // Given
        every { authRepository.currentUser } returns MutableStateFlow(null)

        // When
        viewModel = StudentListViewModel(getStudentsUseCase, logoutUseCase, authRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(emptyList<Student>(), viewModel.state.value.students)
        verify(exactly = 0) { getStudentsUseCase(any()) }
    }

    @Test
    fun `logout should call logout use case`() = runTest {
        // Given
        every { authRepository.currentUser } returns MutableStateFlow(null)
        viewModel = StudentListViewModel(getStudentsUseCase, logoutUseCase, authRepository)

        // When
        viewModel.logout()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { logoutUseCase() }
    }
}
