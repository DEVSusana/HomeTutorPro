package com.devsusana.hometutorpro.data.sync

import com.devsusana.hometutorpro.data.local.dao.StudentDao
import com.devsusana.hometutorpro.data.local.entities.StudentEntity
import com.devsusana.hometutorpro.data.local.entities.SyncStatus
import javax.inject.Inject

/**
 * Resolves conflicts between local and remote data during synchronization.
 * Implements "Last Write Wins" strategy based on modification timestamps.
 */
class ConflictResolver @Inject constructor(
    private val studentDao: StudentDao
) {

    /**
     * Resolves conflict for Student entity.
     * Compares lastModifiedTimestamp to decide winner.
     */
    suspend fun resolveConflict(local: StudentEntity, remote: StudentEntity) {
        // Last Write Wins strategy
        val winner = if (local.lastModifiedTimestamp > remote.lastModifiedTimestamp) {
            // Local is newer, keep it and mark for upload
            local.copy(syncStatus = SyncStatus.PENDING_UPLOAD)
        } else {
            // Remote is newer, overwrite local
            remote.copy(
                id = local.id, // Keep local ID
                syncStatus = SyncStatus.SYNCED
            )
        }
        
        studentDao.updateStudent(winner)
    }
    
    // Add similar methods for other entities if needed
    // For now, focusing on Student as the primary entity
}
