package com.devsusana.hometutorpro.presentation.student_detail.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import com.devsusana.hometutorpro.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExtraClassDialog(
    onDismiss: () -> Unit,
    onConfirm: (Long, String, String) -> Unit
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var startTime by remember { mutableStateOf(LocalTime.of(16, 0)) }
    var endTime by remember { mutableStateOf(LocalTime.of(17, 0)) }
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDate = java.time.Instant.ofEpochMilli(it)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

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
        title = { Text(stringResource(R.string.student_detail_add_extra_class)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Date Selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        onValueChange = {},
                        label = { Text(stringResource(R.string.schedule_form_date)) },
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Default.CalendarToday, null) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showDatePicker = true }
                    )
                }

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
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(startTime.format(DateTimeFormatter.ofPattern("HH:mm")))
                    }
                    
                    // End Time
                    OutlinedButton(
                        onClick = { showEndTimePicker = true },
                        modifier = Modifier.weight(1f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(endTime.format(DateTimeFormatter.ofPattern("HH:mm")))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val epochMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val startStr = startTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                val endStr = endTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                onConfirm(epochMillis, startStr, endStr)
            }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.student_detail_cancel))
            }
        }
    )
}
