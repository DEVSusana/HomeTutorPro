package com.devsusana.hometutorpro.presentation.student_detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.devsusana.hometutorpro.domain.entities.PaymentType
import com.devsusana.hometutorpro.presentation.student_detail.components.StudentDetailContent
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Screen entry point for student details. Manages UI-only state and delegates events to the ViewModel.
 */
@Composable
fun StudentDetailScreen(
    viewModel: StudentDetailViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val student = state.student
    val isNewStudent = student?.id?.isEmpty() == true || student?.id == "new"

    var isLocalEditMode by rememberSaveable(student?.id) { mutableStateOf(isNewStudent) }
    val isEditMode = isLocalEditMode || state.isBalanceEditable

    var showPaymentDialog by rememberSaveable { mutableStateOf(false) }
    var selectedPaymentType by rememberSaveable { mutableStateOf<PaymentType?>(null) }
    var showStartClassDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

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
        onBack = onBack,
        isEditMode = isEditMode,
        onToggleEditMode = { isLocalEditMode = !isLocalEditMode },
        onSetEditMode = { isLocalEditMode = it },
        showPaymentDialog = showPaymentDialog,
        selectedPaymentType = selectedPaymentType,
        onPaymentClick = {
            selectedPaymentType = PaymentType.EFFECTIVE
            showPaymentDialog = true
        },
        onDismissPaymentDialog = {
            showPaymentDialog = false
            selectedPaymentType = null
        },
        showStartClassDialog = showStartClassDialog,
        onStartClassClick = { showStartClassDialog = true },
        onDismissStartClassDialog = { showStartClassDialog = false },
        showDeleteDialog = showDeleteDialog,
        onShowDeleteDialog = { showDeleteDialog = true },
        onDismissDeleteDialog = { showDeleteDialog = false }
    )
}