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

    @Query("SELECT * FROM resources WHERE professorId = :professorId AND pendingDelete = 0 ORDER BY uploadDate DESC")
    fun getAllResources(professorId: String): Flow<List<ResourceEntity>>

    @Query("SELECT * FROM resources WHERE professorId = :professorId ORDER BY id ASC")
    suspend fun getAllResourcesOnce(professorId: String): List<ResourceEntity>

    @Query("SELECT * FROM resources WHERE id = :resourceId AND professorId = :professorId AND pendingDelete = 0")
    fun getResourceById(resourceId: Long, professorId: String): Flow<ResourceEntity?>

    @Query("SELECT * FROM resources WHERE cloudId = :cloudId AND professorId = :professorId AND pendingDelete = 0")
    suspend fun getResourceByCloudId(cloudId: String, professorId: String): ResourceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResource(resource: ResourceEntity): Long

    @Update
    suspend fun updateResource(resource: ResourceEntity)

    @Delete
    suspend fun deleteResource(resource: ResourceEntity)

    @Query("DELETE FROM resources WHERE id = :resourceId AND professorId = :professorId")
    suspend fun deleteResourceById(resourceId: Long, professorId: String)

    // Sync-specific queries
    @Query("SELECT * FROM resources WHERE professorId = :professorId AND syncStatus = :status")
    suspend fun getResourcesBySyncStatus(professorId: String, status: SyncStatus): List<ResourceEntity>

    @Query("SELECT * FROM resources WHERE professorId = :professorId AND lastModifiedTimestamp > :timestamp")
    suspend fun getResourcesModifiedSince(professorId: String, timestamp: Long): List<ResourceEntity>

    @Query("UPDATE resources SET syncStatus = :status, lastModifiedTimestamp = :timestamp WHERE id = :id AND professorId = :professorId")
    suspend fun updateSyncStatus(id: Long, professorId: String, status: SyncStatus, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE resources SET pendingDelete = 1, syncStatus = :syncStatus, lastModifiedTimestamp = :timestamp WHERE id = :id AND professorId = :professorId")
    suspend fun markForDeletion(id: Long, professorId: String, syncStatus: SyncStatus = SyncStatus.PENDING_DELETE, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM resources WHERE pendingDelete = 1 AND syncStatus = :syncStatus AND professorId = :professorId")
    suspend fun deleteSyncedPendingDeletes(professorId: String, syncStatus: SyncStatus = SyncStatus.SYNCED)

    @Query("DELETE FROM resources WHERE professorId = :professorId")
    suspend fun deleteAllResources(professorId: String)
}
