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
        onEvent = viewModel::onEvent,
        onBack = onBack
    )
}
