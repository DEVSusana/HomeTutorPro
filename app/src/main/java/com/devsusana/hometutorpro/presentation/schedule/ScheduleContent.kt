package com.devsusana.hometutorpro.presentation.schedule

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.devsusana.hometutorpro.presentation.schedule.components.ScheduleListContent
import com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme

/**
 * Pure presentation composable for schedule list screen.
 */
@Composable
fun ScheduleContent(
    state: ScheduleState,
    onAddScheduleClick: () -> Unit,
    onDeleteSchedule: (String) -> Unit,
    onBack: () -> Unit
) {
    ScheduleListContent(
        state = state,
        onAddScheduleClick = onAddScheduleClick,
        onDeleteSchedule = onDeleteSchedule,
        onBack = onBack
    )
}

@Preview(showBackground = true)
@Composable
private fun ScheduleContentPreview() {
    HomeTutorProTheme {
        ScheduleContent(
            state = ScheduleState(),
            onAddScheduleClick = {},
            onDeleteSchedule = {},
            onBack = {}
        )
    }
}
