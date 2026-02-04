package com.devsusana.hometutorpro.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for Resource in premium flavor.
 * Includes sync fields for offline-first architecture.
 */
@Entity(
    tableName = "resources",
    indices = [
        Index(value = ["uploadDate"]),
        Index(value = ["cloudId"]),
        Index(value = ["syncStatus"])
    ]
)
data class ResourceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Firestore document ID
    val cloudId: String? = null,
    
    // Resource data
    val name: String,
    val localFilePath: String, // Path to file in app's internal storage
    val cloudStoragePath: String?, // Path in Firebase Storage (null if not uploaded)
    val fileType: String,      // MIME type
    val uploadDate: Long,      // Timestamp in milliseconds
    
    // Sync fields
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val lastModifiedTimestamp: Long = System.currentTimeMillis(),
    val pendingDelete: Boolean = false
)
