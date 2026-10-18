package com.devsusana.hometutorpro.presentation.settings.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.devsusana.hometutorpro.R

/**
 * Notifications and alarm permission section for Settings.
 */
@Composable
fun SettingsNotificationsSection(
    classEndNotificationsEnabled: Boolean,
    onClassEndNotificationsToggle: (Boolean) -> Unit,
    onOpenSystemPermissions: () -> Unit,
    onShowTestNotification: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsSectionTitle(stringResource(R.string.settings_notifications))
    Card(
        modifier = modifier.padding(horizontal = 16.dp).fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        SettingsItem(
            icon = Icons.Default.Notifications,
            title = stringResource(R.string.settings_class_end_notifications),
            subtitle = stringResource(R.string.settings_class_end_notifications_desc),
            onClick = { onClassEndNotificationsToggle(!classEndNotificationsEnabled) },
            trailing = {
                val stateOnDescription = stringResource(R.string.cd_state_on)
                val stateOffDescription = stringResource(R.string.cd_state_off)
                Switch(
                    checked = classEndNotificationsEnabled,
                    onCheckedChange = { onClassEndNotificationsToggle(it) },
                    modifier = Modifier.semantics {
                        stateDescription = if (classEndNotificationsEnabled) {
                            stateOnDescription
                        } else {
                            stateOffDescription
                        }
                    }
                )
            }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        SettingsItem(
            icon = Icons.Default.Tune,
            title = stringResource(R.string.settings_system_permissions_title),
            subtitle = stringResource(R.string.settings_system_permissions_desc),
            onClick = onOpenSystemPermissions
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        TextButton(
            onClick = onShowTestNotification,
            modifier = Modifier.padding(start = 56.dp, top = 4.dp, bottom = 8.dp)
        ) {
            Text(stringResource(R.string.settings_test_alarm))
        }
    }
}
