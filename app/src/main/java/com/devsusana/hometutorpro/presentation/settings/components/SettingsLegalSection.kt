package com.devsusana.hometutorpro.presentation.settings.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.devsusana.hometutorpro.R

/**
 * Legal terms and privacy policy section for Settings.
 */
@Composable
fun SettingsLegalSection(
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current

    SettingsSectionTitle(stringResource(R.string.settings_legal_title))
    Card(
        modifier = modifier.padding(horizontal = 16.dp).fillMaxWidth(),
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
}
