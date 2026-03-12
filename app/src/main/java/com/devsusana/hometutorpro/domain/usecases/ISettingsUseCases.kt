package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.core.settings.SettingsManager
import kotlinx.coroutines.flow.Flow

interface ISetLanguageUseCase {
    suspend operator fun invoke(language: String)
}

interface ISetThemeModeUseCase {
    suspend operator fun invoke(mode: SettingsManager.ThemeMode)
}

interface ISetClassEndNotificationsUseCase {
    suspend operator fun invoke(enabled: Boolean)
}

interface ISetDebugPremiumUseCase {
    suspend operator fun invoke(enabled: Boolean)
}

interface ICreateBackupUseCase {
    suspend operator fun invoke(): String
}

interface IRestoreBackupUseCase {
    suspend operator fun invoke(uri: android.net.Uri): Result<Unit>
}
