package com.devsusana.hometutorpro.presentation.student_detail.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme

/**
 * Dialog for selecting class duration when starting a class.
 */
@Composable
fun StartClassDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selectedDuration by remember { mutableStateOf(60) }
    val durations = listOf(30, 45, 60, 90, 120)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.start_class_dialog_title)) },
        text = {
            Column {
                Text(stringResource(id = R.string.start_class_dialog_text))
                Spacer(modifier = Modifier.height(10.dp))
                durations.forEach { duration ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { selectedDuration = duration }
                    ) {
                        RadioButton(
                            selected = (selectedDuration == duration),
                            onClick = { selectedDuration = duration }
                        )
                        Text(
                            text = stringResource(id = R.string.start_class_dialog_duration, duration),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedDuration) }
            ) {
                Text(stringResource(id = R.string.start_class_dialog_start))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.start_class_dialog_cancel))
            }
        }
    )
}

@Preview(showBackground = true, name = "Start Class Dialog")
@Composable
private fun StartClassDialogPreview() {
    HomeTutorProTheme {
        StartClassDialog(
            onDismiss = {},
            onConfirm = {}
        )
    }
}
