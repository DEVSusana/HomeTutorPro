package com.devsusana.hometutorpro.domain.usecases.implementations

import android.net.Uri
import com.devsusana.hometutorpro.core.settings.SettingsManager
import com.devsusana.hometutorpro.core.utils.BackupManager
import com.devsusana.hometutorpro.domain.usecases.*
import javax.inject.Inject

class SetLanguageUseCase @Inject constructor(
    private val settingsManager: SettingsManager
) : ISetLanguageUseCase {
    override suspend operator fun invoke(language: String) {
        settingsManager.setLanguage(language)
    }
}

class SetThemeModeUseCase @Inject constructor(
    private val settingsManager: SettingsManager
) : ISetThemeModeUseCase {
    override suspend operator fun invoke(mode: SettingsManager.ThemeMode) {
        settingsManager.setThemeMode(mode)
    }
}

class SetClassEndNotificationsUseCase @Inject constructor(
    private val settingsManager: SettingsManager
) : ISetClassEndNotificationsUseCase {
    override suspend operator fun invoke(enabled: Boolean) {
        settingsManager.setClassEndNotifications(enabled)
    }
}

class SetDebugPremiumUseCase @Inject constructor(
    private val settingsManager: SettingsManager
) : ISetDebugPremiumUseCase {
    override suspend operator fun invoke(enabled: Boolean) {
        settingsManager.setDebugPremium(enabled)
    }
}

class CreateBackupUseCase @Inject constructor(
    private val backupManager: BackupManager
) : ICreateBackupUseCase {
    override suspend operator fun invoke(): String {
        return backupManager.createBackup()
    }
}

class RestoreBackupUseCase @Inject constructor(
    private val backupManager: BackupManager,
    private val application: android.app.Application
) : IRestoreBackupUseCase {
    override suspend operator fun invoke(uri: Uri): Result<Unit> {
        return backupManager.restoreBackup(application, uri)
    }
}
