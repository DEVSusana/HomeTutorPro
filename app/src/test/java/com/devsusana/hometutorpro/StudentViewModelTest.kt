package com.devsusana.hometutorpro

import com.devsusana.hometutorpro.domain.entities.StudentSummary
import com.devsusana.hometutorpro.domain.entities.User
import com.devsusana.hometutorpro.domain.usecases.IGetCurrentUserUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetStudentsUseCase
import com.devsusana.hometutorpro.domain.usecases.ILogoutUseCase
import com.devsusana.hometutorpro.domain.usecases.IToggleStudentActiveUseCase
import com.devsusana.hometutorpro.presentation.student_list.StudentListViewModel
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

/**
 * Unit tests mapping the Student list/interaction view model logic (implemented via [StudentListViewModel])
 * to satisfy CI testing pyramid guidelines.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StudentViewModelTest {

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
    fun init_loadsStudentsWhenLoggedIn() = runTest {
        val userId = "user_abc"
        val user = User(uid = userId, email = "test@example.com", displayName = "Test Prof")
        val students = listOf(
            StudentSummary(id = "1", name = "John Doe", subjects = "Math", color = null, pendingBalance = 0.0, pricePerHour = 20.0, isActive = true, lastClassDate = null)
        )

        every { getStudentsUseCase(userId) } returns flowOf(students)
        every { getCurrentUserUseCase() } returns MutableStateFlow(user)

        viewModel = StudentListViewModel(
            getStudentsUseCase,
            logoutUseCase,
            toggleStudentActiveUseCase,
            getCurrentUserUseCase
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(students, viewModel.state.value.students)
        verify { getStudentsUseCase(userId) }
    }
}
