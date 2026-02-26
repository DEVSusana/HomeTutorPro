package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.devsusana.hometutorpro.domain.usecases.implementations.UpdatePasswordUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class UpdatePasswordUseCaseTest {

    private val repository: AuthRepository = mockk()
    private val updatePasswordUseCase = UpdatePasswordUseCase(repository)

    @Test
    fun `invoke returns Success when repository updatePassword succeeds`() = runTest {
        coEvery { repository.updatePassword("newPassword") } returns Result.Success(Unit)

        val result = updatePasswordUseCase("newPassword")

        assert(result is Result.Success)
    }

    @Test
    fun `invoke returns Error when repository updatePassword fails`() = runTest {
        coEvery { repository.updatePassword("newPassword") } returns Result.Error(DomainError.Unknown)

        val result = updatePasswordUseCase("newPassword")

        assert(result is Result.Error)
        assertEquals(DomainError.Unknown, (result as Result.Error).error)
    }
}
