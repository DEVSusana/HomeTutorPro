package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.AppThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * Use case to update the preferred application language.
 */
interface ISetLanguageUseCase {
    /**
     * Executes the language change operation.
     *
     * @param language The locale language code (e.g., "es", "en").
     */
    suspend operator fun invoke(language: String)
}

/**
 * Use case to update the application's theme mode.
 */
interface ISetThemeModeUseCase {
    /**
     * Executes the theme mode change operation.
     *
     * @param mode The selected theme mode.
     */
    suspend operator fun invoke(mode: AppThemeMode)
}

/**
 * Use case to toggle class end notifications on or off.
 */
interface ISetClassEndNotificationsUseCase {
    /**
     * Executes the toggle operation.
     *
     * @param enabled True to enable notifications, false to disable.
     */
    suspend operator fun invoke(enabled: Boolean)
}

/**
 * Use case to toggle premium feature debugging.
 */
interface ISetDebugPremiumUseCase {
    /**
     * Executes the debug toggle operation.
     *
     * @param enabled True to enable premium features debug, false to disable.
     */
    suspend operator fun invoke(enabled: Boolean)
}

/**
 * Use case to create a serialized backup of the application's database.
 */
interface ICreateBackupUseCase {
    /**
     * Executes the backup creation.
     *
     * @return A JSON string containing the serialized application data.
     */
    suspend operator fun invoke(): String
}

/**
 * Use case to restore the application's database from a serialized backup.
 */
interface IRestoreBackupUseCase {
    /**
     * Executes the restoration process.
     *
     * @param jsonContent The JSON string containing the backup data.
     * @return A [Result] representing success or failure of the restoration.
     */
    suspend operator fun invoke(jsonContent: String): Result<Unit>
}

/**
 * Use case to observe the current application language setting.
 */
interface IGetLanguageUseCase {
    /**
     * Observes the language stream.
     *
     * @return Flow emitting language codes.
     */
    operator fun invoke(): Flow<String>
}

/**
 * Use case to observe the current application theme mode setting.
 */
interface IGetThemeModeUseCase {
    /**
     * Observes the theme mode stream.
     *
     * @return Flow emitting [AppThemeMode] updates.
     */
    operator fun invoke(): Flow<AppThemeMode>
}

/**
 * Use case to observe the class end notifications status setting.
 */
interface IGetClassEndNotificationsUseCase {
    /**
     * Observes the notifications enabled stream.
     *
     * @return Flow emitting boolean enabled states.
     */
    operator fun invoke(): Flow<Boolean>
}

/**
 * Use case to observe the debug premium status setting.
 */
interface IGetDebugPremiumUseCase {
    /**
     * Observes the debug premium stream.
     *
     * @return Flow emitting boolean enabled states.
     */
    operator fun invoke(): Flow<Boolean>
}
