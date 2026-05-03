package com.devsusana.hometutorpro.presentation.sue

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.devsusana.hometutorpro.core.sue.SpeechState

/**
 * Full-screen overlay container for Sue's conversational interface.
 *
 * Displays a semi-transparent scrim that can be tapped to dismiss,
 * with the [SueOverlayContent] sliding in from the bottom.
 *
 * This is the "Screen" level composable that connects to the ViewModel
 * state and delegates to the pure-presentation [SueOverlayContent].
 *
 * @param uiState The current [SueUiState] from the ViewModel.
 * @param onDismiss Callback when the overlay should be dismissed.
 */
@Composable
fun SueOverlay(
    uiState: SueUiState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = uiState.isOverlayVisible,
        enter = slideInVertically { it },
        exit = slideOutVertically { it }
    ) {
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    // Only dismiss on scrim tap when idle
                    if (uiState.speechState == SpeechState.IDLE) {
                        onDismiss()
                    }
                }
        ) {
            // Prevent clicks on the content from dismissing
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { /* consume click */ }
            ) {
                SueOverlayContent(
                    speechState = uiState.speechState,
                    partialTranscription = uiState.partialTranscription,
                    finalTranscription = uiState.finalTranscription,
                    agentResponse = uiState.agentResponse,
                    errorMessage = uiState.errorMessage,
                    onCancel = onDismiss
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Sue Overlay - Visible")
@Composable
private fun SueOverlayPreview() {
    MaterialTheme {
        SueOverlay(
            uiState = SueUiState(
                speechState = SpeechState.IDLE,
                isOverlayVisible = true,
                finalTranscription = "¿Cuántos alumnos tengo?",
                agentResponse = "Tienes 12 alumnos activos."
            ),
            onDismiss = {}
        )
    }
}
