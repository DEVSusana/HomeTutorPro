package com.devsusana.hometutorpro.presentation.schedule_form

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.devsusana.hometutorpro.presentation.schedule_form.components.ScheduleFormContent
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Screen entry point for schedule creation/editing. Owns UI-only state and delegates to the ViewModel.
 */
@Composable
fun ScheduleFormScreen(
    viewModel: ScheduleFormViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showStartTimePicker by rememberSaveable { mutableStateOf(false) }
    var showEndTimePicker by rememberSaveable { mutableStateOf(false) }
    var isDayOfWeekMenuExpanded by rememberSaveable { mutableStateOf(false) }

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
        onBack = onBack,
        showStartTimePicker = showStartTimePicker,
        onShowStartTimePickerChange = { showStartTimePicker = it },
        showEndTimePicker = showEndTimePicker,
        onShowEndTimePickerChange = { showEndTimePicker = it },
        isDayOfWeekMenuExpanded = isDayOfWeekMenuExpanded,
        onDayOfWeekMenuExpandedChange = { isDayOfWeekMenuExpanded = it }
    )
}