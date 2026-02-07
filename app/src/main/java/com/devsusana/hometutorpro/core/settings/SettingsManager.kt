package com.devsusana.hometutorpro.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsManager @Inject constructor(
    private val context: Context
) {
    companion object {
        val LANGUAGE_KEY = stringPreferencesKey("language")
        val VIEW_MODE_KEY = booleanPreferencesKey("is_grid_view")
        val DEBUG_PREMIUM_KEY = booleanPreferencesKey("debug_premium")
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        val CLASS_END_NOTIFICATIONS_KEY = booleanPreferencesKey("class_end_notifications")
        
        const val LANGUAGE_ENGLISH = "en"
        const val LANGUAGE_SPANISH = "es"
    }

    enum class ThemeMode {
        LIGHT,
        DARK,
        SYSTEM;

        companion object {
            fun fromString(value: String): ThemeMode {
                return when (value) {
                    "LIGHT" -> LIGHT
                    "DARK" -> DARK
                    else -> SYSTEM
                }
            }
        }
    }

    val languageFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[LANGUAGE_KEY] ?: LANGUAGE_SPANISH
    }

    val isGridViewFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[VIEW_MODE_KEY] ?: false
    }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language
        }
    }

    suspend fun setViewMode(isGridView: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[VIEW_MODE_KEY] = isGridView
        }
    }

    val isDebugPremiumFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DEBUG_PREMIUM_KEY] ?: false
    }

    suspend fun setDebugPremium(isPremium: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DEBUG_PREMIUM_KEY] = isPremium
        }
    }

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        val value = preferences[THEME_MODE_KEY] ?: "SYSTEM"
        ThemeMode.fromString(value)
    }

        suspend fun setThemeMode(mode: ThemeMode) {

            context.dataStore.edit { preferences ->

                preferences[THEME_MODE_KEY] = mode.name

            }

        }

    

        val classEndNotificationsFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->

            preferences[CLASS_END_NOTIFICATIONS_KEY] ?: true

        }

    

        suspend fun setClassEndNotifications(enabled: Boolean) {

            context.dataStore.edit { preferences ->

                preferences[CLASS_END_NOTIFICATIONS_KEY] = enabled

            }

        }

    }

    