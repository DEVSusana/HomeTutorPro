package com.devsusana.hometutorpro.data.remote

/**
 * Semantic type for cloud-compatible data maps.
 */
typealias CloudData = Map<String, Any?>

/**
 * Representation of a remote document retrieved from cloud storage.
 *
 * @property id The document identifier in the remote store.
 * @property data The key-value properties of the remote document.
 */
data class RemoteDocument(
    val id: String,
    val data: Map<String, Any?>
)

/**
 * Interface to decouple data synchronization logic from specific remote cloud storage providers.
 */
interface RemoteDataSource {
    /**
     * Uploads or updates a document at the specified remote path.
     *
     * @param path The cloud collection or document path.
     * @param data The payload data map to persist.
     * @param idempotencyKey Optional key ensuring idempotent uploads across retries.
     * @param idempotencyField The field name within data that carries the idempotency key.
     * @return The resulting remote document ID.
     */
    suspend fun uploadDocument(
        path: String,
        data: CloudData,
        idempotencyKey: String?,
        idempotencyField: String
    ): String

    /**
     * Deletes a document at the specified remote path.
     *
     * @param path The cloud document path to delete.
     */
    suspend fun deleteDocument(path: String)

    /**
     * Downloads a collection of documents modified after the specified timestamp.
     *
     * @param path The cloud collection path to query.
     * @param lastSyncTimestamp Unix timestamp filter for incremental synchronization.
     * @return List of matching [RemoteDocument] instances.
     */
    suspend fun downloadCollection(path: String, lastSyncTimestamp: Long): List<RemoteDocument>

    /**
     * Deletes a student and all their associated data (schedules, exceptions) recursively.
     * Essential for GDPR "Right to be Forgotten" in backendless architecture.
     *
     * @param professorId The ID of the professor who owns the data.
     * @param studentCloudId The remote ID of the student to delete.
     */
    suspend fun deleteStudentData(professorId: String, studentCloudId: String)
}
