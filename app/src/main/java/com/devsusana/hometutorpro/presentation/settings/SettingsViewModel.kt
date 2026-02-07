package com.devsusana.hometutorpro.presentation.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devsusana.hometutorpro.core.settings.SettingsManager
import com.devsusana.hometutorpro.core.utils.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val language: String = "es",
    val themeMode: SettingsManager.ThemeMode = SettingsManager.ThemeMode.SYSTEM,
    val classEndNotificationsEnabled: Boolean = true,
    val isDebugPremium: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager,
    private val application: Application
) : ViewModel() {

    val state: StateFlow<SettingsState> = combine(
        settingsManager.languageFlow,
        settingsManager.themeModeFlow,
        settingsManager.classEndNotificationsFlow,
        settingsManager.isDebugPremiumFlow
    ) { language, themeMode, classEndNotifications, isDebugPremium ->
        SettingsState(
            language = language,
            themeMode = themeMode,
            classEndNotificationsEnabled = classEndNotifications,
            isDebugPremium = isDebugPremium
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
}
