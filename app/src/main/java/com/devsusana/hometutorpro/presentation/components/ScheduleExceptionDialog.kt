package com.devsusana.hometutorpro.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.devsusana.hometutorpro.domain.entities.ExceptionType
import com.devsusana.hometutorpro.domain.entities.ScheduleException
import com.devsusana.hometutorpro.presentation.weekly_schedule.WeeklyScheduleItem
import java.time.DayOfWeek
import java.time.ZoneId

/**
 * Dialog for creating, modifying, or cancelling a class schedule exception.
 */
@Composable
fun ScheduleExceptionDialog(
    item: WeeklyScheduleItem.Regular,
    allRegularSchedules: List<WeeklyScheduleItem.Regular>, // For conflict detection
    isSaving: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (ScheduleException) -> Unit,
    onDelete: ((String, String) -> Unit)? = null,
    onStartClass: ((String, String) -> Unit)? = null, // studentId, studentName
    onAddExtraClass: ((String) -> Unit)? = null // studentId
) {
    var exceptionType: ExceptionType? by remember {
        mutableStateOf(item.exception?.type)
    }
    var newDayOfWeek by remember {
        mutableStateOf(item.exception?.newDayOfWeek ?: item.schedule.dayOfWeek)
    }
    var newStartTime by remember {
        mutableStateOf(item.exception?.newStartTime ?: item.schedule.startTime)
    }
    var newEndTime by remember {
        mutableStateOf(item.exception?.newEndTime ?: item.schedule.endTime)
    }
    var reason by remember { mutableStateOf(item.exception?.reason ?: "") }

    val dateTimestamp = item.occurrence.date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    val scheduleKey = remember(allRegularSchedules) {
        allRegularSchedules.joinToString("|") { regularItem ->
            val exception = regularItem.exception
            val day = exception?.newDayOfWeek ?: regularItem.schedule.dayOfWeek
            val start = regularItem.startTime
            val end = regularItem.endTime
            val status = when (exception?.type) {
                ExceptionType.CANCELLED -> "C"
                ExceptionType.RESCHEDULED -> "R"
                else -> "N"
            }
            "${regularItem.schedule.id}:$day:$start:$end:$status"
        }
    }

    val hasConflict = remember(newDayOfWeek, newStartTime, newEndTime, exceptionType, scheduleKey) {
        if (exceptionType == ExceptionType.RESCHEDULED && newStartTime.isNotEmpty() && newEndTime.isNotEmpty()) {
            allRegularSchedules.any { otherItem ->
                if (otherItem.schedule.id == item.schedule.id) return@any false

                val otherException = otherItem.exception
                if (otherException?.type == ExceptionType.CANCELLED) return@any false

                val otherEffectiveDay = otherException?.newDayOfWeek ?: otherItem.schedule.dayOfWeek

                otherEffectiveDay == newDayOfWeek &&
                    timesOverlap(newStartTime, newEndTime, otherItem.startTime, otherItem.endTime)
            }
        } else {
            false
        }
    }

    val conflictingClass = remember(hasConflict, scheduleKey) {
        if (hasConflict) {
            allRegularSchedules.find { otherItem ->
                if (otherItem.schedule.id == item.schedule.id) return@find false

                val otherException = otherItem.exception
                if (otherException?.type == ExceptionType.CANCELLED) return@find false

                val otherEffectiveDay = otherException?.newDayOfWeek ?: otherItem.schedule.dayOfWeek

                otherEffectiveDay == newDayOfWeek &&
                    timesOverlap(newStartTime, newEndTime, otherItem.startTime, otherItem.endTime)
            }
        } else null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(com.devsusana.hometutorpro.R.string.schedule_exception_modify_class)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ScheduleClassHeader(
                    studentName = item.student.name,
                    dayOfWeek = item.schedule.dayOfWeek,
                    startTime = item.schedule.startTime,
                    endTime = item.schedule.endTime
                )

                HorizontalDivider()

                ScheduleQuickActions(
                    studentId = item.student.id,
                    studentName = item.student.name,
                    onStartClass = onStartClass,
                    onAddExtraClass = onAddExtraClass,
                    onDismiss = onDismiss
                )

                ScheduleExceptionTypeSelector(
                    exceptionType = exceptionType,
                    onTypeSelected = { exceptionType = it }
                )

                if (exceptionType == ExceptionType.RESCHEDULED) {
                    RescheduleSection(
                        newDayOfWeek = newDayOfWeek,
                        onDayOfWeekChange = { newDayOfWeek = it },
                        newStartTime = newStartTime,
                        newEndTime = newEndTime,
                        onStartTimeChange = { newStartTime = it },
                        onEndTimeChange = { newEndTime = it },
                        hasConflict = hasConflict,
                        conflictingClass = conflictingClass
                    )
                }

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text(stringResource(com.devsusana.hometutorpro.R.string.schedule_exception_reason_optional)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val exception = ScheduleException(
                        id = item.exception?.id ?: "",
                        studentId = item.student.id,
                        date = dateTimestamp,
                        type = exceptionType ?: ExceptionType.CANCELLED,
                        originalScheduleId = item.schedule.id,
                        newStartTime = if (exceptionType == ExceptionType.RESCHEDULED) newStartTime else "",
                        newEndTime = if (exceptionType == ExceptionType.RESCHEDULED) newEndTime else "",
                        newDayOfWeek = if (exceptionType == ExceptionType.RESCHEDULED && newDayOfWeek != item.schedule.dayOfWeek) newDayOfWeek else null,
                        reason = reason
                    )
                    onSave(exception)
                },
                enabled = !isSaving && exceptionType != null && (!hasConflict || exceptionType == ExceptionType.CANCELLED)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(com.devsusana.hometutorpro.R.string.schedule_exception_save))
                }
            }
        },
        dismissButton = {
            Row {
                val existingException = item.exception
                if (existingException != null && onDelete != null) {
                    TextButton(
                        onClick = {
                            onDelete(
                                existingException.id,
                                item.student.id
                            )
                            onDismiss()
                        }
                    ) {
                        Text(stringResource(com.devsusana.hometutorpro.R.string.schedule_exception_remove), color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(com.devsusana.hometutorpro.R.string.schedule_exception_cancel_button))
                }
            }
        }
    )
}

