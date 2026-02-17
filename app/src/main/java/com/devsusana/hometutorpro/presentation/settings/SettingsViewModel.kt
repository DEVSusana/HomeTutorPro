package com.devsusana.hometutorpro.presentation.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devsusana.hometutorpro.core.settings.SettingsManager
import com.devsusana.hometutorpro.core.utils.NotificationHelper
import com.devsusana.hometutorpro.core.utils.BackupManager
import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val language: String = "es",
    val themeMode: SettingsManager.ThemeMode = SettingsManager.ThemeMode.SYSTEM,
    val classEndNotificationsEnabled: Boolean = true,
    val isDebugPremium: Boolean = false,
    val isBackupLoading: Boolean = false,
    val backupMessage: String? = null,
    val isBackupSuccess: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager,
    private val backupManager: BackupManager,
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
            settingsManager.setLanguage(language)
        }
    }

    fun onThemeModeChange(mode: SettingsManager.ThemeMode) {
        viewModelScope.launch {
            settingsManager.setThemeMode(mode)
        }
    }

    fun onClassEndNotificationsToggle(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setClassEndNotifications(enabled)
        }
    }

    fun onDebugPremiumToggle(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setDebugPremium(enabled)
        }
    }

    fun showTestNotification() {
        NotificationHelper.showTestNotification(application)
    }

    fun exportBackup(onResult: (String) -> Unit) {
        viewModelScope.launch {
            _backupState.value = true to null
            try {
                val json = backupManager.createBackup()
                onResult(json)
                _backupState.value = false to "Backup listo para guardar"
            } catch (e: Exception) {
                _backupState.value = false to "Error al crear backup: ${e.message}"
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            _backupState.value = true to null
            val result = backupManager.restoreBackup(application, uri)
            if (result.isSuccess) {
                _backupState.value = false to "Datos restaurados con éxito"
            } else {
                _backupState.value = false to "Error al restaurar: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun dismissBackupMessage() {
        _backupState.value = false to null
    }
}
