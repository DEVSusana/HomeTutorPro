package com.devsusana.hometutorpro.presentation.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devsusana.hometutorpro.core.settings.SettingsManager
import com.devsusana.hometutorpro.core.utils.NotificationHelper
import com.devsusana.hometutorpro.core.utils.BackupManager
import android.net.Uri
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.domain.usecases.ISetLanguageUseCase
import com.devsusana.hometutorpro.domain.usecases.ISetThemeModeUseCase
import com.devsusana.hometutorpro.domain.usecases.ISetClassEndNotificationsUseCase
import com.devsusana.hometutorpro.domain.usecases.ISetDebugPremiumUseCase
import com.devsusana.hometutorpro.domain.usecases.ICreateBackupUseCase
import com.devsusana.hometutorpro.domain.usecases.IRestoreBackupUseCase
import com.devsusana.hometutorpro.domain.usecases.IShowTestNotificationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject



@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager, // Still needed for flows for now, but actions should use UseCases
    private val setLanguageUseCase: ISetLanguageUseCase,
    private val setThemeModeUseCase: ISetThemeModeUseCase,
    private val setClassEndNotificationsUseCase: ISetClassEndNotificationsUseCase,
    private val setDebugPremiumUseCase: ISetDebugPremiumUseCase,
    private val createBackupUseCase: ICreateBackupUseCase,
    private val restoreBackupUseCase: IRestoreBackupUseCase,
    private val showTestNotificationUseCase: IShowTestNotificationUseCase,
    private val application: Application
) : ViewModel() {

    private val _backupState = MutableStateFlow(Pair<Boolean, String?>(false, null))

    val state: StateFlow<SettingsState> = combine(
        settingsManager.languageFlow,
        settingsManager.themeModeFlow,
        settingsManager.classEndNotificationsFlow,
        settingsManager.isDebugPremiumFlow,
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

    fun onLanguageChange(language: String) {
        viewModelScope.launch {
            setLanguageUseCase(language)
        }
    }

    suspend fun setLanguageSync(language: String) {
        setLanguageUseCase(language)
    }

    fun onThemeModeChange(mode: SettingsManager.ThemeMode) {
        viewModelScope.launch {
            setThemeModeUseCase(mode)
        }
    }

    fun onClassEndNotificationsToggle(enabled: Boolean) {
        viewModelScope.launch {
            setClassEndNotificationsUseCase(enabled)
        }
    }

    fun onDebugPremiumToggle(enabled: Boolean) {
        viewModelScope.launch {
            setDebugPremiumUseCase(enabled)
        }
    }

    fun showTestNotification() {
        showTestNotificationUseCase()
    }

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

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            _backupState.value = true to null
            val result = restoreBackupUseCase(uri)
            if (result.isSuccess) {
                _backupState.value = false to application.getString(R.string.settings_restore_success)
            } else {
                _backupState.value = false to application.getString(
                    R.string.settings_restore_error,
                    result.exceptionOrNull()?.message ?: ""
                )
            }
        }
    }

    fun dismissBackupMessage() {
        _backupState.value = false to null
    }
}
