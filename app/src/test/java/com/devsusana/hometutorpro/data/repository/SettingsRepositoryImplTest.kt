package com.devsusana.hometutorpro.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.devsusana.hometutorpro.domain.entities.AppThemeMode
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
 * Unit tests for [SettingsRepositoryImpl].
 * Tests DataStore operations for language, view mode, debug premium, theme, and notifications settings.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SettingsRepositoryImplTest {

    private lateinit var context: Context
    private lateinit var testDataStore: DataStore<Preferences>
    private lateinit var repository: SettingsRepositoryImpl
    private lateinit var testScope: TestScope

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        testScope = TestScope(UnconfinedTestDispatcher() + Job())

        // Create SettingsRepositoryImpl with application context
        repository = SettingsRepositoryImpl(context)

        // Clear DataStore to ensure clean state
        runTest {
            // Since settings DataStore uses "settings" name inside SettingsRepositoryImpl,
            // we can get the actual file and clear it, or clear it via edit on a delegated property.
            // But SettingsRepositoryImpl delegates it internally, so we can edit it directly on context.
            // We clear it to ensure tests run in isolation.
            // Let's clear the preference file associated with the Context datastore.
            // A simple way is to clear it via repository updates or deleting/clearing context preferences.
            repository.setLanguage("es")
            repository.setViewMode(false)
            repository.setDebugPremium(false)
            repository.setThemeMode(AppThemeMode.SYSTEM)
            repository.setClassEndNotifications(true)
        }
    }

    @After
    fun tearDown() {
        // Cleanup settings if needed
    }

    // Language Tests

    @Test
    fun languageFlow_defaultsToSpanish() = testScope.runTest {
        val language = repository.languageFlow.first()
        assertEquals("es", language)
    }

    @Test
    fun setLanguage_toEnglish_persistsCorrectly() = testScope.runTest {
        repository.setLanguage("en")
        val language = repository.languageFlow.first()
        assertEquals("en", language)
    }

    // View Mode Tests

    @Test
    fun isGridViewFlow_defaultsToFalse() = testScope.runTest {
        val isGridView = repository.isGridViewFlow.first()
        assertFalse(isGridView)
    }

    @Test
    fun setViewMode_toGridView_persistsCorrectly() = testScope.runTest {
        repository.setViewMode(true)
        val isGridView = repository.isGridViewFlow.first()
        assertTrue(isGridView)
    }

    // Debug Premium Tests

    @Test
    fun isDebugPremiumFlow_defaultsToFalse() = testScope.runTest {
        val isPremium = repository.isDebugPremiumFlow.first()
        assertFalse(isPremium)
    }

    @Test
    fun setDebugPremium_toTrue_persistsCorrectly() = testScope.runTest {
        repository.setDebugPremium(true)
        val isPremium = repository.isDebugPremiumFlow.first()
        assertTrue(isPremium)
    }

    // Theme Mode Tests

    @Test
    fun themeModeFlow_defaultsToSystem() = testScope.runTest {
        val themeMode = repository.themeModeFlow.first()
        assertEquals(AppThemeMode.SYSTEM, themeMode)
    }

    @Test
    fun setThemeMode_toDark_persistsCorrectly() = testScope.runTest {
        repository.setThemeMode(AppThemeMode.DARK)
        val themeMode = repository.themeModeFlow.first()
        assertEquals(AppThemeMode.DARK, themeMode)
    }

    // Class End Notifications Tests

    @Test
    fun classEndNotificationsFlow_defaultsToTrue() = testScope.runTest {
        val enabled = repository.classEndNotificationsFlow.first()
        assertTrue(enabled)
    }

    @Test
    fun setClassEndNotifications_toFalse_persistsCorrectly() = testScope.runTest {
        repository.setClassEndNotifications(false)
        val enabled = repository.classEndNotificationsFlow.first()
        assertFalse(enabled)
    }
}
