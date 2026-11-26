package com.devsusana.hometutorpro.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.domain.entities.ExceptionType
import com.devsusana.hometutorpro.domain.entities.ScheduleException
import com.devsusana.hometutorpro.presentation.components.schedule_exception.RescheduleSection
import com.devsusana.hometutorpro.presentation.components.schedule_exception.ScheduleClassHeader
import com.devsusana.hometutorpro.presentation.components.schedule_exception.ScheduleExceptionTypeSelector
import com.devsusana.hometutorpro.presentation.components.schedule_exception.ScheduleQuickActions
import com.devsusana.hometutorpro.presentation.components.schedule_exception.ScheduleTimeConflictUtils
import com.devsusana.hometutorpro.presentation.weekly_schedule.WeeklyScheduleItem
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
                    ScheduleTimeConflictUtils.timesOverlap(newStartTime, newEndTime, otherItem.startTime, otherItem.endTime)
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
                    ScheduleTimeConflictUtils.timesOverlap(newStartTime, newEndTime, otherItem.startTime, otherItem.endTime)
            }
        } else null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.schedule_exception_modify_class)) },
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
                    label = { Text(stringResource(R.string.schedule_exception_reason_optional)) },
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
                    Text(stringResource(R.string.schedule_exception_save))
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
                        Text(stringResource(R.string.schedule_exception_remove), color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.schedule_exception_cancel_button))
                }
            }
        }
    )
}
