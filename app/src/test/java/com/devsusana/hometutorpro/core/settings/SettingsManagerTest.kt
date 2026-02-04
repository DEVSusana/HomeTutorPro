package com.devsusana.hometutorpro.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Unit tests for SettingsManager.
 * Tests DataStore operations for language, view mode, debug premium, and theme settings.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SettingsManagerTest {

    private lateinit var context: Context
    private lateinit var testDataStore: DataStore<Preferences>
    private lateinit var settingsManager: SettingsManager
    private lateinit var testScope: TestScope

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        testScope = TestScope(UnconfinedTestDispatcher() + Job())
        
        // Create a test DataStore with a unique name
        testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { context.preferencesDataStoreFile("test_settings_${System.currentTimeMillis()}") }
        )
        
        // Create SettingsManager with test context
        settingsManager = SettingsManager(context)
        
        // Clear DataStore to ensure clean state
        runTest {
            context.dataStore.edit { it.clear() }
        }
    }

    @After
    fun tearDown() {
        // Clean up test DataStore file
        context.preferencesDataStoreFile("test_settings").delete()
    }

    // Language Tests

    @Test
    fun languageFlow_defaultsToSpanish() = testScope.runTest {
        // When: Reading language without setting it
        val language = settingsManager.languageFlow.first()

        // Then: Should default to Spanish
        assertEquals(SettingsManager.LANGUAGE_SPANISH, language)
    }

    @Test
    fun setLanguage_toEnglish_persistsCorrectly() = testScope.runTest {
        // When: Set language to English
        settingsManager.setLanguage(SettingsManager.LANGUAGE_ENGLISH)

        // Then: Language flow should emit English
        val language = settingsManager.languageFlow.first()
        assertEquals(SettingsManager.LANGUAGE_ENGLISH, language)
    }

    @Test
    fun setLanguage_toSpanish_persistsCorrectly() = testScope.runTest {
        // Given: Language is English
        settingsManager.setLanguage(SettingsManager.LANGUAGE_ENGLISH)

        // When: Change to Spanish
        settingsManager.setLanguage(SettingsManager.LANGUAGE_SPANISH)

        // Then: Language flow should emit Spanish
        val language = settingsManager.languageFlow.first()
        assertEquals(SettingsManager.LANGUAGE_SPANISH, language)
    }

    @Test
    fun setLanguage_multipleChanges_persistsLatest() = testScope.runTest {
        // When: Make multiple language changes
        settingsManager.setLanguage(SettingsManager.LANGUAGE_ENGLISH)
        settingsManager.setLanguage(SettingsManager.LANGUAGE_SPANISH)
        settingsManager.setLanguage(SettingsManager.LANGUAGE_ENGLISH)

        // Then: Should persist the latest value
        val language = settingsManager.languageFlow.first()
        assertEquals(SettingsManager.LANGUAGE_ENGLISH, language)
    }

    // View Mode Tests

    @Test
    fun isGridViewFlow_defaultsToFalse() = testScope.runTest {
        // When: Reading view mode without setting it
        val isGridView = settingsManager.isGridViewFlow.first()

        // Then: Should default to false (list view)
        assertFalse(isGridView)
    }

    @Test
    fun setViewMode_toGridView_persistsCorrectly() = testScope.runTest {
        // When: Set to grid view
        settingsManager.setViewMode(true)

        // Then: View mode flow should emit true
        val isGridView = settingsManager.isGridViewFlow.first()
        assertTrue(isGridView)
    }

    @Test
    fun setViewMode_toListView_persistsCorrectly() = testScope.runTest {
        // Given: View mode is grid
        settingsManager.setViewMode(true)

        // When: Change to list view
        settingsManager.setViewMode(false)

        // Then: View mode flow should emit false
        val isGridView = settingsManager.isGridViewFlow.first()
        assertFalse(isGridView)
    }

    @Test
    fun setViewMode_toggleMultipleTimes_persistsLatest() = testScope.runTest {
        // When: Toggle view mode multiple times
        settingsManager.setViewMode(true)
        settingsManager.setViewMode(false)
        settingsManager.setViewMode(true)

        // Then: Should persist the latest value
        val isGridView = settingsManager.isGridViewFlow.first()
        assertTrue(isGridView)
    }

    // Debug Premium Tests

    @Test
    fun isDebugPremiumFlow_defaultsToFalse() = testScope.runTest {
        // When: Reading debug premium without setting it
        val isPremium = settingsManager.isDebugPremiumFlow.first()

        // Then: Should default to false
        assertFalse(isPremium)
    }

    @Test
    fun setDebugPremium_toTrue_persistsCorrectly() = testScope.runTest {
        // When: Enable debug premium
        settingsManager.setDebugPremium(true)

        // Then: Debug premium flow should emit true
        val isPremium = settingsManager.isDebugPremiumFlow.first()
        assertTrue(isPremium)
    }

    @Test
    fun setDebugPremium_toFalse_persistsCorrectly() = testScope.runTest {
        // Given: Debug premium is enabled
        settingsManager.setDebugPremium(true)

        // When: Disable debug premium
        settingsManager.setDebugPremium(false)

        // Then: Debug premium flow should emit false
        val isPremium = settingsManager.isDebugPremiumFlow.first()
        assertFalse(isPremium)
    }

    // Theme Mode Tests

    @Test
    fun themeModeFlow_defaultsToSystem() = testScope.runTest {
        // When: Reading theme mode without setting it
        val themeMode = settingsManager.themeModeFlow.first()

        // Then: Should default to SYSTEM
        assertEquals(SettingsManager.ThemeMode.SYSTEM, themeMode)
    }

    @Test
    fun setThemeMode_toLight_persistsCorrectly() = testScope.runTest {
        // When: Set theme to light
        settingsManager.setThemeMode(SettingsManager.ThemeMode.LIGHT)

        // Then: Theme mode flow should emit LIGHT
        val themeMode = settingsManager.themeModeFlow.first()
        assertEquals(SettingsManager.ThemeMode.LIGHT, themeMode)
    }

    @Test
    fun setThemeMode_toDark_persistsCorrectly() = testScope.runTest {
        // When: Set theme to dark
        settingsManager.setThemeMode(SettingsManager.ThemeMode.DARK)

        // Then: Theme mode flow should emit DARK
        val themeMode = settingsManager.themeModeFlow.first()
        assertEquals(SettingsManager.ThemeMode.DARK, themeMode)
    }

    @Test
    fun setThemeMode_toSystem_persistsCorrectly() = testScope.runTest {
        // Given: Theme is light
        settingsManager.setThemeMode(SettingsManager.ThemeMode.LIGHT)

        // When: Change to system
        settingsManager.setThemeMode(SettingsManager.ThemeMode.SYSTEM)

        // Then: Theme mode flow should emit SYSTEM
        val themeMode = settingsManager.themeModeFlow.first()
        assertEquals(SettingsManager.ThemeMode.SYSTEM, themeMode)
    }

    @Test
    fun setThemeMode_multipleChanges_persistsLatest() = testScope.runTest {
        // When: Make multiple theme changes
        settingsManager.setThemeMode(SettingsManager.ThemeMode.LIGHT)
        settingsManager.setThemeMode(SettingsManager.ThemeMode.DARK)
        settingsManager.setThemeMode(SettingsManager.ThemeMode.SYSTEM)

        // Then: Should persist the latest value
        val themeMode = settingsManager.themeModeFlow.first()
        assertEquals(SettingsManager.ThemeMode.SYSTEM, themeMode)
    }

    // ThemeMode.fromString Tests

    @Test
    fun themeModeFromString_light_returnsLight() {
        val mode = SettingsManager.ThemeMode.fromString("LIGHT")
        assertEquals(SettingsManager.ThemeMode.LIGHT, mode)
    }

    @Test
    fun themeModeFromString_dark_returnsDark() {
        val mode = SettingsManager.ThemeMode.fromString("DARK")
        assertEquals(SettingsManager.ThemeMode.DARK, mode)
    }

    @Test
    fun themeModeFromString_system_returnsSystem() {
        val mode = SettingsManager.ThemeMode.fromString("SYSTEM")
        assertEquals(SettingsManager.ThemeMode.SYSTEM, mode)
    }

    @Test
    fun themeModeFromString_invalid_returnsSystem() {
        val mode = SettingsManager.ThemeMode.fromString("INVALID")
        assertEquals(SettingsManager.ThemeMode.SYSTEM, mode)
    }
}
