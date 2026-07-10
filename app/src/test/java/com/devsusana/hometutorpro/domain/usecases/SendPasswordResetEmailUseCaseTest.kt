package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.devsusana.hometutorpro.domain.usecases.implementations.SendPasswordResetEmailUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SendPasswordResetEmailUseCaseTest {

    private val repository: AuthRepository = mockk()
    private val sendPasswordResetEmailUseCase = SendPasswordResetEmailUseCase(repository)

    @Test
    fun `invoke returns Success when repository sendPasswordResetEmail succeeds`() = runTest {
        coEvery { repository.sendPasswordResetEmail("test@example.com") } returns Result.Success(Unit)

        val result = sendPasswordResetEmailUseCase("test@example.com")

        assert(result is Result.Success)
    }

    @Test
    fun `invoke returns Error when repository sendPasswordResetEmail fails`() = runTest {
        coEvery { repository.sendPasswordResetEmail("test@example.com") } returns Result.Error(DomainError.UserNotFound)

        val result = sendPasswordResetEmailUseCase("test@example.com")

        assert(result is Result.Error)
        assertEquals(DomainError.UserNotFound, (result as Result.Error).error)
    }
}
