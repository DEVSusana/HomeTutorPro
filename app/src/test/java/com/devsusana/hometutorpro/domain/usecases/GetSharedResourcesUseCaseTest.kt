package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.SharedResource
import com.devsusana.hometutorpro.domain.entities.ShareMethod
import com.devsusana.hometutorpro.domain.repository.ResourceRepository
import com.devsusana.hometutorpro.domain.usecases.implementations.GetSharedResourcesUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetSharedResourcesUseCaseTest {

    private val resourceRepository: ResourceRepository = mockk()
    private val useCase = GetSharedResourcesUseCase(resourceRepository)

    @Test
    fun `invoke should return shared resources when repository succeeds`() = runTest {
        val professorId = "prof1"
        val studentId = "student1"
        val resources = listOf(
            SharedResource(
                id = "res1",
                studentId = studentId,
                professorId = professorId,
                fileName = "worksheet.pdf",
                fileType = "pdf",
                fileSizeBytes = 1200,
                sharedVia = ShareMethod.EMAIL
            )
        )

        every { resourceRepository.getSharedResources(professorId, studentId) } returns flowOf(resources)

        val result = useCase(professorId, studentId).first()

        assertEquals(resources, result)
        verify(exactly = 1) { resourceRepository.getSharedResources(professorId, studentId) }
    }
}
