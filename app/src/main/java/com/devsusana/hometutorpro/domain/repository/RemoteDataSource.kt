package com.devsusana.hometutorpro.domain.repository

/**
 * Semantic type for cloud-compatible data maps.
 */
typealias CloudData = Map<String, Any?>

/**
 * Interface to decouple sync logic from specific cloud providers (Firestore, Supabase, etc).
 * Resolves "Evolution Debt" finding.
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
