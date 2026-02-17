package com.devsusana.hometutorpro.presentation.schedule_form.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.presentation.schedule.ScheduleFormState
import com.devsusana.hometutorpro.presentation.components.TimePickerDialog
import com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme
import com.devsusana.hometutorpro.presentation.utils.DayOfWeekUtils
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleFormContent(
    state: ScheduleFormState,
    onDayOfWeekChange: (DayOfWeek) -> Unit,
    onStartTimeChange: (String) -> Unit,
    onEndTimeChange: (String) -> Unit,
    onSaveSchedule: () -> Unit,
    onBack: () -> Unit
) {
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    if (showStartTimePicker) {
        TimePickerDialog(
            initialTime = state.schedule.startTime,
            onDismiss = { showStartTimePicker = false },
            onTimeSelected = {
                onStartTimeChange(it)
                showStartTimePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            initialTime = state.schedule.endTime,
            onDismiss = { showEndTimePicker = false },
            onTimeSelected = {
                onEndTimeChange(it)
                showEndTimePicker = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_schedule)) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.schedule_form_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Day of Week Dropdown
                    var expanded by remember { mutableStateOf(false) }
                    
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = DayOfWeekUtils.getLocalizedName(state.schedule.dayOfWeek),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.day_of_week)) },
                            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = stringResource(R.string.cd_calendar_icon)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("day_of_week_field"),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DayOfWeek.values().forEach { day ->
                                DropdownMenuItem(
                                    text = { Text(DayOfWeekUtils.getLocalizedName(day)) },
                                    onClick = {
                                        onDayOfWeekChange(day)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Start Time
                    OutlinedTextField(
                        value = state.schedule.startTime,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text(stringResource(R.string.start_time)) },
                        leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = stringResource(R.string.cd_time_icon)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("start_time_field"),
                        shape = RoundedCornerShape(12.dp),
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                            .also { interactionSource ->
                                LaunchedEffect(interactionSource) {
                                    interactionSource.interactions.collect {
                                        if (it is androidx.compose.foundation.interaction.PressInteraction.Release) {
                                            showStartTimePicker = true
                                        }
                                    }
                                }
                            }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // End Time
                    OutlinedTextField(
                        value = state.schedule.endTime,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text(stringResource(R.string.end_time)) },
                        leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = stringResource(R.string.cd_time_icon)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("end_time_field"),
                        shape = RoundedCornerShape(12.dp),
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                            .also { interactionSource ->
                                LaunchedEffect(interactionSource) {
                                    interactionSource.interactions.collect {
                                        if (it is androidx.compose.foundation.interaction.PressInteraction.Release) {
                                            showEndTimePicker = true
                                        }
                                    }
                                }
                            }
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Save Button
                    Button(
                        onClick = onSaveSchedule,
                        enabled = !state.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_schedule_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(id = R.string.schedule_form_save_schedule))
                        }
                    }

                    // Error message
                    state.error?.let { error ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val errorMessage = when(error) {
                                is Int -> stringResource(id = error)
                                is String -> error
                                else -> ""
                            }
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ScheduleFormContentPreview() {
    HomeTutorProTheme {
        ScheduleFormContent(
            state = ScheduleFormState(),
            onDayOfWeekChange = {},
            onStartTimeChange = {},
            onEndTimeChange = {},
            onSaveSchedule = {},
            onBack = {}
        )
    }
}
