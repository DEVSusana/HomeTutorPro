package com.devsusana.hometutorpro.presentation.student_detail.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartClassDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var startTime by remember { mutableStateOf(LocalTime.now()) }
    var endTime by remember { mutableStateOf(LocalTime.now().plusHours(1)) }
    
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    if (showStartTimePicker) {
        com.devsusana.hometutorpro.presentation.components.TimePickerDialog(
            initialTime = startTime.format(DateTimeFormatter.ofPattern("HH:mm")),
            onDismiss = { showStartTimePicker = false },
            onTimeSelected = { 
                startTime = LocalTime.parse(it)
                endTime = startTime.plusHours(1)
                showStartTimePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        com.devsusana.hometutorpro.presentation.components.TimePickerDialog(
            initialTime = endTime.format(DateTimeFormatter.ofPattern("HH:mm")),
            onDismiss = { showEndTimePicker = false },
            onTimeSelected = { 
               endTime = LocalTime.parse(it)
               showEndTimePicker = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.start_class_dialog_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(id = R.string.start_class_dialog_text))
                
                // Time Selectors ROW
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Start Time
                    OutlinedButton(
                        onClick = { showStartTimePicker = true },
                        modifier = Modifier.weight(1f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(startTime.format(DateTimeFormatter.ofPattern("HH:mm")))
                    }
                    
                    // End Time
                    OutlinedButton(
                        onClick = { showEndTimePicker = true },
                        modifier = Modifier.weight(1f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(endTime.format(DateTimeFormatter.ofPattern("HH:mm")))
                    }
                }
                
                // Display calculated duration
                val durationMinutes = java.time.Duration.between(startTime, endTime).toMinutes().toInt()
                if (durationMinutes > 0) {
                    Text(
                        text = stringResource(id = R.string.start_class_dialog_duration, durationMinutes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    val durationMinutes = java.time.Duration.between(startTime, endTime).toMinutes().toInt()
                    if (durationMinutes > 0) {
                        onConfirm(durationMinutes) 
                    }
                }
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
