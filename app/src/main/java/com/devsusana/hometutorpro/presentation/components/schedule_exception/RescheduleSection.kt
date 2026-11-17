package com.devsusana.hometutorpro.presentation.components.schedule_exception

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.presentation.weekly_schedule.WeeklyScheduleItem
import java.time.DayOfWeek

/**
 * Section grouping the day-of-week dropdown, time pickers, and potential conflict warnings
 * when rescheduling a class.
 *
 * @param newDayOfWeek The proposed new day of week for the rescheduled class.
 * @param onDayOfWeekChange Callback invoked when the day of week changes.
 * @param newStartTime Proposed new class start time.
 * @param newEndTime Proposed new class end time.
 * @param onStartTimeChange Callback invoked when the start time changes.
 * @param onEndTimeChange Callback invoked when the end time changes.
 * @param hasConflict Whether a scheduling conflict is detected with another class.
 * @param conflictingClass The conflicting schedule item, if any.
 */
@Composable
fun RescheduleSection(
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
        stringResource(R.string.schedule_exception_new_schedule),
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
