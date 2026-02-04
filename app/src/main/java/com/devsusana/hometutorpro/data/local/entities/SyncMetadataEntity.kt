package com.devsusana.hometutorpro.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for tracking synchronization metadata.
 * Stores information about last sync times and pending changes.
 */
@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey
    val key: String,
    val value: String,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Predefined keys for sync metadata.
 */
object SyncMetadataKeys {
    const val LAST_SYNC_TIMESTAMP = "last_sync_timestamp"
    const val SYNC_IN_PROGRESS = "sync_in_progress"
    const val LAST_SYNC_ERROR = "last_sync_error"
    const val PENDING_STUDENTS_COUNT = "pending_students_count"
    const val PENDING_SCHEDULES_COUNT = "pending_schedules_count"
    const val PENDING_EXCEPTIONS_COUNT = "pending_exceptions_count"
    const val PENDING_RESOURCES_COUNT = "pending_resources_count"
}
