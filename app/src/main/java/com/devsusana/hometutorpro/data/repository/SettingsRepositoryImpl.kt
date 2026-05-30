package com.devsusana.hometutorpro.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.devsusana.hometutorpro.domain.entities.AppThemeMode
import com.devsusana.hometutorpro.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Android implementation of [SettingsRepository] using Jetpack DataStore Preferences.
 *
 * @param context The application context.
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    companion object {
        private val LANGUAGE_KEY = stringPreferencesKey("language")
        private val VIEW_MODE_KEY = booleanPreferencesKey("is_grid_view")
        private val DEBUG_PREMIUM_KEY = booleanPreferencesKey("debug_premium")
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        private val CLASS_END_NOTIFICATIONS_KEY = booleanPreferencesKey("class_end_notifications")

        private const val LANGUAGE_ENGLISH = "en"
        private const val LANGUAGE_SPANISH = "es"
    }

    override val languageFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[LANGUAGE_KEY] ?: LANGUAGE_SPANISH
    }

    override val isGridViewFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[VIEW_MODE_KEY] ?: false
    }

    override val isDebugPremiumFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DEBUG_PREMIUM_KEY] ?: false
    }

    override val themeModeFlow: Flow<AppThemeMode> = context.dataStore.data.map { preferences ->
        val value = preferences[THEME_MODE_KEY] ?: "SYSTEM"
        AppThemeMode.fromString(value)
    }

    override val classEndNotificationsFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[CLASS_END_NOTIFICATIONS_KEY] ?: true
    }

    override suspend fun setLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language
        }
    }

    override suspend fun setViewMode(isGridView: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[VIEW_MODE_KEY] = isGridView
        }
    }

    override suspend fun setDebugPremium(isPremium: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DEBUG_PREMIUM_KEY] = isPremium
        }
    }

    override suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode.name
        }
    }

    override suspend fun setClassEndNotifications(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CLASS_END_NOTIFICATIONS_KEY] = enabled
        }
    }
}
