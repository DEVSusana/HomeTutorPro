package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.SharedResource
import com.devsusana.hometutorpro.domain.entities.ShareMethod
import com.devsusana.hometutorpro.domain.repository.ResourceRepository
import com.devsusana.hometutorpro.domain.usecases.implementations.SaveSharedResourceUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveSharedResourceUseCaseTest {

    private val resourceRepository: ResourceRepository = mockk()
    private val useCase = SaveSharedResourceUseCase(resourceRepository)

    @Test
    fun `invoke should return success when repository succeeds`() = runTest {
        val professorId = "prof1"
        val resource = SharedResource(
            id = "res1",
            studentId = "student1",
            professorId = professorId,
            fileName = "homework.pdf",
            fileType = "pdf",
            fileSizeBytes = 2048,
            sharedVia = ShareMethod.EMAIL
        )

        coEvery { resourceRepository.saveSharedResource(professorId, resource) } returns Result.Success(Unit)

        val result = useCase(professorId, resource)

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { resourceRepository.saveSharedResource(professorId, resource) }
    }

    @Test
    fun `invoke should return error when repository fails`() = runTest {
        val professorId = "prof1"
        val resource = SharedResource(
            id = "res1",
            studentId = "student1",
            professorId = professorId,
            fileName = "homework.pdf",
            fileType = "pdf",
            fileSizeBytes = 2048,
            sharedVia = ShareMethod.EMAIL
        )

        coEvery { resourceRepository.saveSharedResource(professorId, resource) } returns Result.Error(DomainError.ResourceNotFound)

        val result = useCase(professorId, resource)

        assertTrue(result is Result.Error)
        coVerify(exactly = 1) { resourceRepository.saveSharedResource(professorId, resource) }
    }
}
