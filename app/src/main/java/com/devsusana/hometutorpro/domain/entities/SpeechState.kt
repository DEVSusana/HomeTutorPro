package com.devsusana.hometutorpro.domain.entities

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
