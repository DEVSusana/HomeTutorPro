package com.devsusana.hometutorpro.domain.repository

/**
 * Repository interface for managing application backup and restore operations.
 *
 * Implements Clean Architecture by removing dependencies on Android Context and Uri.
 */
interface BackupRepository {
    
    /**
     * Serializes all app data (students, schedules, exceptions, resources) to a JSON string.
     */
    suspend fun createBackup(): String

    /**
     * Deserializes and restores the app data from the provided JSON content.
     */
    suspend fun restoreBackup(jsonContent: String): Result<Unit>
}
