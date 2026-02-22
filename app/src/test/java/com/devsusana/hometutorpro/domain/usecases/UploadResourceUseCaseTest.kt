package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.repository.ResourceRepository
import com.devsusana.hometutorpro.domain.usecases.implementations.UploadResourceUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class UploadResourceUseCaseTest {

    private val repository: ResourceRepository = mockk()
    private val useCase = UploadResourceUseCase(repository)

    @Test
    fun `invoke should call repository uploadResource`() = runTest {
        // Given
        val professorId = "prof1"
        val name = "Resource 1"
        val uri = "content://file"
        val fileType = "pdf"
        coEvery { repository.uploadResource(professorId, name, fileType, uri) } returns Result.Success(Unit)

        // When
        val result = useCase(professorId, name, fileType, uri)

        // Then
        assertTrue(result is Result.Success)
        coVerify { repository.uploadResource(professorId, name, fileType, uri) }
    }
}
