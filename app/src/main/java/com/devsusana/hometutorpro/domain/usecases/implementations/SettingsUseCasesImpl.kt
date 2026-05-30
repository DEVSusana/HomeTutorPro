package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.entities.AppThemeMode
import com.devsusana.hometutorpro.domain.repository.SettingsRepository
import com.devsusana.hometutorpro.domain.repository.BackupRepository
import com.devsusana.hometutorpro.domain.usecases.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Implementation of [ISetLanguageUseCase] that delegates language updates to [SettingsRepository].
 */
class SetLanguageUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ISetLanguageUseCase {
    override suspend operator fun invoke(language: String) {
        settingsRepository.setLanguage(language)
    }
}

/**
 * Implementation of [ISetThemeModeUseCase] that delegates theme mode updates to [SettingsRepository].
 */
class SetThemeModeUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ISetThemeModeUseCase {
    override suspend operator fun invoke(mode: AppThemeMode) {
        settingsRepository.setThemeMode(mode)
    }
}

/**
 * Implementation of [ISetClassEndNotificationsUseCase] that delegates notifications toggle to [SettingsRepository].
 */
class SetClassEndNotificationsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ISetClassEndNotificationsUseCase {
    override suspend operator fun invoke(enabled: Boolean) {
        settingsRepository.setClassEndNotifications(enabled)
    }
}

/**
 * Implementation of [ISetDebugPremiumUseCase] that delegates premium debug toggle to [SettingsRepository].
 */
class SetDebugPremiumUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ISetDebugPremiumUseCase {
    override suspend operator fun invoke(enabled: Boolean) {
        settingsRepository.setDebugPremium(enabled)
    }
}

/**
 * Implementation of [ICreateBackupUseCase] that delegates backup creation to [BackupRepository].
 */
class CreateBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) : ICreateBackupUseCase {
    override suspend operator fun invoke(): String {
        return backupRepository.createBackup()
    }
}

/**
 * Implementation of [IRestoreBackupUseCase] that delegates restoration to [BackupRepository].
 */
class RestoreBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) : IRestoreBackupUseCase {
    override suspend operator fun invoke(jsonContent: String): Result<Unit> {
        return backupRepository.restoreBackup(jsonContent)
    }
}

/**
 * Implementation of [IGetLanguageUseCase] that observes language from [SettingsRepository].
 */
class GetLanguageUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) : IGetLanguageUseCase {
    override fun invoke(): Flow<String> {
        return settingsRepository.languageFlow
    }
}

/**
 * Implementation of [IGetThemeModeUseCase] that observes theme mode from [SettingsRepository].
 */
class GetThemeModeUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) : IGetThemeModeUseCase {
    override fun invoke(): Flow<AppThemeMode> {
        return settingsRepository.themeModeFlow
    }
}

/**
 * Implementation of [IGetClassEndNotificationsUseCase] that observes class end notifications from [SettingsRepository].
 */
class GetClassEndNotificationsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) : IGetClassEndNotificationsUseCase {
    override fun invoke(): Flow<Boolean> {
        return settingsRepository.classEndNotificationsFlow
    }
}

/**
 * Implementation of [IGetDebugPremiumUseCase] that observes debug premium state from [SettingsRepository].
 */
class GetDebugPremiumUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) : IGetDebugPremiumUseCase {
    override fun invoke(): Flow<Boolean> {
        return settingsRepository.isDebugPremiumFlow
    }
}