@Composable
private fun ScheduleClassHeader(
    studentName: String,
    dayOfWeek: DayOfWeek,
    startTime: String,
    endTime: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "$studentName - ${com.devsusana.hometutorpro.presentation.utils.DayOfWeekUtils.getLocalizedName(dayOfWeek)}",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = stringResource(com.devsusana.hometutorpro.R.string.schedule_exception_original, startTime, endTime),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ScheduleQuickActions(
    studentId: String,
    studentName: String,
    onStartClass: ((String, String) -> Unit)?,
    onAddExtraClass: ((String) -> Unit)?,
    onDismiss: () -> Unit
) {
    if (onStartClass != null) {
        Button(
            onClick = {
                onStartClass(studentId, studentName)
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(com.devsusana.hometutorpro.R.string.student_detail_start_class))
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))
    }

    if (onAddExtraClass != null) {
        Button(
            onClick = {
                onAddExtraClass(studentId)
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
        ) {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(com.devsusana.hometutorpro.R.string.student_detail_add_extra_class))
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun ScheduleExceptionTypeSelector(
    exceptionType: ExceptionType?,
    onTypeSelected: (ExceptionType) -> Unit
) {
    Text(
        stringResource(com.devsusana.hometutorpro.R.string.schedule_exception_action),
        style = MaterialTheme.typography.labelLarge
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val selectedStateDescription = stringResource(com.devsusana.hometutorpro.R.string.cd_state_selected)
        val notSelectedStateDescription = stringResource(com.devsusana.hometutorpro.R.string.cd_state_not_selected)
        FilterChip(
            selected = exceptionType == ExceptionType.CANCELLED,
            onClick = { onTypeSelected(ExceptionType.CANCELLED) },
            label = { Text(stringResource(com.devsusana.hometutorpro.R.string.schedule_exception_cancel)) },
            modifier = Modifier.semantics {
                stateDescription = if (exceptionType == ExceptionType.CANCELLED) {
                    selectedStateDescription
                } else {
                    notSelectedStateDescription
                }
            }
        )
        FilterChip(
            selected = exceptionType == ExceptionType.RESCHEDULED,
            onClick = { onTypeSelected(ExceptionType.RESCHEDULED) },
            label = { Text(stringResource(com.devsusana.hometutorpro.R.string.schedule_exception_reschedule)) },
            modifier = Modifier.semantics {
                stateDescription = if (exceptionType == ExceptionType.RESCHEDULED) {
                    selectedStateDescription
                } else {
                    notSelectedStateDescription
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayOfWeekDropdownSelector(
    selectedDay: DayOfWeek,
    onDaySelected: (DayOfWeek) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = com.devsusana.hometutorpro.presentation.utils.DayOfWeekUtils.getLocalizedName(selectedDay),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(com.devsusana.hometutorpro.R.string.schedule_exception_day_of_week)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DayOfWeek.entries.forEach { day ->
                DropdownMenuItem(
                    text = { Text(com.devsusana.hometutorpro.presentation.utils.DayOfWeekUtils.getLocalizedName(day)) },
                    onClick = {
                        onDaySelected(day)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun RescheduleTimePickers(
    startTime: String,
    endTime: String,
    onStartTimeChange: (String) -> Unit,
    onEndTimeChange: (String) -> Unit
) {
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    if (showStartTimePicker) {
        TimePickerDialog(
            initialTime = startTime,
            onDismiss = { showStartTimePicker = false },
            onTimeSelected = {
                onStartTimeChange(it)
                showStartTimePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            initialTime = endTime,
            onDismiss = { showEndTimePicker = false },
            onTimeSelected = {
                onEndTimeChange(it)
                showEndTimePicker = false
            }
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val startTimeDesc = stringResource(com.devsusana.hometutorpro.R.string.cd_select_start_time, startTime)
        val endTimeDesc = stringResource(com.devsusana.hometutorpro.R.string.cd_select_end_time, endTime)

        OutlinedButton(
            onClick = { showStartTimePicker = true },
            modifier = Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) {
                    contentDescription = startTimeDesc
                },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(startTime)
        }

        OutlinedButton(
            onClick = { showEndTimePicker = true },
            modifier = Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) {
                    contentDescription = endTimeDesc
                },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(endTime)
        }
    }
}

@Composable
private fun ScheduleConflictWarning(conflictingClass: WeeklyScheduleItem.Regular) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(com.devsusana.hometutorpro.R.string.schedule_exception_conflict_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = stringResource(
                    com.devsusana.hometutorpro.R.string.schedule_exception_conflict_message,
                    conflictingClass.student.name,
                    conflictingClass.startTime,
                    conflictingClass.endTime
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun RescheduleSection(
    newDayOfWeek: DayOfWeek,
    onDayOfWeekChange: (DayOfWeek) -> Unit,
    newStartTime: String,
    newEndTime: String,
    onStartTimeChange: (String) -> Unit,
    onEndTimeChange: (String) -> Unit,
    hasConflict: Boolean,
    conflictingClass: WeeklyScheduleItem.Regular?
) {
    Text(
        stringResource(com.devsusana.hometutorpro.R.string.schedule_exception_new_schedule),
        style = MaterialTheme.typography.labelLarge
    )
    DayOfWeekDropdownSelector(
        selectedDay = newDayOfWeek,
        onDaySelected = onDayOfWeekChange
    )
    RescheduleTimePickers(
        startTime = newStartTime,
        endTime = newEndTime,
        onStartTimeChange = onStartTimeChange,
        onEndTimeChange = onEndTimeChange
    )
    if (hasConflict && conflictingClass != null) {
        ScheduleConflictWarning(conflictingClass = conflictingClass)
    }
}

// Helper function to check if two time ranges overlap
private fun timesOverlap(start1: String, end1: String, start2: String, end2: String): Boolean {
    return try {
        val s1 = timeToMinutes(start1)
        val e1 = timeToMinutes(end1)
        val s2 = timeToMinutes(start2)
        val e2 = timeToMinutes(end2)

        // Check if ranges overlap
        s1 < e2 && s2 < e1
    } catch (e: Exception) {
        false
    }
}

private fun timeToMinutes(time: String): Int {
    val parts = time.split(":")
    return parts[0].toInt() * 60 + parts[1].toInt()
}
