package com.devsusana.hometutorpro.presentation.sue

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.devsusana.hometutorpro.domain.entities.SpeechState
import com.devsusana.hometutorpro.domain.entities.SuePendingAction
import com.devsusana.hometutorpro.presentation.sue.components.SueListeningOverlay
import com.devsusana.hometutorpro.presentation.sue.components.SueResponseBubble
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.fillMaxSize

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
 * @param isModelLoading Whether the AI LLM model is currently loading into memory.
 */
@Composable
fun SueOverlayContent(
    speechState: SpeechState,
    partialTranscription: String,
    finalTranscription: String,
    agentResponse: String,
    errorMessage: String?,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    pendingAction: SuePendingAction? = null,
    isModelLoading: Boolean = false,
    onConfirmAction: () -> Unit = {},
    onCancelAction: () -> Unit = {}
) {
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.98f)
        )
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
        color = Color.Transparent,
        tonalElevation = 8.dp,
        shadowElevation = 16.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .background(gradientBrush)
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
                Spacer(modifier = Modifier.height(16.dp))
                Spacer(
                    modifier = Modifier
                        .height(5.dp)
                        .fillMaxWidth(0.15f)
                        .clip(RoundedCornerShape(2.5.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                )
                Spacer(modifier = Modifier.height(16.dp))
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
            // Model Loading State Indicator
            if (isModelLoading) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        text = stringResource(R.string.sue_loading_assistant_brain),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Listening state — waveform + partial transcription
            SueListeningOverlay(
                partialTranscription = partialTranscription,
                isVisible = speechState == SpeechState.LISTENING && !isModelLoading,
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

            // Confirmation buttons for pending actions
            if (pendingAction != null && speechState == SpeechState.IDLE) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancelAction,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    Button(
                        onClick = onConfirmAction,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(stringResource(R.string.confirm))
                    }
                }
            }

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
            
            Spacer(modifier = Modifier.height(88.dp))
        }
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
        Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp),
            shadowElevation = 2.dp,
            modifier = Modifier.padding(end = 8.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
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
