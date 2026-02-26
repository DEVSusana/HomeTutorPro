package com.devsusana.hometutorpro.presentation.student_detail.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Dialog for selecting class duration when starting a class.
 * Uses TimePicker for selecting start and end time.
 */
@Composable
fun StartClassDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selectedDuration by remember { mutableIntStateOf(60) }
    val durations = listOf(30, 45, 60, 90, 120)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.start_class_dialog_title)) },
        text = {
            Column(
                modifier = Modifier.semantics { selectableGroup() }
            ) {
                Text(stringResource(id = R.string.start_class_dialog_text))
                Spacer(modifier = Modifier.height(10.dp))
                durations.forEach { duration ->
                    val isSelected = selectedDuration == duration
                    val selectedStateDescription = stringResource(R.string.cd_state_selected)
                    val notSelectedStateDescription = stringResource(R.string.cd_state_not_selected)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .heightIn(min = 48.dp)
                            .selectable(
                                selected = isSelected,
                                onClick = { selectedDuration = duration },
                                role = Role.RadioButton
                            )
                            .semantics {
                                stateDescription = if (isSelected) {
                                    selectedStateDescription
                                } else {
                                    notSelectedStateDescription
                                }
                            }
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = null
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
