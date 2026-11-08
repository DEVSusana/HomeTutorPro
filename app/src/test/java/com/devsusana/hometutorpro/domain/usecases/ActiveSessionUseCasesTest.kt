package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.ActiveSession
import com.devsusana.hometutorpro.domain.repository.IActiveSessionRepository
import com.devsusana.hometutorpro.domain.usecases.implementations.GetActiveSessionUseCase
import com.devsusana.hometutorpro.domain.usecases.implementations.StartActiveSessionUseCase
import com.devsusana.hometutorpro.domain.usecases.implementations.StopActiveSessionUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActiveSessionUseCasesTest {

    private val repository: IActiveSessionRepository = mockk()

    private val getActiveSessionUseCase = GetActiveSessionUseCase(repository)
    private val startActiveSessionUseCase = StartActiveSessionUseCase(repository)
    private val stopActiveSessionUseCase = StopActiveSessionUseCase(repository)

    @Test
    fun `getActiveSession should return session when repository returns session`() = runTest {
        val session = ActiveSession("stud1", "Juan", 1000L, 60L, true)
        coEvery { repository.getActiveSession() } returns session

        val result = getActiveSessionUseCase()

        assertEquals(session, result)
        coVerify(exactly = 1) { repository.getActiveSession() }
    }

    @Test
    fun `getActiveSession should return null when repository returns null`() = runTest {
        coEvery { repository.getActiveSession() } returns null

        val result = getActiveSessionUseCase()

        assertNull(result)
        coVerify(exactly = 1) { repository.getActiveSession() }
    }

    @Test
    fun `startSession should call repository startSession`() = runTest {
        val session = ActiveSession("stud1", "Juan", 1000L, 60L, true)
        coEvery { repository.startSession(session) } just runs

        startActiveSessionUseCase(session)

        coVerify(exactly = 1) { repository.startSession(session) }
    }

    @Test
    fun `stopSession should call repository stopSession`() = runTest {
        coEvery { repository.stopSession() } just runs

        stopActiveSessionUseCase()

        coVerify(exactly = 1) { repository.stopSession() }
    }
}
