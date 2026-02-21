package com.devsusana.hometutorpro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.devsusana.hometutorpro.data.local.entities.SharedResourceEntity
import com.devsusana.hometutorpro.data.local.entities.SyncStatus
import kotlinx.coroutines.flow.Flow

/**
 * DAO for accessing shared resources in the local database (Free flavor).
 */
@Dao
interface SharedResourceDao {
    
    @Query("SELECT * FROM shared_resources WHERE studentId = :studentId AND professorId = :professorId AND pendingDelete = 0 ORDER BY sharedAt DESC")
    fun getSharedResourcesByStudent(studentId: Long, professorId: String): Flow<List<SharedResourceEntity>>
    
    @Query("SELECT * FROM shared_resources WHERE studentId = :studentId AND professorId = :professorId AND fileType = :fileType AND pendingDelete = 0 ORDER BY sharedAt DESC")
    fun getSharedResourcesByType(studentId: Long, professorId: String, fileType: String): Flow<List<SharedResourceEntity>>
    
    @Query("SELECT * FROM shared_resources WHERE studentId = :studentId AND professorId = :professorId AND sharedVia = :method AND pendingDelete = 0 ORDER BY sharedAt DESC")
    fun getSharedResourcesByMethod(studentId: Long, professorId: String, method: String): Flow<List<SharedResourceEntity>>

    @Query("SELECT * FROM shared_resources WHERE cloudId = :cloudId AND professorId = :professorId AND pendingDelete = 0")
    suspend fun getSharedResourceByCloudId(cloudId: String, professorId: String): SharedResourceEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSharedResource(resource: SharedResourceEntity): Long
    
    @Query("DELETE FROM shared_resources WHERE id = :resourceId AND professorId = :professorId")
    suspend fun deleteSharedResource(resourceId: Long, professorId: String)
    
    @Query("DELETE FROM shared_resources WHERE studentId = :studentId AND professorId = :professorId")
    suspend fun deleteSharedResourcesByStudent(studentId: Long, professorId: String)

    // Sync-specific queries
    @Query("SELECT * FROM shared_resources WHERE professorId = :professorId AND syncStatus = :status")
    suspend fun getSharedResourcesBySyncStatus(professorId: String, status: SyncStatus): List<SharedResourceEntity>

    @Query("SELECT * FROM shared_resources WHERE professorId = :professorId AND lastModifiedTimestamp > :timestamp")
    suspend fun getSharedResourcesModifiedSince(professorId: String, timestamp: Long): List<SharedResourceEntity>

    @Query("UPDATE shared_resources SET syncStatus = :status, lastModifiedTimestamp = :timestamp WHERE id = :id AND professorId = :professorId")
    suspend fun updateSyncStatus(id: Long, professorId: String, status: SyncStatus, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE shared_resources SET pendingDelete = 1, syncStatus = :syncStatus, lastModifiedTimestamp = :timestamp WHERE id = :id AND professorId = :professorId")
    suspend fun markForDeletion(id: Long, professorId: String, syncStatus: SyncStatus = SyncStatus.PENDING_DELETE, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE shared_resources SET pendingDelete = 1, syncStatus = :syncStatus, lastModifiedTimestamp = :timestamp WHERE studentId = :studentId AND professorId = :professorId")
    suspend fun markSharedResourcesForDeletionByStudent(studentId: Long, professorId: String, syncStatus: SyncStatus = SyncStatus.PENDING_DELETE, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM shared_resources WHERE pendingDelete = 1 AND syncStatus = :syncStatus AND professorId = :professorId")
    suspend fun deleteSyncedPendingDeletes(professorId: String, syncStatus: SyncStatus = SyncStatus.SYNCED)
}
