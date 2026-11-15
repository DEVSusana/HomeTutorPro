package com.devsusana.hometutorpro.presentation.components.schedule_exception

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.presentation.utils.DayOfWeekUtils
import java.time.DayOfWeek

/**
 * Displays the student name and original scheduled day/time in the exception dialog.
 */
@Composable
fun ScheduleClassHeader(
    studentName: String,
    dayOfWeek: DayOfWeek,
    startTime: String,
    endTime: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "$studentName - ${DayOfWeekUtils.getLocalizedName(dayOfWeek)}",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = stringResource(R.string.schedule_exception_original, startTime, endTime),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
