package com.devsusana.hometutorpro.data.repository

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.devsusana.hometutorpro.domain.entities.SpeechState
import com.devsusana.hometutorpro.domain.repository.SpeechService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android implementation of [SpeechService] using [SpeechRecognizer] and [TextToSpeech].
 */
@Singleton
class SpeechServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SpeechService {

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow(SpeechState.IDLE)
    override val state: StateFlow<SpeechState> = _state.asStateFlow()

    private val _transcriptions = MutableSharedFlow<String>(extraBufferCapacity = 1)
    override val transcriptions: SharedFlow<String> = _transcriptions.asSharedFlow()

    private val _partialTranscriptions = MutableSharedFlow<String>(extraBufferCapacity = 1)
    override val partialTranscriptions: SharedFlow<String> = _partialTranscriptions.asSharedFlow()

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 1)
    override val errors: SharedFlow<String> = _errors.asSharedFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    override fun initializeTts() {
        if (textToSpeech != null) return

        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.let { tts ->
                    val defaultLocale = Locale.getDefault()
                    val ttsLocale = if (defaultLocale.language == "es") Locale("es", "ES") else defaultLocale
                    val result = tts.setLanguage(ttsLocale)
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
                            emitError("Error during text-to-speech playback.")
                        }
                    })
                }
            } else {
                emitError("Text-to-speech initialization failed.")
            }
        }
    }

    override fun startListening() {
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(createRecognitionListener())
        } else {
            speechRecognizer?.cancel()
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        _state.value = SpeechState.LISTENING
        speechRecognizer?.startListening(intent)
    }

    override fun stopListening() {
        speechRecognizer?.apply {
            stopListening()
            destroy()
        }
        speechRecognizer = null
        if (_state.value == SpeechState.LISTENING || _state.value == SpeechState.PROCESSING) {
            _state.value = SpeechState.IDLE
        }
    }

    override fun resetState() {
        stopListening()
        _state.value = SpeechState.IDLE
    }

    override fun speak(text: String) {
        if (!isTtsReady) {
            emitError("Text-to-speech is not ready.")
            return
        }

        val utteranceId = UUID.randomUUID().toString()
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    override fun stopSpeaking() {
        textToSpeech?.stop()
        if (_state.value == SpeechState.SPEAKING) {
            _state.value = SpeechState.IDLE
        }
    }

    override fun release() {
        stopListening()
        textToSpeech?.apply {
            stop()
            shutdown()
        }
        textToSpeech = null
        isTtsReady = false
        _state.value = SpeechState.IDLE
    }

    private fun emitError(message: String) {
        coroutineScope.launch {
            _errors.emit(message)
        }
    }

    private fun createRecognitionListener(): RecognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _state.value = SpeechState.LISTENING
        }

        override fun onBeginningOfSpeech() {}

        override fun onRmsChanged(rmsdB: Float) {}

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _state.value = SpeechState.PROCESSING
        }

        override fun onError(error: Int) {
            val message = mapSpeechErrorToMessage(error)
            _state.value = SpeechState.ERROR
            emitError(message)
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val transcription = matches?.firstOrNull() ?: ""
            if (transcription.isNotBlank()) {
                coroutineScope.launch {
                    _transcriptions.emit(transcription)
                }
            }
            _state.value = SpeechState.PROCESSING
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partial = matches?.firstOrNull() ?: ""
            if (partial.isNotBlank()) {
                coroutineScope.launch {
                    _partialTranscriptions.emit(partial)
                }
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
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
        10 -> "Too many requests."
        11 -> "Server disconnected."
        12 -> "Language not supported for offline recognition."
        13 -> "Language pack not downloaded. Please install it in system settings."
        14 -> "Cannot check offline support."
        else -> "Unknown speech recognition error (code $errorCode)."
    }
}
