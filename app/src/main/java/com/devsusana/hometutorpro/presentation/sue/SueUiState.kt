package com.devsusana.hometutorpro.presentation.sue

import com.devsusana.hometutorpro.core.sue.SpeechState

/**
 * UI state for the Sue AI agent overlay.
 *
 * @param speechState Current state of the speech subsystem.
 * @param partialTranscription Real-time partial transcription while listening.
 * @param finalTranscription The completed transcription sent to the agent.
 * @param agentResponse The textual response from Sue.
 * @param isOverlayVisible Whether the conversational overlay is shown.
 * @param isModelLoaded Whether the Gemma model is loaded and ready.
 * @param errorMessage An optional error message to display to the user.
 */
data class SueUiState(
    val speechState: SpeechState = SpeechState.IDLE,
    val partialTranscription: String = "",
    val finalTranscription: String = "",
    val agentResponse: String = "",
    val isOverlayVisible: Boolean = false,
    val isModelLoaded: Boolean = false,
    val errorMessage: String? = null
)
