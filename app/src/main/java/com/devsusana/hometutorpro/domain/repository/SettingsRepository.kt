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

    /** Emits whether onboarding is completed. */
    val isOnboardingCompletedFlow: Flow<Boolean>

    /** Emits whether Sue AI assistant is enabled. */
    val isSueEnabledFlow: Flow<Boolean>

    /** Emits whether the Sue floating action button (FAB) is visible. */
    val isSueFabVisibleFlow: Flow<Boolean>

    /** Emits whether Sue onboarding has been completed. */
    val isSueOnboardingCompletedFlow: Flow<Boolean>

    /**
     * Updates the onboarding completed status.
     *
     * @param completed True if onboarding is completed.
     */
    suspend fun setOnboardingCompleted(completed: Boolean)

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

    /**
     * Toggles the Sue AI assistant enabled preference.
     *
     * @param enabled True to enable Sue, false to disable.
     */
    suspend fun setSueEnabled(enabled: Boolean)

    /**
     * Toggles the Sue FAB visibility preference.
     *
     * @param visible True to show the FAB, false to hide.
     */
    suspend fun setSueFabVisible(visible: Boolean)

    /**
     * Updates the Sue onboarding completed status.
     *
     * @param completed True if Sue onboarding is completed.
     */
    suspend fun setSueOnboardingCompleted(completed: Boolean)
}

