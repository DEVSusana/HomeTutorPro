package com.devsusana.hometutorpro.presentation.settings.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.devsusana.hometutorpro.R

/**
 * Confirmation dialog before deleting the downloaded on-device Sue LLM model.
 */
@Composable
fun DeleteSueModelDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sue_model_delete_confirm_title)) },
        text = { Text(stringResource(R.string.sue_model_delete_confirm_desc)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.sue_model_delete_confirm_btn))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.sue_model_delete_cancel_btn))
            }
        }
    )
}
