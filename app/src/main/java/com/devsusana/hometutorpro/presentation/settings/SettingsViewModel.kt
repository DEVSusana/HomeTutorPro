package com.devsusana.hometutorpro.presentation.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devsusana.hometutorpro.core.utils.NotificationHelper
import com.devsusana.hometutorpro.core.utils.IUriReader
import android.net.Uri
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.domain.entities.AppThemeMode
import com.devsusana.hometutorpro.domain.usecases.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing the application's configuration screen (language, theme, notifications, backups).
 *
 * Adheres strictly to Clean Architecture by utilizing pure business logic Use Cases.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getLanguageUseCase: IGetLanguageUseCase,
    private val getThemeModeUseCase: IGetThemeModeUseCase,
    private val getClassEndNotificationsUseCase: IGetClassEndNotificationsUseCase,
    private val getDebugPremiumUseCase: IGetDebugPremiumUseCase,
    private val setLanguageUseCase: ISetLanguageUseCase,
    private val setThemeModeUseCase: ISetThemeModeUseCase,
    private val setClassEndNotificationsUseCase: ISetClassEndNotificationsUseCase,
    private val setDebugPremiumUseCase: ISetDebugPremiumUseCase,
    private val createBackupUseCase: ICreateBackupUseCase,
    private val restoreBackupUseCase: IRestoreBackupUseCase,
    private val showTestNotificationUseCase: IShowTestNotificationUseCase,
    private val uriReader: IUriReader,
    private val application: Application
) : ViewModel() {

    private val _backupState = MutableStateFlow(Pair<Boolean, String?>(false, null))

    /** Screen state combining language, theme, notification status, and backup logs. */
    val state: StateFlow<SettingsState> = combine(
        getLanguageUseCase(),
        getThemeModeUseCase(),
        getClassEndNotificationsUseCase(),
        getDebugPremiumUseCase(),
        _backupState
    ) { language, themeMode, classEndNotifications, isDebugPremium, backupInfo ->
        SettingsState(
            language = language,
            themeMode = themeMode,
            classEndNotificationsEnabled = classEndNotifications,
            isDebugPremium = isDebugPremium,
            isBackupLoading = backupInfo.first,
            backupMessage = backupInfo.second,
            isBackupSuccess = backupInfo.second != null && !backupInfo.first
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsState()
    )

    /** Handles manual language switching. */
    fun onLanguageChange(language: String) {
        viewModelScope.launch {
            setLanguageUseCase(language)
        }
    }

    /** Synchronously changes the language, typically for setup stages. */
    suspend fun setLanguageSync(language: String) {
        setLanguageUseCase(language)
    }

    /** Handles theme configuration changes. */
    fun onThemeModeChange(mode: AppThemeMode) {
        viewModelScope.launch {
            setThemeModeUseCase(mode)
        }
    }

    /** Toggles the preference for triggering notifications at class endings. */
    fun onClassEndNotificationsToggle(enabled: Boolean) {
        viewModelScope.launch {
            setClassEndNotificationsUseCase(enabled)
        }
    }

    /** Toggles the debug premium mode. */
    fun onDebugPremiumToggle(enabled: Boolean) {
        viewModelScope.launch {
            setDebugPremiumUseCase(enabled)
        }
    }

    /** Triggers a placeholder test notification. */
    fun showTestNotification() {
        showTestNotificationUseCase()
    }

    /** Exports the local database backup in JSON string format. */
    fun exportBackup(onResult: (String) -> Unit) {
        viewModelScope.launch {
            _backupState.value = true to null
            try {
                val json = createBackupUseCase()
                onResult(json)
                _backupState.value = false to application.getString(R.string.settings_backup_ready)
            } catch (e: Exception) {
                _backupState.value = false to application.getString(
                    R.string.settings_backup_error,
                    e.message ?: ""
                )
            }
        }
    }

    /** Imports a JSON backup file and replaces the local database entries. */
    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            _backupState.value = true to null
            try {
                val jsonContent = uriReader.readTextFromUri(application, uri)
                val result = restoreBackupUseCase(jsonContent)
                if (result.isSuccess) {
                    _backupState.value = false to application.getString(R.string.settings_restore_success)
                } else {
                    _backupState.value = false to application.getString(
                        R.string.settings_restore_error,
                        result.exceptionOrNull()?.message ?: ""
                    )
                }
            } catch (e: Exception) {
                _backupState.value = false to application.getString(
                    R.string.settings_restore_error,
                    e.message ?: ""
                )
            }
        }
    }

    /** Clears the backup/restoration overlay message. */
    fun dismissBackupMessage() {
        _backupState.value = false to null
    }
}
