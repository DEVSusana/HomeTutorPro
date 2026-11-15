package com.devsusana.hometutorpro.presentation.components.schedule_exception

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.presentation.weekly_schedule.WeeklyScheduleItem

/**
 * Warning card displayed when a proposed rescheduled time overlaps with another class.
 */
@Composable
fun ScheduleConflictWarning(conflictingClass: WeeklyScheduleItem.Regular) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.schedule_exception_conflict_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = stringResource(
                    R.string.schedule_exception_conflict_message,
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
