package com.devsusana.hometutorpro.data.local.dao

import androidx.room.*
import com.devsusana.hometutorpro.data.local.entities.SyncMetadataEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for SyncMetadata operations.
 * Manages synchronization state and tracking.
 */
@Dao
interface SyncMetadataDao {

    @Query("SELECT * FROM sync_metadata WHERE key = :key")
    suspend fun getMetadata(key: String): SyncMetadataEntity?

    @Query("SELECT * FROM sync_metadata WHERE key = :key")
    fun getMetadataFlow(key: String): Flow<SyncMetadataEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetadata(metadata: SyncMetadataEntity)

    @Query("UPDATE sync_metadata SET value = :value, lastUpdated = :timestamp WHERE key = :key")
    suspend fun updateMetadata(key: String, value: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM sync_metadata WHERE key = :key")
    suspend fun deleteMetadata(key: String)

    @Query("DELETE FROM sync_metadata")
    suspend fun deleteAllMetadata()

    // Helper methods
    suspend fun getLastSyncTimestamp(): Long {
        return getMetadata("last_sync_timestamp")?.value?.toLongOrNull() ?: 0L
    }

    suspend fun setLastSyncTimestamp(timestamp: Long) {
        insertMetadata(SyncMetadataEntity("last_sync_timestamp", timestamp.toString()))
    }

    suspend fun isSyncInProgress(): Boolean {
        return getMetadata("sync_in_progress")?.value?.toBoolean() ?: false
    }

    suspend fun setSyncInProgress(inProgress: Boolean) {
        insertMetadata(SyncMetadataEntity("sync_in_progress", inProgress.toString()))
    }

    suspend fun getLastCleanupTimestamp(): Long {
        return getMetadata("last_cleanup_timestamp")?.value?.toLongOrNull() ?: 0L
    }

    suspend fun setLastCleanupTimestamp(timestamp: Long) {
        insertMetadata(SyncMetadataEntity("last_cleanup_timestamp", timestamp.toString()))
    }
}
