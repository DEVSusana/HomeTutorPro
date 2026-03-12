package com.devsusana.hometutorpro.presentation.settings

import com.devsusana.hometutorpro.core.settings.SettingsManager

data class SettingsState(
    val language: String = "es",
    val themeMode: SettingsManager.ThemeMode = SettingsManager.ThemeMode.SYSTEM,
    val classEndNotificationsEnabled: Boolean = true,
    val isDebugPremium: Boolean = false,
    val isBackupLoading: Boolean = false,
    val backupMessage: String? = null,
    val isBackupSuccess: Boolean = false
)
