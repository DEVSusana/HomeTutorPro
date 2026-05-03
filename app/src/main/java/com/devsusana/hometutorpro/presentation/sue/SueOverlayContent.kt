package com.devsusana.hometutorpro.presentation.sue

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.core.sue.SpeechState
import com.devsusana.hometutorpro.presentation.sue.components.SueListeningOverlay
import com.devsusana.hometutorpro.presentation.sue.components.SueResponseBubble

/**
 * Pure presentation composable for Sue's conversational overlay.
 *
 * Displays the appropriate UI based on the current speech state:
 * - Listening: Audio waveform + partial transcription.
 * - Processing: User's transcription + typing indicator.
 * - Speaking: User's transcription + Sue's response.
 * - Error: Error message.
 *
 * This composable is completely stateless — all state and callbacks
 * are received as parameters.
 *
 * @param speechState Current state of the speech subsystem.
 * @param partialTranscription Real-time partial transcription text.
 * @param finalTranscription Completed transcription sent to the agent.
 * @param agentResponse Sue's textual response.
 * @param errorMessage Optional error message.
 * @param onCancel Callback to dismiss the overlay.
 */
@Composable
fun SueOverlayContent(
    speechState: SpeechState,
    partialTranscription: String,
    finalTranscription: String,
    agentResponse: String,
    errorMessage: String?,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(bottom = 24.dp)
    ) {
        // Drag handle area (swipe down to dismiss)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        if (dragAmount > 8f) { // Threshold for downward swipe
                            onCancel()
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(12.dp))
                Spacer(
                    modifier = Modifier
                        .height(4.dp)
                        .fillMaxWidth(0.12f)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        val configuration = LocalConfiguration.current
        val maxHeight = (configuration.screenHeightDp * 0.5f).dp
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .verticalScroll(rememberScrollState())
        ) {
            // Listening state — waveform + partial transcription
            SueListeningOverlay(
                partialTranscription = partialTranscription,
                isVisible = speechState == SpeechState.LISTENING,
                onCancel = onCancel
            )

            // User's final transcription (shown during processing/speaking)
            if (finalTranscription.isNotBlank() && speechState != SpeechState.LISTENING) {
                UserTranscriptionBubble(
                    text = finalTranscription,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Sue's response bubble
            SueResponseBubble(
                response = agentResponse,
                isTyping = speechState == SpeechState.PROCESSING,
                isVisible = speechState == SpeechState.PROCESSING ||
                        speechState == SpeechState.SPEAKING ||
                        (speechState == SpeechState.IDLE && agentResponse.isNotBlank())
            )

            // Error message
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            // Add padding at the bottom so content isn't hidden behind the FAB
            Spacer(modifier = Modifier.height(88.dp))
        }
    }
}

/**
 * Displays the user's spoken transcription as a right-aligned chat bubble.
 */
@Composable
private fun UserTranscriptionBubble(
    text: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.End,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

@Preview(showBackground = true, name = "Overlay - Listening")
@Composable
private fun SueOverlayContentListeningPreview() {
    MaterialTheme {
        SueOverlayContent(
            speechState = SpeechState.LISTENING,
            partialTranscription = "¿Cuántos alumnos...",
            finalTranscription = "",
            agentResponse = "",
            errorMessage = null,
            onCancel = {}
        )
    }
}

@Preview(showBackground = true, name = "Overlay - Processing")
@Composable
private fun SueOverlayContentProcessingPreview() {
    MaterialTheme {
        SueOverlayContent(
            speechState = SpeechState.PROCESSING,
            partialTranscription = "",
            finalTranscription = "¿Cuántos alumnos tengo?",
            agentResponse = "",
            errorMessage = null,
            onCancel = {}
        )
    }
}

@Preview(showBackground = true, name = "Overlay - Response")
@Composable
private fun SueOverlayContentResponsePreview() {
    MaterialTheme {
        SueOverlayContent(
            speechState = SpeechState.IDLE,
            partialTranscription = "",
            finalTranscription = "¿Cuántos alumnos tengo?",
            agentResponse = "Tienes 12 alumnos activos actualmente. ¿Necesitas saber algo más?",
            errorMessage = null,
            onCancel = {}
        )
    }
}
