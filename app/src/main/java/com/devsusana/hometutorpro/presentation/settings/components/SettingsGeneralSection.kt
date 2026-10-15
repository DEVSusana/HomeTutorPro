package com.devsusana.hometutorpro.presentation.settings.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.domain.entities.AppThemeMode

/**
 * General application preferences section (language and theme mode).
 */
@Composable
fun SettingsGeneralSection(
    language: String,
    themeMode: AppThemeMode,
    onLanguageClick: () -> Unit,
    onThemeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsSectionTitle(stringResource(R.string.settings_theme_dialog_title))
    Card(
        modifier = modifier.padding(horizontal = 16.dp).fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        SettingsItem(
            icon = Icons.Default.Language,
            title = stringResource(R.string.settings_language),
            subtitle = if (language == "es") "Español" else "English",
            onClick = onLanguageClick
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        SettingsItem(
            icon = Icons.Default.Palette,
            title = stringResource(R.string.settings_theme),
            subtitle = when (themeMode) {
                AppThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                AppThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                AppThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
            },
            onClick = onThemeClick
        )
    }
}
