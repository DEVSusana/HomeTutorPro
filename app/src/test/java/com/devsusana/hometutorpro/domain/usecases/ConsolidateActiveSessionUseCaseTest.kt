package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.constants.SessionConstants
import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.ActiveSession
import com.devsusana.hometutorpro.domain.entities.Student
import com.devsusana.hometutorpro.domain.entities.User
import com.devsusana.hometutorpro.domain.usecases.implementations.ConsolidateActiveSessionUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ConsolidateActiveSessionUseCaseTest {

    private val getActiveSessionUseCase: IGetActiveSessionUseCase = mockk()
    private val getCurrentUserUseCase: IGetCurrentUserUseCase = mockk()
    private val getStudentByIdUseCase: IGetStudentByIdUseCase = mockk()
    private val addToBalanceUseCase: IAddToBalanceUseCase = mockk()
    private val saveStudentUseCase: ISaveStudentUseCase = mockk()

    private lateinit var useCase: ConsolidateActiveSessionUseCase

    private val testUser = User(uid = "tutor123", email = "tutor@test.com", displayName = "Tutor")
    private val testStudent = Student(
        id = "student_ana",
        professorId = "tutor123",
        name = "Ana",
        pricePerHour = 20.0,
        pendingBalance = 0.0
    )

    @Before
    fun setUp() {
        useCase = ConsolidateActiveSessionUseCase(
            getActiveSessionUseCase,
            getCurrentUserUseCase,
            getStudentByIdUseCase,
            addToBalanceUseCase,
            saveStudentUseCase
        )

        every { getCurrentUserUseCase() } returns MutableStateFlow(testUser)
        every { getStudentByIdUseCase("tutor123", "student_ana") } returns flowOf(testStudent)
        coEvery { addToBalanceUseCase("tutor123", "student_ana", any()) } returns Result.Success(Unit)
        coEvery { saveStudentUseCase("tutor123", any()) } returns Result.Success("student_ana")
    }

    @Test
    fun `invoke returns false when no active session exists`() = runTest {
        every { getActiveSessionUseCase() } returns null

        val result = useCase()

        assertTrue(result is Result.Success)
        assertEquals(false, (result as Result.Success).data)
        coVerify(exactly = 0) { addToBalanceUseCase(any(), any(), any()) }
    }

    @Test
    fun `invoke returns false and skips billing when session elapsed time is less than 15 minutes`() = runTest {
        val youngSession = ActiveSession(
            studentId = "student_ana",
            studentName = "Ana",
            startTimeMillis = System.currentTimeMillis() - (5 * 60 * 1000L), // 5 min (< 15 min)
            durationMinutes = 60L,
            isOngoing = true,
            tutorId = "tutor123",
            isBilled = false
        )
        every { getActiveSessionUseCase() } returns youngSession

        val result = useCase()

        assertTrue(result is Result.Success)
        assertEquals(false, (result as Result.Success).data)
        coVerify(exactly = 0) { addToBalanceUseCase(any(), any(), any()) }
    }

    @Test
    fun `invoke returns false when session is already billed`() = runTest {
        val billedSession = ActiveSession(
            studentId = "student_ana",
            studentName = "Ana",
            startTimeMillis = System.currentTimeMillis() - (20 * 60 * 1000L),
            durationMinutes = 60L,
            isOngoing = true,
            tutorId = "tutor123",
            isBilled = true
        )
        every { getActiveSessionUseCase() } returns billedSession

        val result = useCase()

        assertTrue(result is Result.Success)
        assertEquals(false, (result as Result.Success).data)
        coVerify(exactly = 0) { addToBalanceUseCase(any(), any(), any()) }
    }

    @Test
    fun `invoke charges balance when session has run for at least 15 minutes`() = runTest {
        val matureSession = ActiveSession(
            studentId = "student_ana",
            studentName = "Ana",
            startTimeMillis = System.currentTimeMillis() - (16 * 60 * 1000L), // 16 min (>= 15 min)
            durationMinutes = 60L,
            isOngoing = true,
            tutorId = "tutor123",
            isBilled = false
        )
        every { getActiveSessionUseCase() } returns matureSession

        val result = useCase()

        assertTrue(result is Result.Success)
        assertEquals(true, (result as Result.Success).data)
        coVerify(exactly = 1) { addToBalanceUseCase("tutor123", "student_ana", 20.0) }
    }

    @Test
    fun `invoke records lastClassDate without monetary charge when student price is zero`() = runTest {
        val freeStudent = testStudent.copy(pricePerHour = 0.0)
        every { getStudentByIdUseCase("tutor123", "student_ana") } returns flowOf(freeStudent)

        val matureSession = ActiveSession(
            studentId = "student_ana",
            studentName = "Ana",
            startTimeMillis = System.currentTimeMillis() - (15 * 60 * 1000L), // exactly 15 min
            durationMinutes = 60L,
            isOngoing = true,
            tutorId = "tutor123",
            isBilled = false
        )
        every { getActiveSessionUseCase() } returns matureSession

        val result = useCase()

        assertTrue(result is Result.Success)
        assertEquals(true, (result as Result.Success).data)
        coVerify(exactly = 0) { addToBalanceUseCase(any(), any(), any()) }
        coVerify(exactly = 1) { saveStudentUseCase("tutor123", match { it.lastClassDate != null }) }
    }
}
