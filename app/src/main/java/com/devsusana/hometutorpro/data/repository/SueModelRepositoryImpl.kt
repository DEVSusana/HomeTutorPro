package com.devsusana.hometutorpro.data.repository

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.devsusana.hometutorpro.core.utils.SafeLogger
import com.devsusana.hometutorpro.domain.entities.SueDeviceCompatibility
import com.devsusana.hometutorpro.domain.entities.SueModelStatus
import com.devsusana.hometutorpro.domain.entities.SueUnsupportedReason
import com.devsusana.hometutorpro.domain.repository.InferenceRepository
import com.devsusana.hometutorpro.domain.repository.SueModelRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToInt

/**
 * Concrete implementation of [SueModelRepository] managing on-device LLM model files.
 */
@Singleton
class SueModelRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val inferenceRepository: InferenceRepository
) : SueModelRepository {

    private var ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    private var downloadScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Testing constructor allowing injection of custom [CoroutineDispatcher].
     */
    constructor(
        context: Context,
        inferenceRepository: InferenceRepository,
        ioDispatcher: CoroutineDispatcher
    ) : this(context, inferenceRepository) {
        this.ioDispatcher = ioDispatcher
        this.downloadScope = CoroutineScope(SupervisorJob() + ioDispatcher)
    }

    companion object {
        private const val TAG = "SueModelRepo"
        private const val MODEL_DIRECTORY = "sue_model"
        const val DEFAULT_MODEL_NAME_TASK = "gemma-3-1b-it-int4.task"
        const val DEFAULT_MODEL_NAME_BIN = "gemma-3-1b-it-int4.bin"
        const val DEFAULT_MODEL_NAME = DEFAULT_MODEL_NAME_TASK
        const val TEMP_SUFFIX = ".tmp"

        /** Minimum size for a valid model file (50 MB) to prevent loading corrupt/HTML error downloads */
        const val MIN_MODEL_SIZE_BYTES = 50_000_000L

        /** Default public model URL (Gemma 3 1B INT4 for MediaPipe LLM Inference in .task format). */
        const val DEFAULT_MODEL_URL = "https://github.com/DEVSusana/HomeTutorPro/releases/download/sue-model-v1/gemma3-1B-it-int4.task"

        private const val BUFFER_SIZE = 64 * 1024 // 64 KB
        private const val PROGRESS_EMIT_INTERVAL_BYTES = 512 * 1024 // 512 KB
        private const val CONNECT_TIMEOUT_MS = 30_000
        private const val READ_TIMEOUT_MS = 60_000
        private const val USER_AGENT = "HomeTutorPro/1.0.8 (Android; On-Device AI)"
    }

    private val _modelStatusFlow = MutableStateFlow<SueModelStatus>(SueModelStatus.NotDownloaded)
    override val modelStatusFlow: Flow<SueModelStatus> = _modelStatusFlow.asStateFlow()

    private var currentDownloadJob: Job? = null

    /** Internal override for minimum valid model size in tests */
    internal var minModelSizeOverride: Long? = null

    internal fun getMinValidSize(): Long = minModelSizeOverride ?: MIN_MODEL_SIZE_BYTES

    init {
        refreshModelStatus()
    }

    private fun refreshModelStatus() {
        val size = getDownloadedModelSize()
        if (size != null && size >= getMinValidSize()) {
            _modelStatusFlow.value = SueModelStatus.Downloaded(
                fileSizeBytes = size,
                formattedSize = formatFileSize(size)
            )
        } else {
            _modelStatusFlow.value = SueModelStatus.NotDownloaded
        }
    }

    override fun isModelDownloaded(): Boolean {
        val candidate = findModelFile()
        return candidate != null && candidate.exists() && candidate.length() >= getMinValidSize()
    }

    override fun getDownloadedModelSize(): Long? {
        val file = findModelFile()
        return if (file != null && file.exists() && file.length() >= getMinValidSize()) {
            file.length()
        } else {
            null
        }
    }

    /** Internal property for testing OS version compatibility */
    internal var sdkIntOverride: Int? = null

    override fun checkDeviceCompatibility(): SueDeviceCompatibility {
        val sdkVersion = sdkIntOverride ?: Build.VERSION.SDK_INT

        // 1. Check Android OS version: Must be at least Android 9.0 (API 28, Pie)
        if (sdkVersion < Build.VERSION_CODES.P) {
            SafeLogger.d(TAG, "Device incompatible: SDK $sdkVersion < ${Build.VERSION_CODES.P} (Android 9.0 Pie required)")
            return SueDeviceCompatibility(
                isSupported = false,
                reason = SueUnsupportedReason.UNSUPPORTED_ANDROID_VERSION,
                sdkVersion = sdkVersion
            )
        }

        // 2. Check RAM availability: Must have at least ~3.5 GB total RAM and not be low-RAM device
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        var totalRam = 0L
        var isLowRam = false

        if (activityManager != null) {
            val memInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            totalRam = memInfo.totalMem
            isLowRam = activityManager.isLowRamDevice
        }

        // Minimum 2.5 GB threshold in production (relaxed to 1.0 GB in debug mode to allow testing on standard emulators)
        val minRequiredRam = if (com.devsusana.hometutorpro.BuildConfig.DEBUG) 1_000_000_000L else 2_500_000_000L

        if ((!com.devsusana.hometutorpro.BuildConfig.DEBUG && isLowRam) || (totalRam in 1 until minRequiredRam)) {
            SafeLogger.d(TAG, "Device incompatible: RAM total $totalRam bytes is below $minRequiredRam or isLowRam=$isLowRam")
            return SueDeviceCompatibility(
                isSupported = false,
                reason = SueUnsupportedReason.INSUFFICIENT_RAM,
                totalRamBytes = totalRam,
                sdkVersion = sdkVersion
            )
        }

        return SueDeviceCompatibility(
            isSupported = true,
            reason = null,
            totalRamBytes = totalRam,
            sdkVersion = sdkVersion
        )
    }

    /**
     * Resolves the target filename and extension (.task or .bin) based on the URL and optional Content-Disposition.
     */
    internal fun resolveModelFileName(urlStr: String, contentDisposition: String? = null): String {
        // 1. Try to extract filename from Content-Disposition header if present
        if (!contentDisposition.isNullOrBlank()) {
            val filenameMatch = Regex("""filename\*?=['"]?(?:UTF-8'')?([^'";\r\n]+)['"]?""", RegexOption.IGNORE_CASE)
                .find(contentDisposition)
            val headerFilename = filenameMatch?.groupValues?.get(1)?.trim()
            if (!headerFilename.isNullOrBlank()) {
                val cleanName = File(headerFilename).name
                if (cleanName.endsWith(".task", ignoreCase = true) || cleanName.endsWith(".bin", ignoreCase = true)) {
                    return cleanName
                }
            }
        }

        // 2. Try to extract filename from the URL path
        try {
            val path = URL(urlStr).path
            val urlFilename = path.substringAfterLast('/').substringBefore('?')
            if (urlFilename.isNotBlank()) {
                if (urlFilename.endsWith(".task", ignoreCase = true) || urlFilename.endsWith(".bin", ignoreCase = true)) {
                    return urlFilename
                }
            }
        } catch (_: Exception) {
            // Fallback if URL parsing fails
        }

        // 3. Fallback: preserve .bin if explicitly present in urlStr, otherwise default to .task
        return if (urlStr.contains(".bin", ignoreCase = true)) {
            DEFAULT_MODEL_NAME_BIN
        } else {
            DEFAULT_MODEL_NAME_TASK
        }
    }

    override suspend fun downloadModel(url: String?): Flow<SueModelStatus> {
        val downloadUrl = if (!url.isNullOrBlank()) url else DEFAULT_MODEL_URL

        // If already downloaded, notify immediately
        val existingSize = getDownloadedModelSize()
        if (existingSize != null && existingSize > 0) {
            val status = SueModelStatus.Downloaded(existingSize, formatFileSize(existingSize))
            _modelStatusFlow.value = status
            return _modelStatusFlow.asStateFlow()
        }

        // If already downloading, join the active download flow
        if (currentDownloadJob?.isActive == true) {
            SafeLogger.d(TAG, "Download already active in background, returning existing flow")
            return _modelStatusFlow.asStateFlow()
        }

        SafeLogger.d(TAG, "Starting application-scoped model download from: $downloadUrl")
        currentDownloadJob = downloadScope.launch {
            executeDownload(downloadUrl)
        }

        return _modelStatusFlow.asStateFlow()
    }

    private suspend fun executeDownload(downloadUrl: String) = withContext(ioDispatcher) {
        val targetDir = File(context.filesDir, MODEL_DIRECTORY).apply {
            if (!exists()) mkdirs()
        }

        var targetFileName = resolveModelFileName(downloadUrl)
        var targetFile = File(targetDir, targetFileName)
        var tempFile = File(targetDir, "$targetFileName$TEMP_SUFFIX")

        _modelStatusFlow.value = SueModelStatus.Downloading(0L, -1L, 0f)

        var connection: HttpURLConnection? = null
        try {
            var existingBytes = 0L
            if (tempFile.exists()) {
                existingBytes = tempFile.length()
            }

            connection = openConnectionWithRedirects(downloadUrl, existingBytes)
            val responseCode = connection.responseCode

            val isResume = responseCode == HttpURLConnection.HTTP_PARTIAL
            if (responseCode !in 200..299) {
                if (tempFile.exists()) tempFile.delete()
                val errorMsg = "Download failed with HTTP response code $responseCode"
                SafeLogger.e(TAG, errorMsg)
                _modelStatusFlow.value = SueModelStatus.Error(errorMsg)
                return@withContext
            }

            val contentType = connection.contentType ?: ""
            if (contentType.contains("text/html", ignoreCase = true) || contentType.contains("text/plain", ignoreCase = true)) {
                if (tempFile.exists()) tempFile.delete()
                val errorMsg = "Invalid model response format: $contentType"
                SafeLogger.e(TAG, errorMsg)
                _modelStatusFlow.value = SueModelStatus.Error(errorMsg)
                return@withContext
            }

            // Re-check filename in case redirects or Content-Disposition specify .task / .bin
            val contentDisposition = connection.getHeaderField("Content-Disposition")
            val finalUrl = connection.url?.toString() ?: downloadUrl
            val resolvedName = resolveModelFileName(finalUrl, contentDisposition)
            if (resolvedName != targetFileName) {
                targetFileName = resolvedName
                targetFile = File(targetDir, targetFileName)
                val newTempFile = File(targetDir, "$targetFileName$TEMP_SUFFIX")
                if (tempFile.exists() && tempFile != newTempFile) {
                    tempFile.renameTo(newTempFile)
                }
                tempFile = newTempFile
            }

            val serverContentLength = connection.contentLengthLong
            val totalBytes = if (isResume && serverContentLength > 0) {
                existingBytes + serverContentLength
            } else if (serverContentLength > 0) {
                serverContentLength
            } else {
                -1L
            }

            var bytesDownloaded = if (isResume) existingBytes else 0L
            var lastEmittedBytes = bytesDownloaded

            val append = isResume && existingBytes > 0
            connection.inputStream.use { input ->
                FileOutputStream(tempFile, append).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead = 0

                    while (coroutineContext.isActive && input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        bytesDownloaded += bytesRead

                        if (bytesDownloaded - lastEmittedBytes >= PROGRESS_EMIT_INTERVAL_BYTES || bytesDownloaded == totalBytes) {
                            lastEmittedBytes = bytesDownloaded
                            val progress = if (totalBytes > 0) {
                                (bytesDownloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                            } else {
                                -1f
                            }
                            _modelStatusFlow.value = SueModelStatus.Downloading(bytesDownloaded, totalBytes, progress)
                        }
                    }
                    output.flush()
                }
            }

            if (!coroutineContext.isActive) {
                throw CancellationException("Download cancelled")
            }

            // Verify minimum size before final rename
            if (tempFile.length() < getMinValidSize()) {
                val incompleteSize = tempFile.length()
                if (tempFile.exists()) tempFile.delete()
                val errorMsg = "Downloaded file is incomplete ($incompleteSize bytes < ${getMinValidSize()} bytes minimum)."
                SafeLogger.e(TAG, errorMsg)
                _modelStatusFlow.value = SueModelStatus.Error(errorMsg)
                return@withContext
            }

            // Clean any existing model file before replacing with the newly downloaded format
            val existingModel = findModelFile()
            if (existingModel != null && existingModel.exists()) {
                existingModel.delete()
            }

            // Atomic rename from .tmp to target file
            if (targetFile.exists()) {
                targetFile.delete()
            }
            if (tempFile.renameTo(targetFile)) {
                val finalSize = targetFile.length()
                val successStatus = SueModelStatus.Downloaded(
                    fileSizeBytes = finalSize,
                    formattedSize = formatFileSize(finalSize)
                )
                SafeLogger.d(TAG, "Model downloaded successfully as ${targetFile.name}: $finalSize bytes")
                _modelStatusFlow.value = successStatus
            } else {
                val errorStatus = SueModelStatus.Error("Failed to rename temporary model file.")
                _modelStatusFlow.value = errorStatus
            }

        } catch (e: CancellationException) {
            SafeLogger.d(TAG, "Model download cancelled by user.")
            if (tempFile.exists()) {
                tempFile.delete()
            }
            _modelStatusFlow.value = SueModelStatus.NotDownloaded
        } catch (e: Exception) {
            SafeLogger.e(TAG, "Error during model download: ${e.message}", e)
            if (tempFile.exists()) {
                tempFile.delete()
            }
            _modelStatusFlow.value = SueModelStatus.Error(e.localizedMessage ?: "Unknown download error")
        } finally {
            connection?.disconnect()
            currentDownloadJob = null
        }
    }

    override fun cancelDownload() {
        SafeLogger.d(TAG, "cancelDownload called")
        currentDownloadJob?.cancel()
        currentDownloadJob = null

        val dirs = listOf(
            File(context.filesDir, MODEL_DIRECTORY),
            context.getExternalFilesDir(null)?.let { File(it, MODEL_DIRECTORY) }
        ).filterNotNull()

        for (dir in dirs) {
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles { _, name -> name.endsWith(TEMP_SUFFIX, ignoreCase = true) }?.forEach {
                    it.delete()
                }
            }
        }
        _modelStatusFlow.value = SueModelStatus.NotDownloaded
        refreshModelStatus()
    }

    override suspend fun deleteModel(): Boolean = withContext(ioDispatcher) {
        try {
            SafeLogger.d(TAG, "Deleting Sue model files...")
            cancelDownload()
            inferenceRepository.release()

            val dirs = listOf(
                File(context.filesDir, MODEL_DIRECTORY),
                context.getExternalFilesDir(null)?.let { File(it, MODEL_DIRECTORY) }
            ).filterNotNull()

            var deletedAny = false
            for (dir in dirs) {
                if (dir.exists() && dir.isDirectory) {
                    val files = dir.listFiles { _, name ->
                        name.endsWith(".bin", ignoreCase = true) ||
                                name.endsWith(".task", ignoreCase = true) ||
                                name.endsWith(".tmp", ignoreCase = true)
                    }
                    files?.forEach { file ->
                        val deleted = file.delete()
                        if (deleted) deletedAny = true
                    }
                }
            }

            _modelStatusFlow.value = SueModelStatus.NotDownloaded
            SafeLogger.d(TAG, "Model deletion completed. Result: $deletedAny")
            deletedAny || !isModelDownloaded()
        } catch (e: Exception) {
            SafeLogger.e(TAG, "Failed to delete model: ${e.message}", e)
            false
        }
    }

    private fun findModelFile(): File? {
        val dirs = listOf(
            File(context.filesDir, MODEL_DIRECTORY),
            context.getExternalFilesDir(null)?.let { File(it, MODEL_DIRECTORY) }
        ).filterNotNull()

        for (dir in dirs) {
            if (dir.exists() && dir.isDirectory) {
                val candidate = dir.listFiles { _, name ->
                    name.endsWith(".bin", ignoreCase = true) || name.endsWith(".task", ignoreCase = true)
                }?.firstOrNull()
                if (candidate != null) {
                    return candidate
                }
            }
        }
        return null
    }

    private fun openConnectionWithRedirects(
        urlStr: String,
        resumeFromBytes: Long = 0L,
        maxRedirects: Int = 5
    ): HttpURLConnection {
        var currentUrl = urlStr
        var redirects = 0

        while (redirects < maxRedirects) {
            val url = URL(currentUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Accept", "*/*")

            if (resumeFromBytes > 0) {
                connection.setRequestProperty("Range", "bytes=$resumeFromBytes-")
            }

            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode in 300..399) {
                val location = connection.getHeaderField("Location")
                connection.disconnect()
                if (!location.isNullOrBlank()) {
                    currentUrl = if (location.startsWith("http://") || location.startsWith("https://")) {
                        location
                    } else {
                        URL(URL(currentUrl), location).toString()
                    }
                    redirects++
                    continue
                }
            }
            return connection
        }
        throw IllegalStateException("Too many redirects: $redirects")
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0

        return when {
            gb >= 1.0 -> "${(gb * 10.0).roundToInt() / 10.0} GB"
            mb >= 1.0 -> "${mb.roundToInt()} MB"
            kb >= 1.0 -> "${kb.roundToInt()} KB"
            else -> "$bytes B"
        }
    }
}
