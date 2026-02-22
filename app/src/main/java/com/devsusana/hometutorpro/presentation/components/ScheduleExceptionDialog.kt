package com.devsusana.hometutorpro.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.devsusana.hometutorpro.domain.entities.ExceptionType
import com.devsusana.hometutorpro.domain.entities.ScheduleException
import com.devsusana.hometutorpro.presentation.weekly_schedule.WeeklyScheduleItem
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleExceptionDialog(
    item: WeeklyScheduleItem.Regular,
    allRegularSchedules: List<WeeklyScheduleItem.Regular>, // For conflict detection
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
    
    // Day selector dropdown state
    var dayDropdownExpanded by remember { mutableStateOf(false) }

    // Calculate the date for this week's occurrence
    val today = LocalDate.now()
    val startOfWeek = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
    var currentDate = startOfWeek
    while (currentDate.dayOfWeek != item.schedule.dayOfWeek) {
        currentDate = currentDate.plusDays(1)
    }
    val dateTimestamp = currentDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    
    // Create a stable key from allSchedules to prevent unnecessary recomposition
    // This key only changes when actual schedule data changes, not when the list reference changes
    val scheduleKey = remember(allRegularSchedules.size) {
        allRegularSchedules.joinToString("|") { regularItem ->
            val exception = regularItem.exception
            val day = exception?.newDayOfWeek ?: regularItem.schedule.dayOfWeek
            val start = regularItem.startTime
            val end = regularItem.endTime
            val cancelled = if (exception?.type == ExceptionType.CANCELLED) "C" else ""
            "${regularItem.schedule.id}:$day:$start:$end:$cancelled"
        }
    }
    
    // Conflict detection - now uses scheduleKey instead of allSchedules directly
    val hasConflict = remember(newDayOfWeek, newStartTime, newEndTime, exceptionType, scheduleKey) {
        if (exceptionType == ExceptionType.RESCHEDULED && newStartTime.isNotEmpty() && newEndTime.isNotEmpty()) {
            allRegularSchedules.any { otherItem ->
                // Skip checking against itself
                if (otherItem.schedule.id == item.schedule.id) {
                    false
                } else {
                    // Determine effective schedule for the other class
                    val otherException = otherItem.exception
                    
                    // If cancelled, it doesn't cause a conflict
                    if (otherException?.type == ExceptionType.CANCELLED) {
                        false
                    } else {
                        val otherDay = otherException?.newDayOfWeek ?: otherItem.schedule.dayOfWeek
                        val otherStart = otherItem.startTime
                        val otherEnd = otherItem.endTime
                        
                        // Check if same day and overlapping times
                        otherDay == newDayOfWeek &&
                        timesOverlap(
                            newStartTime, newEndTime,
                            otherStart, otherEnd
                        )
                    }
                }
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
                
                val otherDay = otherException?.newDayOfWeek ?: otherItem.schedule.dayOfWeek
                val otherStart = otherItem.startTime
                val otherEnd = otherItem.endTime
                
                otherDay == newDayOfWeek &&
                timesOverlap(
                    newStartTime, newEndTime,
                    otherStart, otherEnd
                )
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
                Text(
                    text = "${item.student.name} - ${com.devsusana.hometutorpro.presentation.utils.DayOfWeekUtils.getLocalizedName(item.schedule.dayOfWeek)}",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Text(
                    text = stringResource(com.devsusana.hometutorpro.R.string.schedule_exception_original, item.schedule.startTime, item.schedule.endTime),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider()

                // Start Class Button (if callback provided)
                if (onStartClass != null) {
                    Button(
                        onClick = {
                            onStartClass(item.student.id, item.student.name)
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

                // Add Extra Class Button
                if (onAddExtraClass != null) {
                   Button(
                        onClick = {
                            onAddExtraClass(item.student.id)
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

                // Exception Type Selection
                Text(stringResource(com.devsusana.hometutorpro.R.string.schedule_exception_action), style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = exceptionType == ExceptionType.CANCELLED,
                        onClick = { exceptionType = ExceptionType.CANCELLED },
                        label = { Text(stringResource(com.devsusana.hometutorpro.R.string.schedule_exception_cancel)) }
                    )
                    FilterChip(
                        selected = exceptionType == ExceptionType.RESCHEDULED,
                        onClick = { exceptionType = ExceptionType.RESCHEDULED },
                        label = { Text(stringResource(com.devsusana.hometutorpro.R.string.schedule_exception_reschedule)) }
                    )
                }

                // Reschedule options (only if rescheduling)
                if (exceptionType == ExceptionType.RESCHEDULED) {
                    Text(stringResource(com.devsusana.hometutorpro.R.string.schedule_exception_new_schedule), style = MaterialTheme.typography.labelLarge)
                    
                    // Day selector
                    ExposedDropdownMenuBox(
                        expanded = dayDropdownExpanded,
                        onExpandedChange = { dayDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = com.devsusana.hometutorpro.presentation.utils.DayOfWeekUtils.getLocalizedName(newDayOfWeek),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(com.devsusana.hometutorpro.R.string.schedule_exception_day_of_week)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = dayDropdownExpanded,
                            onDismissRequest = { dayDropdownExpanded = false }
                        ) {
                            DayOfWeek.values().forEach { day ->
                                DropdownMenuItem(
                                    text = { Text(com.devsusana.hometutorpro.presentation.utils.DayOfWeekUtils.getLocalizedName(day)) },
                                    onClick = {
                                        newDayOfWeek = day
                                        dayDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    // Time Selectors ROW
                    var showNewStartTimePicker by remember { mutableStateOf(false) }
                    var showNewEndTimePicker by remember { mutableStateOf(false) }
                    
                    if (showNewStartTimePicker) {
                        com.devsusana.hometutorpro.presentation.components.TimePickerDialog(
                            initialTime = newStartTime,
                            onDismiss = { showNewStartTimePicker = false },
                            onTimeSelected = { 
                                newStartTime = it
                                showNewStartTimePicker = false
                            }
                        )
                    }
                    
                    if (showNewEndTimePicker) {
                        com.devsusana.hometutorpro.presentation.components.TimePickerDialog(
                            initialTime = newEndTime,
                            onDismiss = { showNewEndTimePicker = false },
                            onTimeSelected = { 
                                newEndTime = it
                                showNewEndTimePicker = false
                            }
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // New Start Time
                        OutlinedButton(
                            onClick = { showNewStartTimePicker = true },
                            modifier = Modifier.weight(1f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(newStartTime)
                        }
                        
                        // New End Time
                        OutlinedButton(
                            onClick = { showNewEndTimePicker = true },
                            modifier = Modifier.weight(1f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(newEndTime)
                        }
                    }
                    
                    // Conflict warning
                    if (hasConflict && conflictingClass != null) {
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
                                    text = stringResource(com.devsusana.hometutorpro.R.string.schedule_exception_conflict_message, conflictingClass.student.name, conflictingClass.startTime, conflictingClass.endTime),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                // Reason
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
                        type = exceptionType ?: ExceptionType.CANCELLED, // Fallback, but button is disabled if null
                        originalScheduleId = item.schedule.id,
                        newStartTime = if (exceptionType == ExceptionType.RESCHEDULED) newStartTime else "",
                        newEndTime = if (exceptionType == ExceptionType.RESCHEDULED) newEndTime else "",
                        newDayOfWeek = if (exceptionType == ExceptionType.RESCHEDULED && newDayOfWeek != item.schedule.dayOfWeek) newDayOfWeek else null,
                        reason = reason
                    )
                    onSave(exception)
                },
                enabled = exceptionType != null && (!hasConflict || exceptionType == ExceptionType.CANCELLED)
            ) {
                Text(stringResource(com.devsusana.hometutorpro.R.string.schedule_exception_save))
            }
        },
        dismissButton = {
            Row {
                // Delete button if exception exists
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
