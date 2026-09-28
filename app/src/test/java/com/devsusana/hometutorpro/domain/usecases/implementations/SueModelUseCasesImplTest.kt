package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.entities.SueModelStatus
import com.devsusana.hometutorpro.domain.repository.SueModelRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SueModelUseCasesImplTest {

    private lateinit var repository: SueModelRepository

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
    }

    @Test
    fun `GetSueModelStatusUseCase delegates to repository modelStatusFlow`() = runTest {
        val expectedStatus = SueModelStatus.Downloaded(550_000_000L, "550 MB")
        every { repository.modelStatusFlow } returns flowOf(expectedStatus)

        val useCase = GetSueModelStatusUseCaseImpl(repository)
        val status = useCase().first()

        assertEquals(expectedStatus, status)
    }

    @Test
    fun `DownloadSueModelUseCase delegates to repository downloadModel`() = runTest {
        val expectedFlow = flowOf(SueModelStatus.Downloading(0L, 550L, 0f))
        coEvery { repository.downloadModel(any()) } returns expectedFlow

        val useCase = DownloadSueModelUseCaseImpl(repository)
        val flow = useCase("https://custom.url/model.bin")

        assertEquals(expectedFlow, flow)
        coVerify { repository.downloadModel("https://custom.url/model.bin") }
    }

    @Test
    fun `CancelSueModelDownloadUseCase delegates to repository cancelDownload`() {
        val useCase = CancelSueModelDownloadUseCaseImpl(repository)
        useCase()

        verify { repository.cancelDownload() }
    }

    @Test
    fun `CheckSueCompatibilityUseCase delegates to repository checkDeviceCompatibility`() {
        val expected = com.devsusana.hometutorpro.domain.entities.SueDeviceCompatibility(isSupported = true)
        every { repository.checkDeviceCompatibility() } returns expected

        val useCase = CheckSueCompatibilityUseCaseImpl(repository)
        val result = useCase()

        assertEquals(expected, result)
        verify { repository.checkDeviceCompatibility() }
    }

    @Test
    fun `DeleteSueModelUseCase delegates to repository deleteModel`() = runTest {
        coEvery { repository.deleteModel() } returns true

        val useCase = DeleteSueModelUseCaseImpl(repository)
        val result = useCase()

        assertTrue(result)
        coVerify { repository.deleteModel() }
    }
}
