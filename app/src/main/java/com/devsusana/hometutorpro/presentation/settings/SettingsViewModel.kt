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
import kotlinx.coroutines.flow.update
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
    private val logoutUseCase: com.devsusana.hometutorpro.domain.usecases.ILogoutUseCase,
    private val deleteAccountUseCase: com.devsusana.hometutorpro.domain.usecases.IDeleteAccountUseCase,
    private val updatePasswordUseCase: IUpdatePasswordUseCase,
    private val application: Application
) : ViewModel() {

    private val _backupState = MutableStateFlow(Pair<Boolean, String?>(false, null))
    private val _deleteAccountError = MutableStateFlow<Int?>(null)

    data class ChangePasswordStatus(
        val showDialog: Boolean = false,
        val isLoading: Boolean = false,
        val success: Boolean = false,
        val error: Int? = null
    )
    private val _changePasswordStatus = MutableStateFlow(ChangePasswordStatus())

    /** Screen state combining language, theme, notification status, and backup logs. */
    val state: StateFlow<SettingsState> = combine(
        getLanguageUseCase(),
        getThemeModeUseCase(),
        getClassEndNotificationsUseCase(),
        getDebugPremiumUseCase(),
        _backupState,
        _deleteAccountError,
        _changePasswordStatus
    ) { array ->
        val language = array[0] as String
        val themeMode = array[1] as AppThemeMode
        val classEndNotifications = array[2] as Boolean
        val isDebugPremium = array[3] as Boolean
        @Suppress("UNCHECKED_CAST")
        val backupInfo = array[4] as Pair<Boolean, String?>
        val deleteError = array[5] as? Int
        val pwdStatus = array[6] as ChangePasswordStatus
        SettingsState(
            language = language,
            themeMode = themeMode,
            classEndNotificationsEnabled = classEndNotifications,
            isDebugPremium = isDebugPremium,
            isBackupLoading = backupInfo.first,
            backupMessage = backupInfo.second,
            isBackupSuccess = backupInfo.second != null && !backupInfo.first,
            deleteAccountError = deleteError,
            showChangePasswordDialog = pwdStatus.showDialog,
            isChangingPassword = pwdStatus.isLoading,
            changePasswordSuccess = pwdStatus.success,
            changePasswordError = pwdStatus.error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsState()
    )

    /** Handles manual language switching. */
    fun showChangePasswordDialog(show: Boolean) {
        _changePasswordStatus.update { it.copy(showDialog = show, error = null, success = false) }
    }

    fun changePassword(currentPassword: String, newPassword: String, confirm: String) {
        if (currentPassword.isBlank()) {
            _changePasswordStatus.update { 
                it.copy(error = R.string.settings_change_password_error_current_blank) 
            }
            return
        }
        if (newPassword != confirm) {
            _changePasswordStatus.update { 
                it.copy(error = R.string.settings_change_password_error_mismatch) 
            }
            return
        }
        if (newPassword.length < 6) {
            _changePasswordStatus.update { 
                it.copy(error = R.string.settings_change_password_error_short) 
            }
            return
        }

        viewModelScope.launch {
            _changePasswordStatus.update { it.copy(isLoading = true, error = null) }
            when (val result = updatePasswordUseCase(currentPassword, newPassword)) {
                is com.devsusana.hometutorpro.domain.core.Result.Success -> {
                    _changePasswordStatus.update { 
                        it.copy(
                            isLoading = false, 
                            showDialog = false, 
                            success = true 
                        ) 
                    }
                }
                is com.devsusana.hometutorpro.domain.core.Result.Error -> {
                    val errorRes = when (result.error) {
                        is com.devsusana.hometutorpro.domain.core.DomainError.InvalidCredentials -> 
                            R.string.settings_change_password_error_incorrect_current
                        else -> 
                            R.string.settings_change_password_error_generic
                    }
                    _changePasswordStatus.update { 
                        it.copy(
                            isLoading = false, 
                            error = errorRes 
                        ) 
                    }
                }
            }
        }
    }

    fun clearChangePasswordFeedback() {
        _changePasswordStatus.update { it.copy(success = false, error = null) }
    }
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

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            logoutUseCase()
            onComplete()
        }
    }

    fun deleteAccount(password: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            _deleteAccountError.value = null
            when (val result = deleteAccountUseCase(password)) {
                is com.devsusana.hometutorpro.domain.core.Result.Success -> {
                    onComplete()
                }
                is com.devsusana.hometutorpro.domain.core.Result.Error -> {
                    val messageRes = when (result.error) {
                        is com.devsusana.hometutorpro.domain.core.DomainError.InvalidCredentials ->
                            R.string.login_error_invalid_credentials
                        is com.devsusana.hometutorpro.domain.core.DomainError.RecentLoginRequired ->
                            R.string.settings_delete_account_error_recent_login
                        is com.devsusana.hometutorpro.domain.core.DomainError.NetworkError ->
                            R.string.settings_delete_account_error_network
                        else ->
                            R.string.settings_delete_account_error_unknown
                    }
                    _deleteAccountError.value = messageRes
                }
            }
        }
    }

    fun dismissDeleteAccountError() {
        _deleteAccountError.value = null
    }
}
