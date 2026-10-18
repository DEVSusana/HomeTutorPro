package com.devsusana.hometutorpro.presentation.settings.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.devsusana.hometutorpro.R

/**
 * Account management section for Settings (profile edit, password change).
 */
@Composable
fun SettingsAccountSection(
    onEditProfileClick: () -> Unit,
    onChangePasswordClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsSectionTitle(stringResource(R.string.settings_account))
    Card(
        modifier = modifier.padding(horizontal = 16.dp).fillMaxWidth(),
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
}
