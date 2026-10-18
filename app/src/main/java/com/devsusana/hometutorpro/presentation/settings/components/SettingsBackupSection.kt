package com.devsusana.hometutorpro.presentation.settings.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.devsusana.hometutorpro.R

/**
 * Backup and restore section for Settings (export, import).
 */
@Composable
fun SettingsBackupSection(
    isBackupLoading: Boolean,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsSectionTitle(stringResource(R.string.settings_backup_title))
    Card(
        modifier = modifier.padding(horizontal = 16.dp).fillMaxWidth(),
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
        if (isBackupLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}
