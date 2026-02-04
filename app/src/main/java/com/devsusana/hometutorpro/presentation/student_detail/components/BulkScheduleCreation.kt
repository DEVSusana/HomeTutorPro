package com.devsusana.hometutorpro.presentation.student_detail.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.domain.entities.Schedule
import java.time.DayOfWeek

/**
 * Data class to hold a single schedule entry in the bulk creation UI
 */
data class BulkScheduleEntry(
    val id: Int,
    val dayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val startTime: String = "",
    val endTime: String = "",
    val error: String? = null
)

/**
 * Composable for creating multiple schedules at once
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkScheduleCreation(
    studentId: String,
    schedules: List<BulkScheduleEntry>,
    onSchedulesChange: (List<BulkScheduleEntry>) -> Unit,
    onSaveAll: () -> Unit,
    onCancel: () -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = stringResource(R.string.bulk_schedule_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (schedules.isEmpty()) {
            // Empty state
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.bulk_schedule_no_schedules),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Schedule entries
            schedules.forEachIndexed { index, schedule ->
                ScheduleEntryCard(
                    scheduleNumber = index + 1,
                    schedule = schedule,
                    onScheduleChange = { updatedSchedule ->
                        val newSchedules = schedules.toMutableList()
                        newSchedules[index] = updatedSchedule
                        onSchedulesChange(newSchedules)
                    },
                    onRemove = {
                        val newSchedules = schedules.toMutableList()
                        newSchedules.removeAt(index)
                        onSchedulesChange(newSchedules)
                    }
                )
            }
        }

        // Add Another Button
        OutlinedButton(
            onClick = {
                val newSchedules = schedules.toMutableList()
                newSchedules.add(
                    BulkScheduleEntry(
                        id = (schedules.maxOfOrNull { it.id } ?: 0) + 1
                    )
                )
                onSchedulesChange(newSchedules)
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_another_schedule_button")
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.bulk_schedule_add_another))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .testTag("cancel_bulk_schedule_button"),
                enabled = !isLoading
            ) {
                Text(stringResource(R.string.student_detail_cancel))
            }

            Button(
                onClick = onSaveAll,
                modifier = Modifier
                    .weight(1f)
                    .testTag("save_all_schedules_button"),
                enabled = schedules.isNotEmpty() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.bulk_schedule_save_all))
                }
            }
        }

        // Bottom spacing
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleEntryCard(
    scheduleNumber: Int,
    schedule: BulkScheduleEntry,
    onScheduleChange: (BulkScheduleEntry) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (schedule.error != null) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with number and remove button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.bulk_schedule_schedule_number, scheduleNumber),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.testTag("remove_schedule_${scheduleNumber}_button")
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.bulk_schedule_remove),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Day of Week Dropdown
            var expanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = com.devsusana.hometutorpro.presentation.utils.DayOfWeekUtils.getLocalizedName(schedule.dayOfWeek),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.day_of_week)) },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = stringResource(R.string.cd_calendar_icon)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .testTag("day_of_week_field_$scheduleNumber"),
                    shape = RoundedCornerShape(8.dp)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DayOfWeek.entries.forEach { day ->
                        DropdownMenuItem(
                            text = { Text(com.devsusana.hometutorpro.presentation.utils.DayOfWeekUtils.getLocalizedName(day)) },
                            onClick = {
                                onScheduleChange(schedule.copy(dayOfWeek = day))
                                expanded = false
                            }
                        )
                    }
                }
            }

            // Start Time
            OutlinedTextField(
                value = schedule.startTime,
                onValueChange = { onScheduleChange(schedule.copy(startTime = it)) },
                label = { Text(stringResource(R.string.start_time)) },
                leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = stringResource(R.string.cd_time_icon)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("start_time_field_$scheduleNumber"),
                shape = RoundedCornerShape(8.dp),
                placeholder = { Text(stringResource(R.string.start_time_placeholder)) }
            )

            // End Time
            OutlinedTextField(
                value = schedule.endTime,
                onValueChange = { onScheduleChange(schedule.copy(endTime = it)) },
                label = { Text(stringResource(R.string.end_time)) },
                leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = stringResource(R.string.cd_time_icon)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("end_time_field_$scheduleNumber"),
                shape = RoundedCornerShape(8.dp),
                placeholder = { Text(stringResource(R.string.end_time_placeholder)) }
            )

            // Error message
            schedule.error?.let { error ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
