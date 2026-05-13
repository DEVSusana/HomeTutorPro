package com.devsusana.hometutorpro.core.sue.inference

import android.content.Context
import android.util.Log
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
 * Manages the lifecycle of the on-device Gemma LLM model via MediaPipe's
 * LLM Inference API.
 *
 * Handles model discovery, loading, inference execution, and resource cleanup.
 * The model file must be placed at [MODEL_PATH] on the device's external storage.
 *
 * @see <a href="https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference/android">MediaPipe LLM Inference</a>
 */
@Singleton
class MediaPipeModelManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "MediaPipeModelManager"

        /**
         * Expected path for the Gemma 2B model file on device.
         * The model should be pushed via `adb push` during development.
         * See docs/sue-model-setup.md for instructions.
         */
        private const val MODEL_DIRECTORY = "sue_model"
        private const val MODEL_FILENAME = "gemma-2b-it-gpu-int4.bin"

        /** Maximum number of tokens the model can process in a single prompt. */
        private const val MAX_TOKENS = 1024

        /** Temperature for text generation (0.0 = deterministic, 1.0 = creative). */
        private const val TEMPERATURE = 0.7f

        /** Top-K sampling parameter. */
        private const val TOP_K = 40
    }

    private var llmInference: LlmInference? = null

    private val _isModelLoaded = MutableStateFlow(false)
    /** Whether the model is loaded and ready for inference. */
    val isModelLoaded: StateFlow<Boolean> get() = _isModelLoaded.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    /** Whether the model is currently being loaded. */
    val isLoading: StateFlow<Boolean> get() = _isLoading.asStateFlow()

    /**
     * Attempts to load the Gemma model from the device's file system.
     *
     * Searches for the model at:
     * 1. App-specific files directory: `files/sue_model/gemma-2b-it-gpu-int4.bin`
     * 2. External storage (legacy): `/sdcard/sue_model/gemma-2b-it-gpu-int4.bin`
     *
     * @return `true` if the model was loaded successfully, `false` otherwise.
     */
    suspend fun loadModel(): Boolean = withContext(Dispatchers.IO) {
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

    /**
     * Generates a response for the given prompt using the loaded model.
     *
     * @param prompt The full prompt including system instructions and context.
     * @return The generated text response, or an error message if inference fails.
     * @throws IllegalStateException if the model is not loaded.
     */
    suspend fun generateResponse(prompt: String): String = withContext(Dispatchers.IO) {
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
            "Lo siento, hubo un error al procesar tu consulta. Por favor, inténtalo de nuevo."
        } finally {
            try {
                session?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing session: ${e.message}", e)
            }
        }
    }

    /**
     * Releases the model from memory.
     * Should be called when the agent is no longer needed.
     */
    fun release() {
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

    /**
     * Searches for the model file in known locations.
     *
     * @return The absolute path to the model file, or null if not found.
     */
    private fun findModelFile(): String? {
        // Priority 1: App-specific internal files directory
        val internalPath = File(context.filesDir, "$MODEL_DIRECTORY/$MODEL_FILENAME")
        if (internalPath.exists()) {
            return internalPath.absolutePath
        }

        // Priority 2: External files directory (accessible via adb push)
        val externalPath = context.getExternalFilesDir(null)?.let {
            File(it, "$MODEL_DIRECTORY/$MODEL_FILENAME")
        }
        if (externalPath?.exists() == true) {
            return externalPath.absolutePath
        }

        // Priority 3: Legacy location on sdcard
        val sdcardPath = File("/sdcard/$MODEL_DIRECTORY/$MODEL_FILENAME")
        if (sdcardPath.exists()) {
            return sdcardPath.absolutePath
        }

        return null
    }
}
