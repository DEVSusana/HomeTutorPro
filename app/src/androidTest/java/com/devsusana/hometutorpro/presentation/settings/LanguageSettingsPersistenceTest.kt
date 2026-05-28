package com.devsusana.hometutorpro.presentation.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.devsusana.hometutorpro.data.repository.SettingsRepositoryImpl
import com.devsusana.hometutorpro.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test to verify that language settings are persisted correctly.
 * Asserts settings persistence logic by checking updates emitted by [SettingsRepository.languageFlow].
 */
@RunWith(AndroidJUnit4::class)
class LanguageSettingsPersistenceTest {

    private lateinit var context: Context
    private lateinit var settingsRepository: SettingsRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        settingsRepository = SettingsRepositoryImpl(context)
        
        // Clear/Reset language preference to Spanish
        runBlocking {
            settingsRepository.setLanguage("es")
        }
    }

    @Test
    fun setLanguage_toEnglish_shouldPersistCorrectly() = runBlocking {
        // When: Set language to English
        settingsRepository.setLanguage("en")

        // Then: Verify it's persisted
        val savedLanguage = settingsRepository.languageFlow.first()
        assertEquals(
            "Language should be persisted as English",
            "en",
            savedLanguage
        )
    }

    @Test
    fun setLanguage_toSpanish_shouldPersistCorrectly() = runBlocking {
        // When: Set language to Spanish
        settingsRepository.setLanguage("es")

        // Then: Verify it's persisted
        val savedLanguage = settingsRepository.languageFlow.first()
        assertEquals(
            "Language should be persisted as Spanish",
            "es",
            savedLanguage
        )
    }

    @Test
    fun setLanguage_multipleChanges_shouldPersistLatest() = runBlocking {
        // Given: Start with English
        settingsRepository.setLanguage("en")
        
        // When: Change to Spanish
        settingsRepository.setLanguage("es")
        
        // Then: Verify Spanish is persisted
        val savedLanguage1 = settingsRepository.languageFlow.first()
        assertEquals(
            "Language should be Spanish after first change",
            "es",
            savedLanguage1
        )
        
        // When: Change back to English
        settingsRepository.setLanguage("en")
        
        // Then: Verify English is persisted
        val savedLanguage2 = settingsRepository.languageFlow.first()
        assertEquals(
            "Language should be English after second change",
            "en",
            savedLanguage2
        )
    }

    @Test
    fun setLanguage_rapidChanges_shouldPersistLatest() = runBlocking {
        // When: Make rapid language changes
        settingsRepository.setLanguage("en")
        settingsRepository.setLanguage("es")
        settingsRepository.setLanguage("en")
        settingsRepository.setLanguage("es")

        // Then: Verify the latest change is persisted
        val savedLanguage = settingsRepository.languageFlow.first()
        assertEquals(
            "Language should be Spanish (the last change)",
            "es",
            savedLanguage
        )
    }
}
