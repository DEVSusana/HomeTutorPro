package com.devsusana.hometutorpro.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devsusana.hometutorpro.BuildConfig
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.core.settings.SettingsManager
import com.devsusana.hometutorpro.presentation.settings.components.SettingsItem
import com.devsusana.hometutorpro.presentation.settings.components.SettingsSectionTitle
import com.devsusana.hometutorpro.presentation.utils.LocaleHelper
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onLogoutClick: () -> Unit,
    onPremiumClick: () -> Unit,
    onEditProfileClick: () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            com.devsusana.hometutorpro.di.SettingsManagerEntryPoint::class.java
        ).settingsManager()
    }
    
    val coroutineScope = rememberCoroutineScope()
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
            
            // Premium Status
            // TODO: Remove false check to re-enable Premium UI
            if (false) {
                SettingsItem(
                    icon = Icons.Default.Star,
                    title = stringResource(R.string.settings_premium),
                    subtitle = if (isPremium) stringResource(R.string.settings_premium_active) else stringResource(R.string.settings_premium_inactive),
                    onClick = onPremiumClick,
                    trailing = {
                        if (!isPremium) {
                            TextButton(onClick = onPremiumClick) {
                                Text(stringResource(R.string.settings_upgrade_premium))
                            }
                        }
                    }
                )
            }
            
            // Edit Profile
            SettingsItem(
                icon = Icons.Default.Person,
                title = stringResource(R.string.settings_edit_profile),
                onClick = onEditProfileClick
            )
            
            HorizontalDivider()
            
            // Language
            SettingsItem(
                icon = Icons.Default.Language,
                title = stringResource(R.string.settings_language),
                subtitle = if (java.util.Locale.getDefault().language == "es") "Español" else "English",
                onClick = { showLanguageDialog = true }
            )
            
            HorizontalDivider()
            
            // Theme Mode
            val themeMode by settingsManager.themeModeFlow.collectAsState(initial = SettingsManager.ThemeMode.SYSTEM)
            
            SettingsItem(
                icon = Icons.Default.Palette,
                title = stringResource(R.string.settings_theme),
                subtitle = when (themeMode) {
                    SettingsManager.ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                    SettingsManager.ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                    SettingsManager.ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
                },
                onClick = { showThemeDialog = true }
            )
            
            HorizontalDivider()
            
            // Debug Section (Only in Debug builds)
            // TODO: Remove checks to re-enable Debug Premium UI
            if (BuildConfig.DEBUG && false) {
                val isDebugPremium by settingsManager.isDebugPremiumFlow.collectAsState(initial = false)
                
                SettingsSectionTitle(stringResource(R.string.settings_debug))
                
                SettingsItem(
                    icon = Icons.Default.BugReport,
                    title = stringResource(R.string.settings_debug_premium),
                    subtitle = if (isDebugPremium) "ON" else "OFF",
                    onClick = { 
                        coroutineScope.launch {
                            settingsManager.setDebugPremium(!isDebugPremium)
                        }
                    },
                    trailing = {
                        Switch(
                            checked = isDebugPremium,
                            onCheckedChange = { 
                                coroutineScope.launch {
                                    settingsManager.setDebugPremium(it)
                                }
                            }
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
                            coroutineScope.launch {
                                settingsManager.setLanguage(SettingsManager.LANGUAGE_ENGLISH)
                                LocaleHelper.setLocale(context as android.app.Activity, "en")
                            }
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
                            coroutineScope.launch {
                                settingsManager.setLanguage(SettingsManager.LANGUAGE_SPANISH)
                                LocaleHelper.setLocale(context as android.app.Activity, "es")
                            }
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
        val themeMode by settingsManager.themeModeFlow.collectAsState(initial = SettingsManager.ThemeMode.SYSTEM)
        
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
                            selected = themeMode == SettingsManager.ThemeMode.LIGHT,
                            onClick = {
                                coroutineScope.launch {
                                    settingsManager.setThemeMode(SettingsManager.ThemeMode.LIGHT)
                                }
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
                            selected = themeMode == SettingsManager.ThemeMode.DARK,
                            onClick = {
                                coroutineScope.launch {
                                    settingsManager.setThemeMode(SettingsManager.ThemeMode.DARK)
                                }
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
                            selected = themeMode == SettingsManager.ThemeMode.SYSTEM,
                            onClick = {
                                coroutineScope.launch {
                                    settingsManager.setThemeMode(SettingsManager.ThemeMode.SYSTEM)
                                }
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

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme {
        // Mocking SettingsManager would be needed for a real preview with data logic
        // For visual preview of layout:
        SettingsSectionTitle("Account")
    }
}
