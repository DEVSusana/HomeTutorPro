package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.Resource
import com.devsusana.hometutorpro.domain.repository.ResourceRepository
import com.devsusana.hometutorpro.domain.usecases.implementations.GetResourcesUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetResourcesUseCaseTest {

    private val repository: ResourceRepository = mockk()
    private val useCase = GetResourcesUseCase(repository)

    @Test
    fun `invoke should return list of resources from repository`() = runTest {
        // Given
        val professorId = "prof1"
        val resources = listOf(
            Resource(id = "1", name = "Resource 1", url = "http://example.com", professorId = professorId)
        )
        every { repository.getResources(professorId) } returns flowOf(resources)

        // When
        val result = useCase(professorId).first()

        // Then
        assertEquals(resources, result)
        verify { repository.getResources(professorId) }
    }
}
