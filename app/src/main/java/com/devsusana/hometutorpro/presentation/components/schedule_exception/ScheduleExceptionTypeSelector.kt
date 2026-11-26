package com.devsusana.hometutorpro.presentation.components.schedule_exception

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.domain.entities.ExceptionType

/**
 * Filter chips allowing the user to choose between CANCELLED or RESCHEDULED exception types.
 *
 * @param exceptionType The currently selected exception type. Can be null if no type is selected.
 * @param onTypeSelected Callback invoked when an exception type is selected.
 */
@Composable
fun ScheduleExceptionTypeSelector(
    exceptionType: ExceptionType?,
    onTypeSelected: (ExceptionType) -> Unit
) {
    Text(
        stringResource(R.string.schedule_exception_action),
        style = MaterialTheme.typography.labelLarge
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val selectedStateDescription = stringResource(R.string.cd_state_selected)
        val notSelectedStateDescription = stringResource(R.string.cd_state_not_selected)
        val isCancelled = exceptionType == ExceptionType.CANCELLED
        val isRescheduled = exceptionType == ExceptionType.RESCHEDULED
        FilterChip(
            selected = isCancelled,
            onClick = { onTypeSelected(ExceptionType.CANCELLED) },
            label = { Text(stringResource(R.string.schedule_exception_cancel)) },
            modifier = Modifier.chipStateDescription(isCancelled, selectedStateDescription, notSelectedStateDescription)
        )
        FilterChip(
            selected = isRescheduled,
            onClick = { onTypeSelected(ExceptionType.RESCHEDULED) },
            label = { Text(stringResource(R.string.schedule_exception_reschedule)) },
            modifier = Modifier.chipStateDescription(isRescheduled, selectedStateDescription, notSelectedStateDescription)
        )
    }
}

private fun Modifier.chipStateDescription(
    isSelected: Boolean,
    selectedDesc: String,
    notSelectedDesc: String
): Modifier = semantics {
    stateDescription = if (isSelected) selectedDesc else notSelectedDesc
}
