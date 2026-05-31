package com.devsusana.hometutorpro.domain.repository

import com.devsusana.hometutorpro.domain.entities.SpeechState
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Service interface for managing speech recognition (STT) and text-to-speech (TTS).
 */
interface SpeechService {
    
    /** Current state of the speech system. */
    val state: StateFlow<SpeechState>

    /** Emits completed transcription results. */
    val transcriptions: SharedFlow<String>

    /** Emits partial (real-time) transcription updates while listening. */
    val partialTranscriptions: SharedFlow<String>

    /** Emits human-readable error messages. */
    val errors: SharedFlow<String>

    /**
     * Initializes the TTS engine.
     */
    fun initializeTts()

    /**
     * Starts listening for speech input from the microphone.
     */
    fun startListening()

    /**
     * Stops the current listening session.
     */
    fun stopListening()

    /**
     * Forces the state machine back to [SpeechState.IDLE].
     */
    fun resetState()

    /**
     * Speaks the given [text] aloud.
     */
    fun speak(text: String)

    /**
     * Stops any ongoing TTS playback.
     */
    fun stopSpeaking()

    /**
     * Releases resources held by the speech service.
     */
    fun release()
}
