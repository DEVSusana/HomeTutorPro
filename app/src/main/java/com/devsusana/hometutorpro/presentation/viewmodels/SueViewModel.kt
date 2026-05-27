package com.devsusana.hometutorpro.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devsusana.hometutorpro.domain.entities.SpeechState
import com.devsusana.hometutorpro.domain.entities.SuePendingAction
import com.devsusana.hometutorpro.domain.entities.SueOperationResult
import com.devsusana.hometutorpro.domain.repository.SpeechService
import com.devsusana.hometutorpro.domain.repository.InferenceRepository
import com.devsusana.hometutorpro.domain.usecases.ISueAgent
import com.devsusana.hometutorpro.domain.usecases.implementations.ScheduleTools
import com.devsusana.hometutorpro.domain.usecases.implementations.StudentTools
import com.devsusana.hometutorpro.presentation.sue.SueResponseFormatter
import com.devsusana.hometutorpro.presentation.sue.SueUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
 * 2. SueAgent → Intent detection (schedule management or normal query).
 * 3. Confirmation flow → If a pending action is detected, Sue asks for
 *    confirmation before executing. "sí" executes, "no" cancels.
 * 4. InferenceRepository → On-device LLM inference for normal queries.
 * 5. TextToSpeech → Speaks the response.
 *
 * Scoped at the navigation host level so Sue's state persists across screens.
 */
