package com.devsusana.hometutorpro.presentation.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devsusana.hometutorpro.BuildConfig
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.domain.entities.AppThemeMode
import com.devsusana.hometutorpro.presentation.components.FeedbackDialog
import com.devsusana.hometutorpro.presentation.settings.components.LanguageSelectionDialog
import com.devsusana.hometutorpro.presentation.settings.components.SettingsAccountSection
import com.devsusana.hometutorpro.presentation.settings.components.SettingsBackupSection
import com.devsusana.hometutorpro.presentation.settings.components.SettingsDangerZoneSection
import com.devsusana.hometutorpro.presentation.settings.components.SettingsGeneralSection
import com.devsusana.hometutorpro.presentation.settings.components.SettingsLegalSection
import com.devsusana.hometutorpro.presentation.settings.components.SettingsNotificationsSection
import com.devsusana.hometutorpro.presentation.settings.components.SettingsSueSection
import com.devsusana.hometutorpro.presentation.settings.components.SueHelpDialog
import com.devsusana.hometutorpro.presentation.settings.components.ThemeSelectionDialog
import com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme
import kotlinx.coroutines.delay

/**
 * Main Settings presentation layout orchestrating modular setting cards and dialogs.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SettingsContent(
    state: SettingsState,
    scrollToSue: Boolean = false,
    onEditProfileClick: () -> Unit,
    onChangePasswordClick: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onClassEndNotificationsToggle: (Boolean) -> Unit,
    onShowTestNotification: () -> Unit,
    onOpenSystemPermissions: () -> Unit,
    onSueEnabledToggle: (Boolean) -> Unit,
    onSueFabVisibleToggle: (Boolean) -> Unit,
    onDownloadSueModel: () -> Unit,
    onCancelSueModelDownload: () -> Unit,
    onDeleteSueModelClick: () -> Unit,
    onLanguageChange: (String) -> Unit,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onDebugPremiumToggle: (Boolean) -> Unit,
    onLogoutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit,
    onDismissBackupMessage: () -> Unit
) {
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showSueHelpDialog by remember { mutableStateOf(false) }

    val sueBringIntoViewRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(scrollToSue) {
        if (scrollToSue) {
            delay(300)
            sueBringIntoViewRequester.bringIntoView()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsAccountSection(
                onEditProfileClick = onEditProfileClick,
                onChangePasswordClick = onChangePasswordClick
            )

            SettingsBackupSection(
                isBackupLoading = state.isBackupLoading,
                onExportBackup = onExportBackup,
                onImportBackup = onImportBackup
            )

            SettingsNotificationsSection(
                classEndNotificationsEnabled = state.classEndNotificationsEnabled,
                onClassEndNotificationsToggle = onClassEndNotificationsToggle,
                onOpenSystemPermissions = onOpenSystemPermissions,
                onShowTestNotification = onShowTestNotification
            )

            SettingsGeneralSection(
                language = state.language,
                themeMode = state.themeMode,
                onLanguageClick = { showLanguageDialog = true },
                onThemeClick = { showThemeDialog = true }
            )

            SettingsSueSection(
                isSueEnabled = state.isSueEnabled,
                isSueFabVisible = state.isSueFabVisible,
                sueDeviceCompatibility = state.sueDeviceCompatibility,
                sueModelStatus = state.sueModelStatus,
                bringIntoViewRequester = sueBringIntoViewRequester,
                onSueEnabledToggle = onSueEnabledToggle,
                onSueFabVisibleToggle = onSueFabVisibleToggle,
                onDownloadSueModel = onDownloadSueModel,
                onCancelSueModelDownload = onCancelSueModelDownload,
                onDeleteSueModelClick = onDeleteSueModelClick,
                onShowSueHelpDialog = { showSueHelpDialog = true }
            )

            SettingsLegalSection()

            Spacer(modifier = Modifier.height(24.dp))

            SettingsDangerZoneSection(
                onLogoutClick = onLogoutClick,
                onDeleteAccountClick = onDeleteAccountClick
            )

            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            onLanguageChange = onLanguageChange,
            onDismiss = { showLanguageDialog = false }
        )
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentThemeMode = state.themeMode,
            onThemeModeChange = onThemeModeChange,
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showSueHelpDialog) {
        SueHelpDialog(
            onDismiss = { showSueHelpDialog = false }
        )
    }

    state.backupMessage?.let { message ->
        FeedbackDialog(
            isSuccess = state.isBackupSuccess,
            message = { Text(message) },
            onDismiss = onDismissBackupMessage
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsContentPreview() {
    HomeTutorProTheme {
        SettingsContent(
            state = SettingsState(
                language = "en",
                themeMode = AppThemeMode.SYSTEM
            ),
            onEditProfileClick = {},
            onChangePasswordClick = {},
            onExportBackup = {},
            onImportBackup = {},
            onClassEndNotificationsToggle = {},
            onShowTestNotification = {},
            onOpenSystemPermissions = {},
            onSueEnabledToggle = {},
            onSueFabVisibleToggle = {},
            onDownloadSueModel = {},
            onCancelSueModelDownload = {},
            onDeleteSueModelClick = {},
            onLanguageChange = {},
            onThemeModeChange = {},
            onDebugPremiumToggle = {},
            onLogoutClick = {},
            onDeleteAccountClick = {},
            onDismissBackupMessage = {}
        )
    }
}
