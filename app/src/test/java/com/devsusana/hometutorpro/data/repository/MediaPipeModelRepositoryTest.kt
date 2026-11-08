package com.devsusana.hometutorpro.data.repository

import android.content.Context
import com.devsusana.hometutorpro.domain.entities.AppFeatureFlags
import com.devsusana.hometutorpro.domain.repository.IConfigRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class MediaPipeModelRepositoryTest {

    private lateinit var context: Context
    private lateinit var configRepository: IConfigRepository
    private val featureFlagsFlow = MutableStateFlow(AppFeatureFlags(isSueLocalAiEnabled = true))

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        configRepository = mockk(relaxed = true)

        every { configRepository.featureFlags } returns featureFlagsFlow
        every { configRepository.isSueLocalAiEnabled() } returns true
        every { context.filesDir } returns File("/tmp/nonexistent_files_dir")
        every { context.getExternalFilesDir(null) } returns null
    }

    @Test
    fun `loadModel returns false when remote config disables local AI`() = runTest {
        every { configRepository.isSueLocalAiEnabled() } returns false

        val repository = MediaPipeModelRepository(context, configRepository)

        val result = repository.loadModel()

        assertFalse(result)
        assertFalse(repository.isModelLoaded.value)
    }

    @Test
    fun `loadModel returns false when model file is not found`() = runTest {
        every { configRepository.isSueLocalAiEnabled() } returns true

        val repository = MediaPipeModelRepository(context, configRepository)

        val result = repository.loadModel()

        assertFalse(result)
        assertFalse(repository.isModelLoaded.value)
    }

    @Test
    fun `generateResponse returns maintenance message when kill-switch is active`() = runTest {
        val originalLocale = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale.ENGLISH)
            every { configRepository.isSueLocalAiEnabled() } returns false
            every { configRepository.getMaintenanceMessage() } returns ""

            val repository = MediaPipeModelRepository(context, configRepository)
            val response = repository.generateResponse("How do I solve quadratic equations?")

            assertTrue(response.contains("temporarily paused for maintenance"))
        } finally {
            java.util.Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun `generateResponse returns Spanish maintenance message when locale is Spanish`() = runTest {
        val originalLocale = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale.forLanguageTag("es-ES"))
            every { configRepository.isSueLocalAiEnabled() } returns false
            every { configRepository.getMaintenanceMessage() } returns ""

            val repository = MediaPipeModelRepository(context, configRepository)
            val response = repository.generateResponse("¿Cómo resuelvo ecuaciones?")

            assertTrue(response.contains("pausada temporalmente por mantenimiento"))
        } finally {
            java.util.Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun `generateResponse returns custom maintenance message when configured in remote config`() = runTest {
        every { configRepository.isSueLocalAiEnabled() } returns false
        every { configRepository.getMaintenanceMessage() } returns "Mantenimiento programado: volvemos a las 18:00"

        val repository = MediaPipeModelRepository(context, configRepository)
        val response = repository.generateResponse("Test query")

        assertEquals("Mantenimiento programado: volvemos a las 18:00", response)
    }

    @Test
    fun `generateResponse returns model not ready message when model is not loaded`() = runTest {
        val originalLocale = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale.ENGLISH)
            every { configRepository.isSueLocalAiEnabled() } returns true

            val repository = MediaPipeModelRepository(context, configRepository)

            val response = repository.generateResponse("Hello SUE")

            assertTrue(response.contains("AI model is not ready yet"))
        } finally {
            java.util.Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun `generateResponse returns Spanish model not ready message when locale is Spanish and model not loaded`() = runTest {
        val originalLocale = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale.forLanguageTag("es-ES"))
            every { configRepository.isSueLocalAiEnabled() } returns true

            val repository = MediaPipeModelRepository(context, configRepository)

            val response = repository.generateResponse("Hola SUE")

            assertTrue(response.contains("El modelo de IA no está listo todavía"))
        } finally {
            java.util.Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun `release resets model loading and loaded state`() = runTest {
        val repository = MediaPipeModelRepository(context, configRepository)

        repository.release()

        assertFalse(repository.isModelLoaded.value)
        assertFalse(repository.isLoading.value)
    }

    @Test
    fun `feature flags change triggers automatic model release when local AI disabled`() = runTest {
        val repository = MediaPipeModelRepository(context, configRepository)

        // Simulate config change to disabled
        featureFlagsFlow.value = AppFeatureFlags(isSueLocalAiEnabled = false)

        assertFalse(repository.isModelLoaded.value)
    }
}
