package com.devsusana.hometutorpro.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.devsusana.hometutorpro.BuildConfig
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.core.settings.SettingsManager
import com.devsusana.hometutorpro.presentation.settings.components.SettingsItem
import com.devsusana.hometutorpro.presentation.settings.components.SettingsSectionTitle
import com.devsusana.hometutorpro.presentation.utils.LocaleHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onLogoutClick: () -> Unit,
    onPremiumClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    
    // Premium state placeholder
    val isPremium = false 
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Account Section
            SettingsSectionTitle(stringResource(R.string.settings_account))
            
            // Edit Profile
            SettingsItem(
                icon = Icons.Default.Person,
                title = stringResource(R.string.settings_edit_profile),
                onClick = onEditProfileClick
            )
            
            HorizontalDivider()
            
            // Notifications Section
            SettingsSectionTitle(stringResource(R.string.settings_notifications))
            
            SettingsItem(
                icon = Icons.Default.Notifications,
                title = stringResource(R.string.settings_class_end_notifications),
                subtitle = stringResource(R.string.settings_class_end_notifications_desc),
                onClick = { viewModel.onClassEndNotificationsToggle(!state.classEndNotificationsEnabled) },
                trailing = {
                    Switch(
                        checked = state.classEndNotificationsEnabled,
                        onCheckedChange = { viewModel.onClassEndNotificationsToggle(it) }
                    )
                }
            )

            // Test Notification Button
            TextButton(
                onClick = { viewModel.showTestNotification() },
                modifier = Modifier.padding(start = 56.dp)
            ) {
                Text(stringResource(R.string.settings_test_alarm))
            }

            HorizontalDivider()
            
            // Preferences Section
            SettingsSectionTitle(stringResource(R.string.settings_theme_dialog_title))
            
            // Language
            SettingsItem(
                icon = Icons.Default.Language,
                title = stringResource(R.string.settings_language),
                subtitle = if (state.language == "es") "Español" else "English",
                onClick = { showLanguageDialog = true }
            )
            
            HorizontalDivider()
            
            // Theme Mode
            SettingsItem(
                icon = Icons.Default.Palette,
                title = stringResource(R.string.settings_theme),
                subtitle = when (state.themeMode) {
                    SettingsManager.ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                    SettingsManager.ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                    SettingsManager.ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
                },
                onClick = { showThemeDialog = true }
            )
            
            HorizontalDivider()
            
            // Debug Section (Only in Debug builds)
            if (BuildConfig.DEBUG && false) {
                SettingsSectionTitle(stringResource(R.string.settings_debug))
                
                SettingsItem(
                    icon = Icons.Default.BugReport,
                    title = stringResource(R.string.settings_debug_premium),
                    subtitle = if (state.isDebugPremium) "ON" else "OFF",
                    onClick = { viewModel.onDebugPremiumToggle(!state.isDebugPremium) },
                    trailing = {
                        Switch(
                            checked = state.isDebugPremium,
                            onCheckedChange = { viewModel.onDebugPremiumToggle(it) }
                        )
                    }
                )
                
                HorizontalDivider()
            }
            
            // Logout
            SettingsSectionTitle("")
            
            SettingsItem(
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                title = stringResource(R.string.settings_logout),
                onClick = onLogoutClick,
                textColor = MaterialTheme.colorScheme.error
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Version Info
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
    
    // Language Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.language_dialog_title)) },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            viewModel.onLanguageChange(SettingsManager.LANGUAGE_ENGLISH)
                            LocaleHelper.setLocale(context as android.app.Activity, "en")
                            showLanguageDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Text(stringResource(R.string.language_english), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            viewModel.onLanguageChange(SettingsManager.LANGUAGE_SPANISH)
                            LocaleHelper.setLocale(context as android.app.Activity, "es")
                            showLanguageDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Text(stringResource(R.string.language_spanish), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.language_cancel))
                }
            }
        )
    }
    
    // Theme Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.settings_theme_dialog_title)) },
            text = {
                Column {
                    // Light Mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = state.themeMode == SettingsManager.ThemeMode.LIGHT,
                            onClick = {
                                viewModel.onThemeModeChange(SettingsManager.ThemeMode.LIGHT)
                                showThemeDialog = false
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.settings_theme_light),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    // Dark Mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = state.themeMode == SettingsManager.ThemeMode.DARK,
                            onClick = {
                                viewModel.onThemeModeChange(SettingsManager.ThemeMode.DARK)
                                showThemeDialog = false
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.settings_theme_dark),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    // System Mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = state.themeMode == SettingsManager.ThemeMode.SYSTEM,
                            onClick = {
                                viewModel.onThemeModeChange(SettingsManager.ThemeMode.SYSTEM)
                                showThemeDialog = false
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.settings_theme_system),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(stringResource(R.string.language_cancel))
                }
            }
        )
    }
}