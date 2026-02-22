package com.devsusana.hometutorpro.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for shared resources in Free flavor.
 * Tracks metadata of files shared with students.
 */
@Entity(
    tableName = "shared_resources",
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("studentId"),
        Index("sharedAt"),
        Index("cloudId"),
        Index("syncStatus"),
        Index("professorId")
    ]
)
data class SharedResourceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Multi-user security
    val professorId: String,
    
    // Firestore document ID
    val cloudId: String? = null,

    val studentId: Long,
    val fileName: String,
    val fileType: String,
    val fileSizeBytes: Long,
    val sharedVia: String, // "EMAIL" or "WHATSAPP"
    val sharedAt: Long,
    val notes: String,

    // Sync fields
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val lastModifiedTimestamp: Long = System.currentTimeMillis(),
    val pendingDelete: Boolean = false
)
