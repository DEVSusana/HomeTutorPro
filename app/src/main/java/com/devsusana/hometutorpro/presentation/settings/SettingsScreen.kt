package com.devsusana.hometutorpro.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.devsusana.hometutorpro.BuildConfig
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.domain.entities.AppThemeMode
import com.devsusana.hometutorpro.presentation.settings.components.SettingsItem
import com.devsusana.hometutorpro.presentation.settings.components.SettingsSectionTitle
import com.devsusana.hometutorpro.presentation.utils.LocaleHelper

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.devsusana.hometutorpro.presentation.components.FeedbackDialog
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsScreen(
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
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmation = false
                confirmPassword = ""
                isPasswordVisible = false
            },
            title = { Text(stringResource(R.string.settings_delete_account_confirm_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.settings_delete_account_confirm_desc))
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text(stringResource(R.string.settings_delete_account_confirm_password_label)) },
                        visualTransformation = if (isPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (isPasswordVisible) stringResource(R.string.hide_password) else stringResource(R.string.show_password)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pw = confirmPassword
                        showDeleteConfirmation = false
                        confirmPassword = ""
                        isPasswordVisible = false
                        viewModel.deleteAccount(pw) {
                            onLogoutClick()
                        }
                    },
                    enabled = confirmPassword.length >= 6,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.settings_delete_account_btn_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    confirmPassword = ""
                    isPasswordVisible = false
                }) {
                    Text(stringResource(R.string.cancel))
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
        var currentPassword by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        var confirmPasswordVal by remember { mutableStateOf("") }
        var isCurrentPasswordVisible by remember { mutableStateOf(false) }
        var isNewPasswordVisible by remember { mutableStateOf(false) }
        var isConfirmPasswordVisible by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { viewModel.showChangePasswordDialog(false) },
            title = {
                Text(
                    text = stringResource(R.string.settings_change_password_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.settings_change_password_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text(stringResource(R.string.settings_change_password_current_label)) },
                        visualTransformation = if (isCurrentPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { isCurrentPasswordVisible = !isCurrentPasswordVisible }) {
                                Icon(
                                    imageVector = if (isCurrentPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (isCurrentPasswordVisible) stringResource(R.string.hide_password) else stringResource(R.string.show_password)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text(stringResource(R.string.settings_change_password_new_label)) },
                        visualTransformation = if (isNewPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { isNewPasswordVisible = !isNewPasswordVisible }) {
                                Icon(
                                    imageVector = if (isNewPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (isNewPasswordVisible) stringResource(R.string.hide_password) else stringResource(R.string.show_password)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = confirmPasswordVal,
                        onValueChange = { confirmPasswordVal = it },
                        label = { Text(stringResource(R.string.settings_change_password_confirm_label)) },
                        visualTransformation = if (isConfirmPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (isConfirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (isConfirmPasswordVisible) stringResource(R.string.hide_password) else stringResource(R.string.show_password)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    )
                    if (state.changePasswordError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(state.changePasswordError!!),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.changePassword(currentPassword, newPassword, confirmPasswordVal) },
                    enabled = !state.isChangingPassword && currentPassword.isNotBlank() && newPassword.isNotBlank() && confirmPasswordVal.isNotBlank(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    if (state.isChangingPassword) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(stringResource(R.string.settings_change_password_submit))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.showChangePasswordDialog(false) },
                    enabled = !state.isChangingPassword
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        )
    }

    if (state.changePasswordSuccess) {
        FeedbackDialog(
            isSuccess = true,
            message = { Text(stringResource(R.string.settings_change_password_success)) },
            onDismiss = viewModel::clearChangePasswordFeedback
        )
    }

    SettingsContent(
        state = state,
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
        onLanguageChange = { lang ->
            scope.launch {
                viewModel.setLanguageSync(lang)
                val activity = context as? android.app.Activity ?: return@launch
                LocaleHelper.setLocale(activity, lang)
                activity.recreate()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    state: SettingsState,
    onEditProfileClick: () -> Unit,
    onChangePasswordClick: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onClassEndNotificationsToggle: (Boolean) -> Unit,
    onShowTestNotification: () -> Unit,
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
            // Account Section
            SettingsSectionTitle(stringResource(R.string.settings_account))
            Card(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                SettingsItem(
                    icon = Icons.Default.Person,
                    title = stringResource(R.string.settings_edit_profile),
                    onClick = onEditProfileClick
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsItem(
                    icon = Icons.Default.Lock,
                    title = stringResource(R.string.settings_change_password),
                    onClick = onChangePasswordClick
                )
            }

            // Backup Section
            SettingsSectionTitle(stringResource(R.string.settings_backup_title))
            Card(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                SettingsItem(
                    icon = Icons.Default.Save,
                    title = stringResource(R.string.settings_backup_export),
                    subtitle = stringResource(R.string.settings_backup_desc),
                    onClick = onExportBackup
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsItem(
                    icon = Icons.Default.UploadFile,
                    title = stringResource(R.string.settings_backup_import),
                    onClick = onImportBackup
                )
                if (state.isBackupLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
            
            // Notifications Section
            SettingsSectionTitle(stringResource(R.string.settings_notifications))
            Card(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                SettingsItem(
                    icon = Icons.Default.Notifications,
                    title = stringResource(R.string.settings_class_end_notifications),
                    subtitle = stringResource(R.string.settings_class_end_notifications_desc),
                    onClick = { onClassEndNotificationsToggle(!state.classEndNotificationsEnabled) },
                    trailing = {
                        val stateOnDescription = stringResource(R.string.cd_state_on)
                        val stateOffDescription = stringResource(R.string.cd_state_off)
                        Switch(
                            checked = state.classEndNotificationsEnabled,
                            onCheckedChange = { onClassEndNotificationsToggle(it) },
                            modifier = Modifier.semantics {
                                stateDescription = if (state.classEndNotificationsEnabled) {
                                    stateOnDescription
                                } else {
                                    stateOffDescription
                                }
                            }
                        )
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                TextButton(
                    onClick = onShowTestNotification,
                    modifier = Modifier.padding(start = 56.dp, top = 4.dp, bottom = 8.dp)
                ) {
                    Text(stringResource(R.string.settings_test_alarm))
                }
            }
            
            // Preferences Section
            SettingsSectionTitle(stringResource(R.string.settings_theme_dialog_title))
            Card(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                SettingsItem(
                    icon = Icons.Default.Language,
                    title = stringResource(R.string.settings_language),
                    subtitle = if (state.language == "es") "Español" else "English",
                    onClick = { showLanguageDialog = true }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsItem(
                    icon = Icons.Default.Palette,
                    title = stringResource(R.string.settings_theme),
                    subtitle = when (state.themeMode) {
                        AppThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                        AppThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                        AppThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
                    },
                    onClick = { showThemeDialog = true }
                )
            }

            // Sue Help Section
            SettingsSectionTitle("Asistente SUE")
            Card(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "Guía de comandos de SUE",
                    subtitle = "Aprende cómo hablarle a SUE",
                    onClick = { showSueHelpDialog = true }
                )
            }

            // Legal Section
            SettingsSectionTitle(stringResource(R.string.settings_legal_title))
            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
            Card(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                SettingsItem(
                    icon = Icons.Default.Description,
                    title = stringResource(R.string.settings_terms_of_use),
                    onClick = { uriHandler.openUri("https://hometutorpro.web.app/terms_of_use.html") }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = stringResource(R.string.settings_privacy_policy),
                    onClick = { uriHandler.openUri("https://hometutorpro.web.app/privacy_policy.html") }
                )
            }
            
            // Actions Section
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                SettingsItem(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    title = stringResource(R.string.settings_logout),
                    onClick = onLogoutClick,
                    textColor = MaterialTheme.colorScheme.error
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsItem(
                    icon = Icons.Default.Delete,
                    title = stringResource(R.string.settings_delete_account),
                    onClick = onDeleteAccountClick,
                    textColor = MaterialTheme.colorScheme.error
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
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
                            onLanguageChange("en")
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
                            onLanguageChange("es")
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
                            selected = state.themeMode == AppThemeMode.LIGHT,
                            onClick = {
                                onThemeModeChange(AppThemeMode.LIGHT)
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
                            selected = state.themeMode == AppThemeMode.DARK,
                            onClick = {
                                onThemeModeChange(AppThemeMode.DARK)
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
                            selected = state.themeMode == AppThemeMode.SYSTEM,
                            onClick = {
                                onThemeModeChange(AppThemeMode.SYSTEM)
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

    if (showSueHelpDialog) {
        AlertDialog(
            onDismissRequest = { showSueHelpDialog = false },
            title = { Text("Guía de Ayuda de SUE") },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "SUE es tu asistente inteligente local. Puedes pedirle que realice tareas mediante comandos de voz coloquiales.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Ejemplos de Comandos Soportados:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    HelpExampleItem(
                        intent = "Crear Alumno",
                        examples = listOf(
                            "\"Crea un alumno llamado Pepe los miércoles a las 3 de la tarde\"",
                            "\"Añade un estudiante llamado Ana de bachillerato a 15 la hora\""
                        )
                    )
                    HelpExampleItem(
                        intent = "Consultar Horario",
                        examples = listOf(
                            "\"¿Qué clases tengo el viernes?\"",
                            "\"¿Qué clase tengo hoy a las 1800?\"",
                            "\"¿Cuál es mi siguiente clase?\""
                        )
                    )
                    
                    HelpExampleItem(
                        intent = "Consultar Huecos Libres",
                        examples = listOf(
                            "\"¿Tengo huecos libres hoy?\"",
                            "\"¿Qué huecos libres tengo esta semana?\""
                        )
                    )
                    
                    HelpExampleItem(
                        intent = "Consultar Clases Canceladas",
                        examples = listOf(
                            "\"¿Qué clases tengo canceladas hoy?\"",
                            "\"¿Tengo alguna clase cancelada el jueves?\""
                        )
                    )
                    
                    HelpExampleItem(
                        intent = "Mover o Reprogramar Clases",
                        examples = listOf(
                            "\"Mueve la clase de Pepe del lunes al viernes\"",
                            "\"Reprograma la clase de Ana de hoy para mañana a las 5 de la tarde\""
                        )
                    )
                    
                    HelpExampleItem(
                        intent = "Cancelar Clases",
                        examples = listOf(
                            "\"Cancela la clase de Pepe de este viernes\"",
                            "\"Borra el horario de Ana los martes\""
                        )
                    )
                    
                    HelpExampleItem(
                        intent = "Pagos y Deudas",
                        examples = listOf(
                            "\"Pepe me ha pagado 30 euros en efectivo\"",
                            "\"Súmale 20 euros a la deuda de Ana\""
                        )
                    )
                    
                    HelpExampleItem(
                        intent = "Iniciar Clases",
                        examples = listOf(
                            "\"Inicia una clase con Pepe de 60 minutos\""
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSueHelpDialog = false }) {
                    Text("Entendido")
                }
            }
        )
    }

    // Backup Feedback
    state.backupMessage?.let { message ->
        FeedbackDialog(
            isSuccess = state.isBackupSuccess,
            message = { Text(message) },
            onDismiss = onDismissBackupMessage
        )
    }
}

@Composable
private fun HelpExampleItem(intent: String, examples: List<String>) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = intent,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        examples.forEach { example ->
            Text(
                text = example,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsContentPreview() {
    com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme {
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
            onLanguageChange = {},
            onThemeModeChange = {},
            onDebugPremiumToggle = {},
            onLogoutClick = {},
            onDeleteAccountClick = {},
            onDismissBackupMessage = {}
        )
    }
}
