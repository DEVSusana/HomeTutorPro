package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.entities.AppFeatureFlags
import com.devsusana.hometutorpro.domain.repository.IConfigRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ConfigUseCasesTest {

    private lateinit var configRepository: IConfigRepository

    @Before
    fun setUp() {
        configRepository = mockk()
    }

    @Test
    fun getAppFeatureFlagsUseCase_returnsRepositoryFlow() {
        val expectedFlags = AppFeatureFlags(isWidgetActionsEnabled = false)
        val flow = MutableStateFlow(expectedFlags)
        every { configRepository.featureFlags } returns flow

        val useCase = GetAppFeatureFlagsUseCase(configRepository)
        val resultFlow = useCase()

        assertEquals(expectedFlags, resultFlow.value)
    }

    @Test
    fun fetchRemoteConfigUseCase_invokesRepositoryAndReturnsResult() = runTest {
        coEvery { configRepository.fetchAndActivate() } returns true

        val useCase = FetchRemoteConfigUseCase(configRepository)
        val result = useCase()

        assertTrue(result)
    }
}