@HiltViewModel
class SueViewModel @Inject constructor(
    private val speechService: SpeechService,
    private val sueAgent: ISueAgent,
    private val inferenceRepository: InferenceRepository,
    private val scheduleTools: ScheduleTools,
    private val studentTools: StudentTools
) : ViewModel() {

    private val _uiState = MutableStateFlow(SueUiState())
    /** UI state for Sue's overlay and FAB. */
    val uiState: StateFlow<SueUiState> = _uiState.asStateFlow()

    /** Job that resets the speech state after a timeout (Bug-fix #2). */
    private var listeningTimeoutJob: Job? = null

    companion object {
        /** Max milliseconds to remain in LISTENING/PROCESSING before forcing IDLE. */
        private const val LISTENING_TIMEOUT_MS = 15_000L

        private val AFFIRMATIVE_WORDS = setOf("sí", "si", "yes", "confirmar", "confirmo", "ok", "vale", "correcto", "adelante", "de acuerdo")
        private val NEGATIVE_WORDS = setOf("no", "cancelar", "cancela", "nope", "negativo", "para", "olvídalo", "olvida")
    }

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
            SpeechState.IDLE, SpeechState.ERROR -> {
                speechService.initializeTts()
                _uiState.update {
                    it.copy(
                        isOverlayVisible = true,
                        errorMessage = null,
                        partialTranscription = "",
                        finalTranscription = "",
                        agentResponse = ""
                    )
                }
                speechService.startListening()
                startListeningTimeout()
            }
            SpeechState.LISTENING -> {
                cancelListeningTimeout()
                speechService.stopListening()
            }
            SpeechState.SPEAKING -> {
                speechService.stopSpeaking()
            }
            else -> {
                _uiState.update { it.copy(isOverlayVisible = true) }
            }
        }
    }

    /**
     * Dismisses the overlay and stops any active operations.
     * Also clears any pending action.
     */
    fun onDismiss() {
        cancelListeningTimeout()
        speechService.stopListening()
        speechService.stopSpeaking()
        _uiState.update {
            it.copy(
                isOverlayVisible = false,
                errorMessage = null,
                pendingAction = null
            )
        }
    }

    /** Clears any displayed error message and resets speech state if needed. */
    fun onErrorDismissed() {
        if (_uiState.value.speechState == SpeechState.ERROR) {
            speechService.resetState()
        }
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        cancelListeningTimeout()
        speechService.release()
        inferenceRepository.release()
    }

    private fun preloadModel() {
        viewModelScope.launch { inferenceRepository.loadModel() }
    }

    private fun collectSpeechState() {
        viewModelScope.launch {
            speechService.state.collect { state ->
                _uiState.update { it.copy(speechState = state) }
                // Cancel the timeout once we leave LISTENING/PROCESSING
                if (state == SpeechState.IDLE || state == SpeechState.SPEAKING) {
                    cancelListeningTimeout()
                }
            }
        }
    }

    private fun collectTranscriptions() {
        viewModelScope.launch {
            speechService.transcriptions.collect { transcription ->
                cancelListeningTimeout()
                _uiState.update {
                    it.copy(
                        finalTranscription = transcription,
                        partialTranscription = ""
                    )
                }
                handleTranscription(transcription)
            }
        }
    }

    private fun collectPartialTranscriptions() {
        viewModelScope.launch {
            speechService.partialTranscriptions.collect { partial ->
                _uiState.update { it.copy(partialTranscription = partial) }
            }
        }
    }

    private fun collectErrors() {
        viewModelScope.launch {
            speechService.errors.collect { error ->
                cancelListeningTimeout()
                _uiState.update { it.copy(errorMessage = error) }
            }
        }
    }


    private fun collectModelState() {
        viewModelScope.launch {
            inferenceRepository.isModelLoaded.collect { loaded ->
                _uiState.update { it.copy(isModelLoaded = loaded) }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Bug-fix #2 — Timeout mechanism
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Starts a coroutine that resets the speech state to IDLE after
     * [LISTENING_TIMEOUT_MS] if no transcription arrives.
     * This prevents the FAB from getting stuck when navigating between screens.
     */
    private fun startListeningTimeout() {
        cancelListeningTimeout()
        listeningTimeoutJob = viewModelScope.launch {
            delay(LISTENING_TIMEOUT_MS)
            val state = _uiState.value.speechState
            if (state == SpeechState.LISTENING || state == SpeechState.PROCESSING) {
                speechService.resetState()
            }
        }
    }

    private fun cancelListeningTimeout() {
        listeningTimeoutJob?.cancel()
        listeningTimeoutJob = null
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Transcription handling — confirmation flow + normal query
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Routes a transcription through either the confirmation flow (if a pending
     * action exists) or the normal agent pipeline.
     */
    private suspend fun handleTranscription(transcription: String) {
        val pendingAction = _uiState.value.pendingAction

        if (pendingAction != null) {
            handleConfirmation(transcription, pendingAction)
        } else {
            // Check for action intents first (rule-based, no LLM needed)
            val intentResult = sueAgent.detectActionIntent(transcription)
            if (intentResult != null) {
                val message = SueResponseFormatter.format(intentResult)
                val action = (intentResult as? SueOperationResult.Prepare.Success)?.action
                _uiState.update { it.copy(agentResponse = message, pendingAction = action) }
                speechService.speak(message)
            } else {
                processWithAgent(transcription)
            }
        }
    }

    /**
     * Handles a user response to a pending-action confirmation.
     * "sí" → execute; "no" → cancel; anything else → ask again.
     */
    private suspend fun handleConfirmation(transcription: String, pendingAction: SuePendingAction) {
        val lower = transcription.lowercase().trim()

        when {
            AFFIRMATIVE_WORDS.any { it in lower } -> {
                _uiState.update { it.copy(pendingAction = null) }
                executePendingAction(pendingAction)
            }
            NEGATIVE_WORDS.any { it in lower } -> {
                val cancelMessage = "De acuerdo, he cancelado la acción."
                _uiState.update { it.copy(agentResponse = cancelMessage, pendingAction = null) }
                speechService.speak(cancelMessage)
            }
            else -> {
                val askAgain = "No he entendido. Di «sí» para confirmar o «no» para cancelar."
                _uiState.update { it.copy(agentResponse = askAgain) }
                speechService.speak(askAgain)
            }
        }
    }

    /**
     * Executes a confirmed [SuePendingAction] and updates the UI with the result.
     */
    private suspend fun executePendingAction(action: SuePendingAction) {
        try {
            val executeResult = when (action) {
                is SuePendingAction.CancelClass ->
                    scheduleTools.executeCancelAction(action)
                is SuePendingAction.RescheduleClass ->
                    scheduleTools.executeRescheduleAction(action)
                is SuePendingAction.RegisterPayment ->
                    studentTools.executeRegisterPayment(action)
                is SuePendingAction.AddBalance ->
                    studentTools.executeAddBalance(action)
            }
            val response = SueResponseFormatter.format(executeResult)
            _uiState.update { it.copy(agentResponse = response) }
            speechService.speak(response)
        } catch (e: Exception) {
            val errorMsg = "Lo siento, no se pudo completar la acción."
            _uiState.update { it.copy(agentResponse = errorMsg, errorMessage = e.message) }
            speechService.speak(errorMsg)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Normal LLM pipeline
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Processes the user's transcription through the Sue agent pipeline:
     * 1. SueAgent gathers relevant tool context and builds a prompt.
     * 2. InferenceRepository runs inference on the prompt.
     * 3. The response is displayed and spoken via TTS.
     *
     * Falls back to raw tool data if the model is not loaded.
     */
    private suspend fun processWithAgent(transcription: String) {
        try {
            val prompt = sueAgent.buildPromptWithContext(transcription)

            val response = if (_uiState.value.isModelLoaded) {
                inferenceRepository.generateResponse(prompt)
            } else {
                extractDataFromPrompt(prompt)
            }

            _uiState.update { it.copy(agentResponse = response) }
            speechService.speak(response)
        } catch (e: Exception) {
            val errorResponse = "Lo siento, hubo un error al procesar tu consulta."
            _uiState.update {
                it.copy(agentResponse = errorResponse, errorMessage = e.message)
            }
            speechService.speak(errorResponse)
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
