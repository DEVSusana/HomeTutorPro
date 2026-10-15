package com.devsusana.hometutorpro.presentation.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devsusana.hometutorpro.presentation.components.FeedbackDialog

/**
 * Screen entry point for user notes. Connects [NotesViewModel] with [NotesContent].
 */
@Composable
fun NotesScreen(
    onNavigateBack: () -> Unit,
    viewModel: NotesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    NotesContent(
        state = state,
        onNavigateBack = onNavigateBack,
        onNotesChange = viewModel::onNotesChange,
        onSaveNotes = viewModel::saveNotes
    )

    if (state.isLoading) {
        Dialog(onDismissRequest = {}) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(24.dp)
            ) {
                CircularProgressIndicator()
            }
        }
    }

    val successMessage = state.successMessage
    if (successMessage != null) {
        FeedbackDialog(
            isSuccess = true,
            message = { Text(successMessage) },
            onDismiss = viewModel::clearFeedback
        )
    }

    val errorMessage = state.errorMessage
    if (errorMessage != null) {
        FeedbackDialog(
            isSuccess = false,
            message = { Text(errorMessage) },
            onDismiss = viewModel::clearFeedback
        )
    }
}
