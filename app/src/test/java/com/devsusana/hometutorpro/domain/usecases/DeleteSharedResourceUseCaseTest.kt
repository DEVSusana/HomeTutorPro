package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.repository.ResourceRepository
import com.devsusana.hometutorpro.domain.usecases.implementations.DeleteSharedResourceUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteSharedResourceUseCaseTest {

    private val resourceRepository: ResourceRepository = mockk()
    private val useCase = DeleteSharedResourceUseCase(resourceRepository)

    @Test
    fun `invoke should return success when repository succeeds`() = runTest {
        val professorId = "prof1"
        val resourceId = "res1"

        coEvery { resourceRepository.deleteSharedResource(professorId, resourceId) } returns Result.Success(Unit)

        val result = useCase(professorId, resourceId)

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { resourceRepository.deleteSharedResource(professorId, resourceId) }
    }

    @Test
    fun `invoke should return error when repository fails`() = runTest {
        val professorId = "prof1"
        val resourceId = "res1"

        coEvery { resourceRepository.deleteSharedResource(professorId, resourceId) } returns Result.Error(DomainError.ResourceNotFound)

        val result = useCase(professorId, resourceId)

        assertTrue(result is Result.Error)
        coVerify(exactly = 1) { resourceRepository.deleteSharedResource(professorId, resourceId) }
    }
}
