package com.devsusana.hometutorpro.presentation.schedule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.devsusana.hometutorpro.presentation.components.FeedbackDialog
import com.devsusana.hometutorpro.presentation.schedule.components.ScheduleListContent

@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel = hiltViewModel(),
    onAddScheduleClick: () -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    ScheduleListContent(
        state = state,
        onAddScheduleClick = onAddScheduleClick,
        onDeleteSchedule = viewModel::deleteSchedule,
        onBack = onBack
    )

    if (state.successMessage != null) {
        FeedbackDialog(
            isSuccess = true,
            message = {
                when (val message = state.successMessage!!) {
                    is Int -> androidx.compose.material3.Text(androidx.compose.ui.res.stringResource(id = message))
                    is String -> androidx.compose.material3.Text(message)
                }
            },
            onDismiss = viewModel::clearFeedback
        )
    }

    if (state.errorMessage != null) {
        FeedbackDialog(
            isSuccess = false,
            message = {
                when (val message = state.errorMessage!!) {
                    is Int -> androidx.compose.material3.Text(androidx.compose.ui.res.stringResource(id = message))
                    is String -> androidx.compose.material3.Text(message)
                }
            },
            onDismiss = viewModel::clearFeedback
        )
    }
}
