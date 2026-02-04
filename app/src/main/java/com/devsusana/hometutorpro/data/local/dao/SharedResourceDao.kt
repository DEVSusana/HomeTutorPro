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
    
    @Query("SELECT * FROM shared_resources WHERE studentId = :studentId AND pendingDelete = 0 ORDER BY sharedAt DESC")
    fun getSharedResourcesByStudent(studentId: Long): Flow<List<SharedResourceEntity>>
    
    @Query("SELECT * FROM shared_resources WHERE studentId = :studentId AND fileType = :fileType AND pendingDelete = 0 ORDER BY sharedAt DESC")
    fun getSharedResourcesByType(studentId: Long, fileType: String): Flow<List<SharedResourceEntity>>
    
    @Query("SELECT * FROM shared_resources WHERE studentId = :studentId AND sharedVia = :method AND pendingDelete = 0 ORDER BY sharedAt DESC")
    fun getSharedResourcesByMethod(studentId: Long, method: String): Flow<List<SharedResourceEntity>>

    @Query("SELECT * FROM shared_resources WHERE cloudId = :cloudId AND pendingDelete = 0")
    suspend fun getSharedResourceByCloudId(cloudId: String): SharedResourceEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSharedResource(resource: SharedResourceEntity): Long
    
    @Query("DELETE FROM shared_resources WHERE id = :resourceId")
    suspend fun deleteSharedResource(resourceId: Long)
    
    @Query("DELETE FROM shared_resources WHERE studentId = :studentId")
    suspend fun deleteSharedResourcesByStudent(studentId: Long)

    // Sync-specific queries
    @Query("SELECT * FROM shared_resources WHERE syncStatus = :status")
    suspend fun getSharedResourcesBySyncStatus(status: SyncStatus): List<SharedResourceEntity>

    @Query("SELECT * FROM shared_resources WHERE lastModifiedTimestamp > :timestamp")
    suspend fun getSharedResourcesModifiedSince(timestamp: Long): List<SharedResourceEntity>

    @Query("UPDATE shared_resources SET syncStatus = :status, lastModifiedTimestamp = :timestamp WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: SyncStatus, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE shared_resources SET pendingDelete = 1, syncStatus = :syncStatus, lastModifiedTimestamp = :timestamp WHERE id = :id")
    suspend fun markForDeletion(id: Long, syncStatus: SyncStatus = SyncStatus.PENDING_DELETE, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE shared_resources SET pendingDelete = 1, syncStatus = :syncStatus, lastModifiedTimestamp = :timestamp WHERE studentId = :studentId")
    suspend fun markSharedResourcesForDeletionByStudent(studentId: Long, syncStatus: SyncStatus = SyncStatus.PENDING_DELETE, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM shared_resources WHERE pendingDelete = 1 AND syncStatus = :syncStatus")
    suspend fun deleteSyncedPendingDeletes(syncStatus: SyncStatus = SyncStatus.SYNCED)
}
