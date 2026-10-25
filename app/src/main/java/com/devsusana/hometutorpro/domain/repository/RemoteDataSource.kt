package com.devsusana.hometutorpro.domain.repository

/**
 * Semantic type for cloud-compatible data maps.
 */
typealias CloudData = Map<String, Any?>

/**
 * Interface to decouple data synchronization logic from specific remote cloud storage providers.
 */
interface RemoteDataSource {
    suspend fun uploadDocument(
        path: String,
        data: CloudData,
        idempotencyKey: String?,
        idempotencyField: String
    ): String
    
    suspend fun deleteDocument(path: String)
    
    suspend fun downloadCollection(path: String, lastSyncTimestamp: Long): List<RemoteDocument>

    /**
     * Deletes a student and all their associated data (schedules, exceptions) recursively.
     * Essential for GDPR "Right to be Forgotten" in backendless architecture.
     */
    suspend fun deleteStudentData(professorId: String, studentCloudId: String)
}
