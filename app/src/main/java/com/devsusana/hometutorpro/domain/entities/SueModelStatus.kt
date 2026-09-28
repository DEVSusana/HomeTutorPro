package com.devsusana.hometutorpro.domain.entities

/**
 * Represents the current availability and download status of the Sue LLM on-device model.
 */
sealed interface SueModelStatus {

    /** Model file is not present on the device. */
    data object NotDownloaded : SueModelStatus

    /**
     * Model is currently being downloaded.
     *
     * @property bytesDownloaded Number of bytes downloaded so far.
     * @property totalBytes Total size in bytes if known from Content-Length, or -1 if unknown.
     * @property progress Value from 0.0f to 1.0f (or -1.0f if indeterminate).
     */
    data class Downloading(
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val progress: Float
    ) : SueModelStatus

    /**
     * Model is downloaded and available locally on disk.
     *
     * @property fileSizeBytes Total size of the model file in bytes.
     * @property formattedSize Human-readable file size string (e.g. "550 MB").
     */
    data class Downloaded(
        val fileSizeBytes: Long,
        val formattedSize: String
    ) : SueModelStatus

    /**
     * An error occurred while checking or downloading the model.
     *
     * @property message Error description.
     */
    data class Error(
        val message: String
    ) : SueModelStatus
}
