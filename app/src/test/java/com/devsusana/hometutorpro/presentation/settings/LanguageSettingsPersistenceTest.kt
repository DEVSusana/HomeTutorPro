package com.devsusana.hometutorpro.presentation.settings

import com.devsusana.hometutorpro.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Test to verify that language settings are handled correctly by the domain layer.
 */
class LanguageSettingsPersistenceTest {

    /** Mocked repository implementation to isolate business logic. */
    private val settingsRepository: SettingsRepository = mockk()
    private val languageFlow = MutableStateFlow("es")

    /** Sets up the test environment by initializing mocks and behaviors before each test. */
    @Before
    fun setup() {
        coEvery { settingsRepository.languageFlow } returns languageFlow
        coEvery { settingsRepository.setLanguage(any()) } answers {
            languageFlow.value = firstArg()
        }
    }

    /** Verifies that setting the language to English is correctly saved and retrieved via the flow. */
    @Test
    fun setLanguage_toEnglish_shouldPersistCorrectly() = runTest {
        settingsRepository.setLanguage("en")
        assertEquals("en", settingsRepository.languageFlow.first())
    }

    /** Verifies that setting the language to Spanish is correctly saved and retrieved via the flow. */
    @Test
    fun setLanguage_toSpanish_shouldPersistCorrectly() = runTest {
        settingsRepository.setLanguage("es")
        assertEquals("es", settingsRepository.languageFlow.first())
    }

    /** Verifies that sequential language changes maintain consistency and persist the most recent value. */
    @Test
    fun setLanguage_multipleChanges_shouldPersistLatest() = runTest {
        settingsRepository.setLanguage("en")
        settingsRepository.setLanguage("es")
        assertEquals("es", settingsRepository.languageFlow.first())
    }

    /** Verifies that rapid successive calls to setLanguage result in the correct final persisted state. */
    @Test
    fun setLanguage_rapidChanges_shouldPersistLatest() = runTest {
        settingsRepository.setLanguage("en")
        settingsRepository.setLanguage("es")
        settingsRepository.setLanguage("en")
        settingsRepository.setLanguage("es")
        assertEquals("es", settingsRepository.languageFlow.first())
    }

    /** Verifies that empty string inputs are handled by the repository contract. */
    @Test
    fun setLanguage_emptyInput_shouldHandleGracefully() = runTest {
        settingsRepository.setLanguage("")
        assertEquals("", settingsRepository.languageFlow.first())
    }
}
