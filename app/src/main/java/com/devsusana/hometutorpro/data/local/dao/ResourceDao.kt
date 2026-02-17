package com.devsusana.hometutorpro.data.local.dao

import androidx.room.*
import com.devsusana.hometutorpro.data.local.entities.ResourceEntity
import com.devsusana.hometutorpro.data.local.entities.SyncStatus
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Resource operations in premium flavor.
 * Includes sync-specific queries.
 */
@Dao
interface ResourceDao {

    @Query("SELECT * FROM resources WHERE pendingDelete = 0 ORDER BY uploadDate DESC")
    fun getAllResources(): Flow<List<ResourceEntity>>

    @Query("SELECT * FROM resources ORDER BY id ASC")
    suspend fun getAllResourcesOnce(): List<ResourceEntity>

    @Query("SELECT * FROM resources WHERE id = :resourceId AND pendingDelete = 0")
    fun getResourceById(resourceId: Long): Flow<ResourceEntity?>

    @Query("SELECT * FROM resources WHERE cloudId = :cloudId AND pendingDelete = 0")
    suspend fun getResourceByCloudId(cloudId: String): ResourceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResource(resource: ResourceEntity): Long

    @Update
    suspend fun updateResource(resource: ResourceEntity)

    @Delete
    suspend fun deleteResource(resource: ResourceEntity)

    @Query("DELETE FROM resources WHERE id = :resourceId")
    suspend fun deleteResourceById(resourceId: Long)

    // Sync-specific queries
    @Query("SELECT * FROM resources WHERE syncStatus = :status")
    suspend fun getResourcesBySyncStatus(status: SyncStatus): List<ResourceEntity>

    @Query("SELECT * FROM resources WHERE lastModifiedTimestamp > :timestamp")
    suspend fun getResourcesModifiedSince(timestamp: Long): List<ResourceEntity>

    @Query("UPDATE resources SET syncStatus = :status, lastModifiedTimestamp = :timestamp WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: SyncStatus, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE resources SET pendingDelete = 1, syncStatus = :syncStatus, lastModifiedTimestamp = :timestamp WHERE id = :id")
    suspend fun markForDeletion(id: Long, syncStatus: SyncStatus = SyncStatus.PENDING_DELETE, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM resources WHERE pendingDelete = 1 AND syncStatus = :syncStatus")
    suspend fun deleteSyncedPendingDeletes(syncStatus: SyncStatus = SyncStatus.SYNCED)

    @Query("DELETE FROM resources")
    suspend fun deleteAllResources()
}
