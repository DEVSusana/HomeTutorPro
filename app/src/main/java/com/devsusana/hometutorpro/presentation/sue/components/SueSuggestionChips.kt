package com.devsusana.hometutorpro.presentation.sue.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Horizontally scrollable row of contextual prompt suggestion chips for SUE.
 *
 * @param suggestions List of suggested prompt strings to display.
 * @param onSuggestionClick Callback invoked when a suggestion chip is tapped.
 * @param modifier Layout modifier.
 */
@Composable
fun SueSuggestionChips(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (suggestions.isEmpty()) return

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        items(suggestions, key = { it }) { suggestion ->
            SuggestionChip(
                onClick = { onSuggestionClick(suggestion) },
                label = {
                    Text(
                        text = suggestion,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = SuggestionChipDefaults.suggestionChipBorder(
                    enabled = true,
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            )
        }
    }
}

@Preview(showBackground = true, name = "Suggestion Chips Preview")
@Composable
private fun SueSuggestionChipsPreview() {
    MaterialTheme {
        SueSuggestionChips(
            suggestions = listOf(
                "¿Qué clases tengo hoy?",
                "¿Quién me debe dinero?",
                "Próxima clase"
            ),
            onSuggestionClick = {}
        )
    }
}
