package com.devsusana.hometutorpro.presentation.student_detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.devsusana.hometutorpro.presentation.student_detail.components.StudentDetailContent

@Composable
fun StudentDetailScreen(
    viewModel: StudentDetailViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.isStudentSaved) {
        if (state.isStudentSaved) {
            onBack()
        }
    }

    LaunchedEffect(state.isStudentDeleted) {
        if (state.isStudentDeleted) {
            onBack()
        }
    }

    StudentDetailContent(
        state = state,
        onStudentChange = viewModel::onStudentChange,
        onSaveStudent = viewModel::saveStudent,
        onDeleteStudent = viewModel::deleteStudent,
        onDeleteSchedule = viewModel::deleteSchedule,
        onRegisterPayment = viewModel::registerPayment,
        onBulkScheduleModeToggle = viewModel::onBulkScheduleModeToggle,
        onBulkSchedulesChange = viewModel::onBulkSchedulesChange,
        onSaveBulkSchedules = viewModel::saveBulkSchedules,
        onStartClass = viewModel::startClass,
        onBack = onBack,
        onClearFeedback = viewModel::clearFeedback,
        onFileSelected = viewModel::onFileSelected,
        onShareResource = viewModel::shareResource,
        onDeleteSharedResource = viewModel::deleteSharedResource,
        onShareDialogDismiss = viewModel::onShareDialogDismiss,
        onShareNotesChange = viewModel::onShareNotesChange,
        onBalanceChange = viewModel::onBalanceChange,
        onBalanceEditToggle = viewModel::onBalanceEditToggle,
        onTabChange = viewModel::onTabChange,
        onPriceChange = viewModel::onPriceChange,
        onContinue = viewModel::continueToNextStep
    )
}
