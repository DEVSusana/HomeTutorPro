package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.ActiveSession
import com.devsusana.hometutorpro.domain.repository.IActiveSessionRepository
import com.devsusana.hometutorpro.domain.usecases.implementations.GetActiveSessionUseCase
import com.devsusana.hometutorpro.domain.usecases.implementations.StartActiveSessionUseCase
import com.devsusana.hometutorpro.domain.usecases.implementations.StopActiveSessionUseCase
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActiveSessionUseCasesTest {

    private val repository: IActiveSessionRepository = mockk()
    
    private val getActiveSessionUseCase = GetActiveSessionUseCase(repository)
    private val startActiveSessionUseCase = StartActiveSessionUseCase(repository)
    private val stopActiveSessionUseCase = StopActiveSessionUseCase(repository)

    @Test
    fun `getActiveSession should return session when repository returns session`() {
        val session = ActiveSession("stud1", "Juan", 1000L, 60L, true)
        every { repository.getActiveSession() } returns session

        val result = getActiveSessionUseCase()

        assertEquals(session, result)
        verify(exactly = 1) { repository.getActiveSession() }
    }

    @Test
    fun `getActiveSession should return null when repository returns null`() {
        every { repository.getActiveSession() } returns null

        val result = getActiveSessionUseCase()

        assertNull(result)
    }

    @Test
    fun `startSession should call repository startSession`() {
        val session = ActiveSession("stud1", "Juan", 1000L, 60L, true)
        every { repository.startSession(session) } just runs

        startActiveSessionUseCase(session)

        verify(exactly = 1) { repository.startSession(session) }
    }

    @Test
    fun `stopSession should call repository stopSession`() {
        every { repository.stopSession() } just runs

        stopActiveSessionUseCase()

        verify(exactly = 1) { repository.stopSession() }
    }
}
