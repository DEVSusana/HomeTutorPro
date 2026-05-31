package com.devsusana.hometutorpro.data.repository

import android.content.Context
import android.util.Log
import com.devsusana.hometutorpro.domain.repository.InferenceRepository
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [InferenceRepository] utilizing MediaPipe LLM Inference for local execution.
 *
 * Discovers the model in secure app-specific storage locations and handles session lifecycle.
 */
@Singleton
class MediaPipeModelRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : InferenceRepository {

    companion object {
        private const val TAG = "MediaPipeModelRepo"
        private const val MODEL_DIRECTORY = "sue_model"
        private const val MODEL_FILENAME = "gemma-2b-it-gpu-int4.bin"
        private const val MAX_TOKENS = 1024
        private const val TEMPERATURE = 0.7f
        private const val TOP_K = 40
    }

    private var llmInference: LlmInference? = null

    private val _isModelLoaded = MutableStateFlow(false)
    override val isModelLoaded: StateFlow<Boolean> = _isModelLoaded.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    override suspend fun loadModel(): Boolean = withContext(Dispatchers.IO) {
        if (_isModelLoaded.value) {
            Log.d(TAG, "Model already loaded.")
            return@withContext true
        }

        if (_isLoading.value) {
            Log.d(TAG, "Model loading already in progress.")
            return@withContext false
        }

        _isLoading.value = true

        try {
            val modelPath = findModelFile()
            if (modelPath == null) {
                Log.w(TAG, "Model file not found. See docs/sue-model-setup.md for instructions.")
                _isLoading.value = false
                return@withContext false
            }

            Log.d(TAG, "Loading model from: $modelPath")

            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(MAX_TOKENS)
                .build()

            llmInference = LlmInference.createFromOptions(context, options)

            _isModelLoaded.value = true
            _isLoading.value = false

            Log.d(TAG, "Model loaded successfully.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model: ${e.message}", e)
            _isModelLoaded.value = false
            _isLoading.value = false
            false
        }
    }

    override suspend fun generateResponse(prompt: String): String = withContext(Dispatchers.Default) {
        val inference = llmInference
            ?: throw IllegalStateException("Model not loaded. Call loadModel() first.")

        var session: LlmInferenceSession? = null
        try {
            val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                .setTemperature(TEMPERATURE)
                .setTopK(TOP_K)
                .build()

            session = LlmInferenceSession.createFromOptions(inference, sessionOptions)
            session.addQueryChunk(prompt)
            session.generateResponse()
        } catch (e: Exception) {
            Log.e(TAG, "Inference error: ${e.message}", e)
            "Sorry, there was an error processing your query. Please try again."
        } finally {
            try {
                session?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing session: ${e.message}", e)
            }
        }
    }

    override fun release() {
        try {
            llmInference?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing LLM inference: ${e.message}", e)
        }
        llmInference = null
        _isModelLoaded.value = false
        _isLoading.value = false
        Log.d(TAG, "Model released.")
    }

    private fun findModelFile(): String? {
        // App-specific internal files directory
        val internalPath = File(context.filesDir, "$MODEL_DIRECTORY/$MODEL_FILENAME")
        if (internalPath.exists()) {
            return internalPath.absolutePath
        }

        // External files directory (accessible via adb push)
        val externalPath = context.getExternalFilesDir(null)?.let {
            File(it, "$MODEL_DIRECTORY/$MODEL_FILENAME")
        }
        if (externalPath?.exists() == true) {
            return externalPath.absolutePath
        }

        return null
    }
}
