package com.devsusana.hometutorpro.domain.repository

import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for managing local LLM inference operations.
 */
interface InferenceRepository {

    /** Emits true when the model is loaded and ready. */
    val isModelLoaded: StateFlow<Boolean>

    /** Emits true when the model is currently loading. */
    val isLoading: StateFlow<Boolean>

    /**
     * Loads the model into memory.
     * @return true if successful, false otherwise.
     */
    suspend fun loadModel(): Boolean

    /**
     * Generates a response from the model for the given prompt.
     */
    suspend fun generateResponse(prompt: String): String

    /**
     * Releases model resources.
     */
    fun release()
}
