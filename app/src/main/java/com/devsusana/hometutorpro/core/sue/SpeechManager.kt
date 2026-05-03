package com.devsusana.hometutorpro.core.sue

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Represents the current state of the speech system.
 */
enum class SpeechState {
    /** No active speech operation. */
    IDLE,
    /** Actively listening for speech input. */
    LISTENING,
    /** Processing the captured speech. */
    PROCESSING,
    /** Speaking a response via TTS. */
    SPEAKING,
    /** An error occurred during the speech operation. */
    ERROR
}

/**
 * Manages the speech-to-text ([SpeechRecognizer]) and text-to-speech ([TextToSpeech])
 * subsystems for the Sue AI agent.
 *
 * Exposes:
 * - [state]: Current [SpeechState] as a [StateFlow].
 * - [transcriptions]: Completed transcription results as a [SharedFlow].
 * - [partialTranscriptions]: Real-time partial transcription updates as a [SharedFlow].
 *
 * Uses on-device recognition (API 31+) with fallback to cloud-based recognition
 * for older devices.
 *
 * @param context Application context for initializing Android speech services.
 */
@Singleton
class SpeechManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val _state = MutableStateFlow(SpeechState.IDLE)
    /** Current state of the speech system. */
    val state: StateFlow<SpeechState> = _state.asStateFlow()

    private val _transcriptions = MutableSharedFlow<String>(extraBufferCapacity = 1)
    /** Emits completed transcription results. */
    val transcriptions: SharedFlow<String> = _transcriptions.asSharedFlow()

    private val _partialTranscriptions = MutableSharedFlow<String>(extraBufferCapacity = 1)
    /** Emits partial (real-time) transcription updates while listening. */
    val partialTranscriptions: SharedFlow<String> = _partialTranscriptions.asSharedFlow()

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 1)
    /** Emits human-readable error messages. */
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    /**
     * Initializes the TTS engine. Should be called during app startup or
     * when the agent is first activated.
     */
    fun initializeTts() {
        if (textToSpeech != null) return

        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.let { tts ->
                    val result = tts.setLanguage(Locale("es", "ES"))
                    isTtsReady = result != TextToSpeech.LANG_MISSING_DATA &&
                            result != TextToSpeech.LANG_NOT_SUPPORTED

                    tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _state.value = SpeechState.SPEAKING
                        }

                        override fun onDone(utteranceId: String?) {
                            _state.value = SpeechState.IDLE
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            _state.value = SpeechState.ERROR
                            _errors.tryEmit("Error during text-to-speech playback.")
                        }
                    })
                }
            } else {
                _errors.tryEmit("Text-to-speech initialization failed.")
            }
        }
    }

    /**
     * Starts listening for speech input using the device microphone.
     *
     * Uses on-device recognition on API 31+ (offline), falling back to
     * cloud-based recognition on older devices.
     */
    fun startListening() {
        stopListening()

        speechRecognizer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else {
            SpeechRecognizer.createSpeechRecognizer(context)
        }

        speechRecognizer?.setRecognitionListener(createRecognitionListener())

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        _state.value = SpeechState.LISTENING
        speechRecognizer?.startListening(intent)
    }

    /**
     * Stops the current listening session and releases the recognizer.
     */
    fun stopListening() {
        speechRecognizer?.apply {
            stopListening()
            cancel()
            destroy()
        }
        speechRecognizer = null
    }

    /**
     * Speaks the given [text] aloud using the TTS engine.
     *
     * @param text The text for Sue to speak.
     */
    fun speak(text: String) {
        if (!isTtsReady) {
            _errors.tryEmit("Text-to-speech is not ready.")
            return
        }

        val utteranceId = UUID.randomUUID().toString()
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    /**
     * Stops any ongoing TTS playback.
     */
    fun stopSpeaking() {
        textToSpeech?.stop()
        if (_state.value == SpeechState.SPEAKING) {
            _state.value = SpeechState.IDLE
        }
    }

    /**
     * Releases all resources held by the speech system.
     * Should be called when the agent is no longer needed.
     */
    fun release() {
        stopListening()
        textToSpeech?.apply {
            stop()
            shutdown()
        }
        textToSpeech = null
        isTtsReady = false
        _state.value = SpeechState.IDLE
    }

    private fun createRecognitionListener(): RecognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _state.value = SpeechState.LISTENING
        }

        override fun onBeginningOfSpeech() {
            // User started speaking — keep LISTENING state
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Could be used for audio wave visualization
        }

        override fun onBufferReceived(buffer: ByteArray?) {
            // Raw audio data — not needed for transcription
        }

        override fun onEndOfSpeech() {
            _state.value = SpeechState.PROCESSING
        }

        override fun onError(error: Int) {
            val message = mapSpeechErrorToMessage(error)
            _state.value = SpeechState.ERROR
            _errors.tryEmit(message)
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val transcription = matches?.firstOrNull() ?: ""
            if (transcription.isNotBlank()) {
                _transcriptions.tryEmit(transcription)
            }
            _state.value = SpeechState.PROCESSING
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partial = matches?.firstOrNull() ?: ""
            if (partial.isNotBlank()) {
                _partialTranscriptions.tryEmit(partial)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
            // Reserved for future use
        }
    }

    private fun mapSpeechErrorToMessage(errorCode: Int): String = when (errorCode) {
        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
        SpeechRecognizer.ERROR_CLIENT -> "Client-side error."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission not granted."
        SpeechRecognizer.ERROR_NETWORK -> "Network error during recognition."
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout."
        SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Please try again."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service is busy."
        SpeechRecognizer.ERROR_SERVER -> "Server error."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Please try again."
        else -> "Unknown speech recognition error."
    }
}
