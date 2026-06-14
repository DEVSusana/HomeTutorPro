package com.devsusana.hometutorpro.presentation.settings

import com.devsusana.hometutorpro.domain.entities.AppThemeMode

/**
 * State representing settings UI options.
 */
data class SettingsState(
    val language: String = "es",
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val classEndNotificationsEnabled: Boolean = true,
    val isDebugPremium: Boolean = false,
    val isBackupLoading: Boolean = false,
    val backupMessage: String? = null,
    val isBackupSuccess: Boolean = false
)
