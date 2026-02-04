package com.devsusana.hometutorpro.presentation.schedule_form

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.devsusana.hometutorpro.presentation.schedule_form.components.ScheduleFormContent

@Composable
fun ScheduleFormScreen(
    viewModel: ScheduleFormViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    // Navigate back when saved
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            onBack()
        }
    }

    ScheduleFormContent(
        state = state,
        onDayOfWeekChange = viewModel::onDayOfWeekChange,
        onStartTimeChange = viewModel::onStartTimeChange,
        onEndTimeChange = viewModel::onEndTimeChange,
        onSaveSchedule = viewModel::saveSchedule,
        onBack = onBack
    )
}
