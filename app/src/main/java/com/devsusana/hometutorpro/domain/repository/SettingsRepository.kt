package com.devsusana.hometutorpro.domain.repository

import com.devsusana.hometutorpro.domain.entities.AppThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for accessing and modifying application settings.
 *
 * Implements Clean Architecture by decoupling business logic from preference storage.
 */
interface SettingsRepository {
    /** Emits the current selected language code (e.g., "en", "es"). */
    val languageFlow: Flow<String>

    /** Emits whether grid view is enabled in lists. */
    val isGridViewFlow: Flow<Boolean>

    /** Emits whether premium debugging features are enabled. */
    val isDebugPremiumFlow: Flow<Boolean>

    /** Emits the current theme mode setting. */
    val themeModeFlow: Flow<AppThemeMode>

    /** Emits whether notifications at the end of classes are enabled. */
    val classEndNotificationsFlow: Flow<Boolean>

    /**
     * Updates the selected language.
     *
     * @param language The language code (e.g., "en", "es").
     */
    suspend fun setLanguage(language: String)

    /**
     * Updates the grid view preference.
     *
     * @param isGridView True to enable grid view, false for list view.
     */
    suspend fun setViewMode(isGridView: Boolean)

    /**
     * Toggles the debug premium preference.
     *
     * @param isPremium True to enable premium features, false to disable.
     */
    suspend fun setDebugPremium(isPremium: Boolean)

    /**
     * Updates the preferred theme mode.
     *
     * @param mode The selected theme mode.
     */
    suspend fun setThemeMode(mode: AppThemeMode)

    /**
     * Toggles the class end notifications preference.
     *
     * @param enabled True to enable notifications, false to disable.
     */
    suspend fun setClassEndNotifications(enabled: Boolean)
}
