package com.devsusana.hometutorpro.domain.repository

import com.devsusana.hometutorpro.domain.entities.SueDeviceCompatibility
import com.devsusana.hometutorpro.domain.entities.SueModelStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for managing the on-device Sue LLM model file lifecycle.
 *
 * Handles checking presence, querying file size, streaming downloads, and deletion.
 */
interface SueModelRepository {

    /** Emits the real-time status of the Sue LLM model. */
    val modelStatusFlow: Flow<SueModelStatus>

    /**
     * Checks if a valid model file (.bin or .task) exists on disk.
     *
     * @return True if the model file is present.
     */
    fun isModelDownloaded(): Boolean

    /**
     * Returns the size in bytes of the downloaded model file, or null if not downloaded.
     */
    fun getDownloadedModelSize(): Long?

    /**
     * Initiates downloading the Sue LLM model from the given URL (or default URL).
     *
     * @param url Optional custom model URL. If null, the default CDN/HuggingFace URL is used.
     * @return A Flow emitting progress and status updates during download.
     */
    suspend fun downloadModel(url: String? = null): Flow<SueModelStatus>

    /**
     * Cancels any active download and removes partial temporary files.
     */
    fun cancelDownload()

    /**
     * Checks whether the current device meets the OS and hardware requirements for running Sue.
     *
     * @return [SueDeviceCompatibility] indicating support status, RAM, and SDK level.
     */
    fun checkDeviceCompatibility(): SueDeviceCompatibility

    /**
     * Deletes the local model file from disk and releases inference memory.
     *
     * @return True if model files were successfully removed.
     */
    suspend fun deleteModel(): Boolean
}
