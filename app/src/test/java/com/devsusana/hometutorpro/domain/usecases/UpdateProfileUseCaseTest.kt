package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.devsusana.hometutorpro.domain.usecases.implementations.UpdateProfileUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateProfileUseCaseTest {

    private val repository: AuthRepository = mockk()
    private val updateProfileUseCase = UpdateProfileUseCase(repository)

    @Test
    fun `invoke returns Success when repository updateProfile succeeds`() = runTest {
        coEvery {
            repository.updateProfile(
                name = "Name",
                email = "email@test.com",
                workingStartTime = "08:00",
                workingEndTime = "23:00",
                notes = "notes"
            )
        } returns Result.Success(Unit)

        val result = updateProfileUseCase(
            name = "Name",
            email = "email@test.com",
            workingStartTime = "08:00",
            workingEndTime = "23:00",
            notes = "notes"
        )

        assert(result is Result.Success)
    }

    @Test
    fun `invoke returns Error when repository updateProfile fails`() = runTest {
        coEvery {
            repository.updateProfile(
                name = "Name",
                email = "email@test.com",
                workingStartTime = "08:00",
                workingEndTime = "23:00",
                notes = "notes"
            )
        } returns Result.Error(DomainError.Unknown)

        val result = updateProfileUseCase(
            name = "Name",
            email = "email@test.com",
            workingStartTime = "08:00",
            workingEndTime = "23:00",
            notes = "notes"
        )

        assert(result is Result.Error)
        assertEquals(DomainError.Unknown, (result as Result.Error).error)
    }
}
