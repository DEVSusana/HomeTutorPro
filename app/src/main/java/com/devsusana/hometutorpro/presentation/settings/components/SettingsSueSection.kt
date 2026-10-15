package com.devsusana.hometutorpro.presentation.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.domain.entities.SueDeviceCompatibility
import com.devsusana.hometutorpro.domain.entities.SueModelStatus
import com.devsusana.hometutorpro.domain.entities.SueUnsupportedReason

/**
 * SUE AI Assistant settings section, including compatibility, model download progress, and help.
 */
@Composable
fun SettingsSueSection(
    isSueEnabled: Boolean,
    isSueFabVisible: Boolean,
    sueDeviceCompatibility: SueDeviceCompatibility,
    sueModelStatus: SueModelStatus,
    bringIntoViewRequester: BringIntoViewRequester,
    onSueEnabledToggle: (Boolean) -> Unit,
    onSueFabVisibleToggle: (Boolean) -> Unit,
    onDownloadSueModel: () -> Unit,
    onCancelSueModelDownload: () -> Unit,
    onDeleteSueModelClick: () -> Unit,
    onShowSueHelpDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
    ) {
        SettingsSectionTitle(stringResource(R.string.sue_settings_section_title))
        Card(
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            if (!sueDeviceCompatibility.isSupported) {
                val reasonText = when (sueDeviceCompatibility.reason) {
                    SueUnsupportedReason.UNSUPPORTED_ANDROID_VERSION ->
                        stringResource(R.string.sue_compatibility_reason_android)
                    SueUnsupportedReason.INSUFFICIENT_RAM ->
                        stringResource(R.string.sue_compatibility_reason_ram)
                    null ->
                        stringResource(R.string.sue_compatibility_reason_generic)
                }

                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.sue_compatibility_unsupported_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = reasonText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            } else {
                SettingsItem(
                    icon = Icons.Default.AutoAwesome,
                    title = stringResource(R.string.sue_settings_enable_title),
                    subtitle = stringResource(R.string.sue_settings_enable_desc),
                    onClick = { onSueEnabledToggle(!isSueEnabled) },
                    trailing = {
                        Switch(
                            checked = isSueEnabled,
                            onCheckedChange = { onSueEnabledToggle(it) }
                        )
                    }
                )
                if (isSueEnabled) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsItem(
                        icon = Icons.Default.Mic,
                        title = stringResource(R.string.sue_settings_fab_visibility_title),
                        subtitle = stringResource(R.string.sue_settings_fab_visibility_desc),
                        onClick = { onSueFabVisibleToggle(!isSueFabVisible) },
                        trailing = {
                            Switch(
                                checked = isSueFabVisible,
                                onCheckedChange = { onSueFabVisibleToggle(it) }
                            )
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Model Storage & Download Management
                    when (sueModelStatus) {
                        is SueModelStatus.Downloaded -> {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDone,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.sue_model_status_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = stringResource(R.string.sue_model_downloaded_badge, sueModelStatus.formattedSize),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    onClick = onDeleteSueModelClick,
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.sue_model_delete_btn, sueModelStatus.formattedSize))
                                }
                            }
                        }
                        is SueModelStatus.Downloading -> {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.sue_model_status_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        val percent = if (sueModelStatus.progress >= 0) (sueModelStatus.progress * 100).toInt() else 0
                                        val downloadedMb = sueModelStatus.bytesDownloaded / (1024 * 1024)
                                        val totalMb = if (sueModelStatus.totalBytes > 0) sueModelStatus.totalBytes / (1024 * 1024) else 550
                                        Text(
                                            text = stringResource(R.string.sue_model_downloading, percent, "${downloadedMb}MB", "${totalMb}MB"),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                if (sueModelStatus.progress >= 0) {
                                    LinearProgressIndicator(
                                        progress = { sueModelStatus.progress },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = onCancelSueModelDownload,
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text(stringResource(R.string.sue_model_download_cancel_btn))
                                }
                            }
                        }
                        else -> {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.sue_model_status_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = stringResource(R.string.sue_model_download_desc),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = onDownloadSueModel,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.sue_model_download_btn))
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "Guía de comandos de SUE",
                    subtitle = "Aprende cómo hablarle a SUE",
                    onClick = onShowSueHelpDialog
                )
            }
        }
    }
}
