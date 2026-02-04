package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.repository.ResourceRepository
import com.devsusana.hometutorpro.domain.usecases.implementations.DeleteResourceUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteResourceUseCaseTest {

    private val repository: ResourceRepository = mockk()
    private val useCase = DeleteResourceUseCase(repository)

    @Test
    fun `invoke should call repository deleteResource`() = runTest {
        // Given
        val professorId = "prof1"
        val resourceId = "res1"
        coEvery { repository.deleteResource(professorId, resourceId) } returns Result.Success(Unit)

        // When
        val result = useCase(professorId, resourceId)

        // Then
        assertTrue(result is Result.Success)
        coVerify { repository.deleteResource(professorId, resourceId) }
    }
}
