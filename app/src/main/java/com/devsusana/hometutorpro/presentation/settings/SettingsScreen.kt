package com.devsusana.hometutorpro.presentation.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.core.utils.NotificationHelper
import com.devsusana.hometutorpro.presentation.components.FeedbackDialog
import com.devsusana.hometutorpro.presentation.settings.components.ChangePasswordDialog
import com.devsusana.hometutorpro.presentation.settings.components.DeleteAccountDialog
import com.devsusana.hometutorpro.presentation.settings.components.DeleteSueModelDialog
import com.devsusana.hometutorpro.presentation.utils.LocaleHelper
import kotlinx.coroutines.launch

/**
 * Settings screen composable that manages ViewModel state, file launchers,
 * and dialog presentations, delegating visual rendering to [SettingsContent].
 */
@Composable
fun SettingsScreen(
    scrollToSue: Boolean = false,
    onLogoutClick: () -> Unit,
    onPremiumClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // Backup File Launchers
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            viewModel.exportBackup { json ->
                context.contentResolver.openOutputStream(it)?.use { output ->
                    output.write(json.toByteArray())
                }
            }
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importBackup(it) }
    }

    var showDeleteConfirmation by remember { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        DeleteAccountDialog(
            onDismiss = { showDeleteConfirmation = false },
            onConfirm = { password ->
                showDeleteConfirmation = false
                viewModel.deleteAccount(password) {
                    onLogoutClick()
                }
            }
        )
    }

    if (state.deleteAccountError != null) {
        FeedbackDialog(
            isSuccess = false,
            message = { Text(stringResource(id = state.deleteAccountError!!)) },
            onDismiss = viewModel::dismissDeleteAccountError
        )
    }

    if (state.showChangePasswordDialog) {
        ChangePasswordDialog(
            isChangingPassword = state.isChangingPassword,
            changePasswordError = state.changePasswordError,
            onDismiss = { viewModel.showChangePasswordDialog(false) },
            onSubmit = { cur, new, conf ->
                viewModel.changePassword(cur, new, conf)
            }
        )
    }

    if (state.changePasswordSuccess) {
        FeedbackDialog(
            isSuccess = true,
            message = { Text(stringResource(R.string.settings_change_password_success)) },
            onDismiss = viewModel::clearChangePasswordFeedback
        )
    }

    if (state.showDeleteModelConfirmDialog) {
        DeleteSueModelDialog(
            onDismiss = viewModel::onDismissDeleteModelDialog,
            onConfirm = viewModel::onConfirmDeleteModel
        )
    }

    SettingsContent(
        state = state,
        scrollToSue = scrollToSue,
        onEditProfileClick = onEditProfileClick,
        onChangePasswordClick = { viewModel.showChangePasswordDialog(true) },
        onExportBackup = {
            val fileName = "hometutor_backup_${System.currentTimeMillis()}.json"
            createDocumentLauncher.launch(fileName)
        },
        onImportBackup = {
            openDocumentLauncher.launch(arrayOf("application/json", "application/octet-stream"))
        },
        onClassEndNotificationsToggle = viewModel::onClassEndNotificationsToggle,
        onShowTestNotification = viewModel::showTestNotification,
        onOpenSystemPermissions = { NotificationHelper.openExactAlarmSettings(context) },
        onSueEnabledToggle = viewModel::onSueEnabledToggle,
        onSueFabVisibleToggle = viewModel::onSueFabVisibleToggle,
        onDownloadSueModel = { viewModel.downloadSueModel() },
        onCancelSueModelDownload = viewModel::cancelSueModelDownload,
        onDeleteSueModelClick = viewModel::onDeleteModelClick,
        onLanguageChange = { lang ->
            scope.launch {
                viewModel.setLanguageSync(lang)
                val activity = context as? android.app.Activity ?: return@launch
                LocaleHelper.setLocale(activity, lang)
            }
        },
        onThemeModeChange = viewModel::onThemeModeChange,
        onDebugPremiumToggle = viewModel::onDebugPremiumToggle,
        onLogoutClick = {
            viewModel.logout {
                onLogoutClick()
            }
        },
        onDeleteAccountClick = {
            showDeleteConfirmation = true
        },
        onDismissBackupMessage = viewModel::dismissBackupMessage
    )
}
