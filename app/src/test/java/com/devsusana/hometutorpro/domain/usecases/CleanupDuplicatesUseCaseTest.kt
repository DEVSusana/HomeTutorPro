package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.repository.ScheduleExceptionRepository
import com.devsusana.hometutorpro.domain.usecases.implementations.CleanupDuplicatesUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [CleanupDuplicatesUseCase].
 */
class CleanupDuplicatesUseCaseTest {

    private val repository: ScheduleExceptionRepository = mockk()
    private val useCase = CleanupDuplicatesUseCase(repository)

    @Test
    fun `invoke should call repository cleanupDuplicates and return success`() = runTest {
        // Given
        coEvery { repository.cleanupDuplicates() } returns Result.Success(Unit)

        // When
        val result = useCase()

        // Then
        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { repository.cleanupDuplicates() }
    }

    @Test
    fun `invoke should propagate error from repository`() = runTest {
        // Given
        val error = DomainError.Unknown
        coEvery { repository.cleanupDuplicates() } returns Result.Error(error)

        // When
        val result = useCase()

        // Then
        assertTrue(result is Result.Error)
        assertEquals(error, (result as Result.Error).error)
        coVerify(exactly = 1) { repository.cleanupDuplicates() }
    }
}
