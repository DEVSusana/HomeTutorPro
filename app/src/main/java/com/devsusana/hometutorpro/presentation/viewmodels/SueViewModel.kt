package com.devsusana.hometutorpro.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devsusana.hometutorpro.core.sue.SpeechManager
import com.devsusana.hometutorpro.core.sue.SpeechState
import com.devsusana.hometutorpro.core.sue.SueAgent
import com.devsusana.hometutorpro.core.sue.inference.MediaPipeModelManager
import com.devsusana.hometutorpro.presentation.sue.SueUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Sue AI agent feature.
 *
 * Orchestrates the full pipeline:
 * 1. SpeechRecognizer → User transcription.
 * 2. SueAgent → Builds context-enriched prompt with tools.
 * 3. MediaPipeModelManager → On-device LLM inference.
 * 4. TextToSpeech → Speaks the response.
 *
 * Scoped at the navigation host level so Sue's state persists across screens.
 */
@HiltViewModel
class SueViewModel @Inject constructor(
    private val speechManager: SpeechManager,
    private val sueAgent: SueAgent,
    private val modelManager: MediaPipeModelManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SueUiState())
    /** UI state for Sue's overlay and FAB. */
    val uiState: StateFlow<SueUiState> = _uiState.asStateFlow()

    init {
        collectSpeechState()
        collectTranscriptions()
        collectPartialTranscriptions()
        collectErrors()
        collectModelState()
        preloadModel()
    }

    /**
     * Handles FAB tap. Behavior depends on current state:
     * - Idle → Start listening + show overlay.
     * - Listening → Stop listening.
     * - Speaking → Stop speaking.
     * - Other → Show overlay if hidden.
     */
    fun onFabClick() {
        when (_uiState.value.speechState) {
            SpeechState.IDLE -> {
                speechManager.initializeTts()
                _uiState.update {
                    it.copy(
                        isOverlayVisible = true,
                        errorMessage = null,
                        partialTranscription = "",
                        finalTranscription = "",
                        agentResponse = ""
                    )
                }
                speechManager.startListening()
            }
            SpeechState.LISTENING -> {
                speechManager.stopListening()
            }
            SpeechState.SPEAKING -> {
                speechManager.stopSpeaking()
            }
            else -> {
                _uiState.update { it.copy(isOverlayVisible = true) }
            }
        }
    }

    /**
     * Dismisses the overlay and stops any active operations.
     */
    fun onDismiss() {
        speechManager.stopListening()
        speechManager.stopSpeaking()
        _uiState.update {
            it.copy(
                isOverlayVisible = false,
                errorMessage = null
            )
        }
    }

    /**
     * Clears any displayed error message.
     */
    fun onErrorDismissed() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.release()
        modelManager.release()
    }

    private fun preloadModel() {
        viewModelScope.launch {
            modelManager.loadModel()
        }
    }

    private fun collectSpeechState() {
        viewModelScope.launch {
            speechManager.state.collect { state ->
                _uiState.update { it.copy(speechState = state) }
            }
        }
    }

    private fun collectTranscriptions() {
        viewModelScope.launch {
            speechManager.transcriptions.collect { transcription ->
                _uiState.update {
                    it.copy(
                        finalTranscription = transcription,
                        partialTranscription = ""
                    )
                }
                processWithAgent(transcription)
            }
        }
    }

    private fun collectPartialTranscriptions() {
        viewModelScope.launch {
            speechManager.partialTranscriptions.collect { partial ->
                _uiState.update { it.copy(partialTranscription = partial) }
            }
        }
    }

    private fun collectErrors() {
        viewModelScope.launch {
            speechManager.errors.collect { error ->
                _uiState.update { it.copy(errorMessage = error) }
            }
        }
    }

    private fun collectModelState() {
        viewModelScope.launch {
            modelManager.isModelLoaded.collect { loaded ->
                _uiState.update { it.copy(isModelLoaded = loaded) }
            }
        }
    }

    /**
     * Processes the user's transcription through the Sue agent pipeline:
     * 1. SueAgent gathers relevant tool context and builds a prompt.
     * 2. MediaPipeModelManager runs inference on the prompt.
     * 3. The response is displayed and spoken via TTS.
     *
     * If the model is not loaded, falls back to a tool-only response
     * (no LLM generation, just raw data from tools).
     */
    private suspend fun processWithAgent(transcription: String) {
        try {
            val prompt = sueAgent.buildPromptWithContext(transcription)

            val response = if (_uiState.value.isModelLoaded) {
                modelManager.generateResponse(prompt)
            } else {
                // Fallback: extract the data section from the prompt
                extractDataFromPrompt(prompt)
            }

            _uiState.update { it.copy(agentResponse = response) }
            speechManager.speak(response)
        } catch (e: Exception) {
            val errorResponse = "Lo siento, hubo un error al procesar tu consulta."
            _uiState.update {
                it.copy(
                    agentResponse = errorResponse,
                    errorMessage = e.message
                )
            }
            speechManager.speak(errorResponse)
        }
    }

    /**
     * Fallback for when the LLM model is not loaded.
     * Extracts the tool data section from the prompt and presents it directly.
     */
    private fun extractDataFromPrompt(prompt: String): String {
        val dataStart = prompt.indexOf("--- DATOS DISPONIBLES ---")
        val dataEnd = prompt.indexOf("--- FIN DE DATOS ---")

        return if (dataStart >= 0 && dataEnd >= 0) {
            val data = prompt.substring(dataStart + 25, dataEnd).trim()
            "Aquí tienes los datos solicitados:\n\n$data"
        } else {
            "No he encontrado datos relevantes para tu consulta."
        }
    }
}
