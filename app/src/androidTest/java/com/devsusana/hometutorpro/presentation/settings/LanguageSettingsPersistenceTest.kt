package com.devsusana.hometutorpro.presentation.settings

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.devsusana.hometutorpro.core.settings.SettingsManager
import com.devsusana.hometutorpro.core.settings.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test to verify that language settings are persisted correctly.
 * This test ensures the fix for the bug where language switching caused mixed localizations
 * due to a race condition between saving settings and restarting the activity.
 */
@RunWith(AndroidJUnit4::class)
class LanguageSettingsPersistenceTest {

    private lateinit var context: Context
    private lateinit var settingsManager: SettingsManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        settingsManager = SettingsManager(context)
        
        // Clear any existing language preference
        runBlocking {
            settingsManager.setLanguage(SettingsManager.LANGUAGE_SPANISH) // Reset to default
        }
    }

    @Test
    fun setLanguage_toEnglish_shouldPersistCorrectly() = runBlocking {
        // When: Set language to English
        settingsManager.setLanguage(SettingsManager.LANGUAGE_ENGLISH)

        // Then: Verify it's persisted
        val savedLanguage = context.dataStore.data.first()[stringPreferencesKey("language")]
        assertEquals(
            "Language should be persisted as English",
            SettingsManager.LANGUAGE_ENGLISH,
            savedLanguage
        )
    }

    @Test
    fun setLanguage_toSpanish_shouldPersistCorrectly() = runBlocking {
        // When: Set language to Spanish
        settingsManager.setLanguage(SettingsManager.LANGUAGE_SPANISH)

        // Then: Verify it's persisted
        val savedLanguage = context.dataStore.data.first()[stringPreferencesKey("language")]
        assertEquals(
            "Language should be persisted as Spanish",
            SettingsManager.LANGUAGE_SPANISH,
            savedLanguage
        )
    }

    @Test
    fun setLanguage_multipleChanges_shouldPersistLatest() = runBlocking {
        // Given: Start with English
        settingsManager.setLanguage(SettingsManager.LANGUAGE_ENGLISH)
        
        // When: Change to Spanish
        settingsManager.setLanguage(SettingsManager.LANGUAGE_SPANISH)
        
        // Then: Verify Spanish is persisted
        val savedLanguage1 = context.dataStore.data.first()[stringPreferencesKey("language")]
        assertEquals(
            "Language should be Spanish after first change",
            SettingsManager.LANGUAGE_SPANISH,
            savedLanguage1
        )
        
        // When: Change back to English
        settingsManager.setLanguage(SettingsManager.LANGUAGE_ENGLISH)
        
        // Then: Verify English is persisted
        val savedLanguage2 = context.dataStore.data.first()[stringPreferencesKey("language")]
        assertEquals(
            "Language should be English after second change",
            SettingsManager.LANGUAGE_ENGLISH,
            savedLanguage2
        )
    }

    @Test
    fun setLanguage_rapidChanges_shouldPersistLatest() = runBlocking {
        // When: Make rapid language changes (simulating user clicking multiple times)
        settingsManager.setLanguage(SettingsManager.LANGUAGE_ENGLISH)
        settingsManager.setLanguage(SettingsManager.LANGUAGE_SPANISH)
        settingsManager.setLanguage(SettingsManager.LANGUAGE_ENGLISH)
        settingsManager.setLanguage(SettingsManager.LANGUAGE_SPANISH)

        // Then: Verify the latest change is persisted
        val savedLanguage = context.dataStore.data.first()[stringPreferencesKey("language")]
        assertEquals(
            "Language should be Spanish (the last change)",
            SettingsManager.LANGUAGE_SPANISH,
            savedLanguage
        )
    }
}
