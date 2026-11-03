package com.devsusana.hometutorpro.data.repository

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Context
import android.util.Log
import com.devsusana.hometutorpro.domain.entities.AppFeatureFlags
import com.devsusana.hometutorpro.domain.repository.IConfigRepository
import com.devsusana.hometutorpro.domain.repository.InferenceRepository
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    @param:ApplicationContext private val context: Context,
    private val configRepository: IConfigRepository
) : InferenceRepository {

    /**
     * Testing constructor allowing instantiation without Hilt or mock configuration.
     */
    constructor(context: Context) : this(
        context,
        object : IConfigRepository {
            override val featureFlags = MutableStateFlow(AppFeatureFlags())
            override suspend fun fetchAndActivate(): Boolean = true
            override fun isWidgetActionsEnabled(): Boolean = true
            override fun isSueLocalAiEnabled(): Boolean = true
            override fun getSueModelUrl(): String = ""
            override fun getMaintenanceMessage(): String = ""
        }
    )

    companion object {
        private const val TAG = "MediaPipeModelRepo"
        private const val MODEL_DIRECTORY = "sue_model"
        private const val MAX_TOKENS = 2048
        private const val TEMPERATURE = 0.3f
        private const val TOP_K = 20
    }

    private var llmInference: LlmInference? = null

    private val _isModelLoaded = MutableStateFlow(false)
    override val isModelLoaded: StateFlow<Boolean> = _isModelLoaded.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        // Automatically release model when on-device AI kill-switch is activated
        repositoryScope.launch {
            configRepository.featureFlags.collect { flags ->
                if (!flags.isSueLocalAiEnabled && _isModelLoaded.value) {
                    Log.w(TAG, "Local AI kill-switch activated via remote config: releasing loaded model.")
                    release()
                }
            }
        }

        (context.applicationContext as? Application)?.registerComponentCallbacks(object : ComponentCallbacks2 {
            override fun onTrimMemory(level: Int) {
                if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
                    Log.d(TAG, "onTrimMemory (level $level) triggered: releasing LLM inference to free RAM.")
                    release()
                }
            }

            override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {}

            @Deprecated("Deprecated in Java")
            @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
            override fun onLowMemory() {
                Log.d(TAG, "onLowMemory triggered: releasing LLM inference immediately.")
                release()
            }
        })
    }

    override suspend fun loadModel(): Boolean = withContext(Dispatchers.IO) {
        if (!configRepository.isSueLocalAiEnabled()) {
            Log.w(TAG, "Local LLM inference is disabled via remote configuration kill-switch.")
            return@withContext false
        }

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

            val modelFile = File(modelPath)
            if (!modelFile.exists() || modelFile.length() <= 0) {
                Log.w(TAG, "Model file is invalid or empty: $modelPath")
                _isLoading.value = false
                return@withContext false
            }

            Log.d(TAG, "Loading model from: $modelPath (${modelFile.length()} bytes)")

            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(MAX_TOKENS)
                .build()

            llmInference = LlmInference.createFromOptions(context, options)

            _isModelLoaded.value = true
            _isLoading.value = false

            Log.d(TAG, "Model loaded successfully.")
            true
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to load model: ${t.message}", t)
            _isModelLoaded.value = false
            _isLoading.value = false
            false
        }
    }

    override suspend fun generateResponse(prompt: String): String = withContext(Dispatchers.Default) {
        if (!configRepository.isSueLocalAiEnabled()) {
            Log.w(TAG, "Local LLM inference is disabled via remote configuration kill-switch.")
            release()
            return@withContext "Sorry, on-device AI is temporarily paused for maintenance. Please try again later."
        }

        val inference = llmInference
        if (inference == null) {
            Log.e(TAG, "Inference error: Model not loaded for prompt.")
            return@withContext "Sorry, the AI model is not ready yet. Please try again after loading the model."
        }

        var session: LlmInferenceSession? = null
        try {
            val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                .setTemperature(TEMPERATURE)
                .setTopK(TOP_K)
                .build()

            session = LlmInferenceSession.createFromOptions(inference, sessionOptions)
            session.addQueryChunk(prompt)
            session.generateResponse()
        } catch (t: Throwable) {
            Log.e(TAG, "Inference error: ${t.message}", t)
            "Sorry, there was an error processing your query. Please try again."
        } finally {
            try {
                session?.close()
            } catch (t: Throwable) {
                Log.e(TAG, "Error closing session: ${t.message}", t)
            }
        }
    }

    override fun release() {
        val inferenceToClose = llmInference
        llmInference = null
        _isModelLoaded.value = false
        _isLoading.value = false
        if (inferenceToClose != null) {
            repositoryScope.launch(Dispatchers.IO) {
                try {
                    inferenceToClose.close()
                } catch (t: Throwable) {
                    Log.e(TAG, "Error closing LLM inference off-main: ${t.message}", t)
                }
            }
        }
        Log.d(TAG, "Model released.")
    }

    private fun findModelFile(): String? {
        val dirs = listOf(
            File(context.filesDir, MODEL_DIRECTORY),
            context.getExternalFilesDir(null)?.let { File(it, MODEL_DIRECTORY) }
        ).filterNotNull()

        // Auto-detect any model file ending with .bin or .task in sue_model/
        for (dir in dirs) {
            if (dir.exists() && dir.isDirectory) {
                val candidate = dir.listFiles { _, name ->
                    name.endsWith(".bin", ignoreCase = true) || name.endsWith(".task", ignoreCase = true)
                }?.firstOrNull()
                if (candidate != null) {
                    Log.i(TAG, "Found model file: ${candidate.name}")
                    return candidate.absolutePath
                }
            }
        }

        return null
    }
}
