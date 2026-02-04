package com.devsusana.hometutorpro.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.DayOfWeek

/**
 * Room entity for Schedule in premium flavor.
 * Includes sync fields for offline-first architecture.
 */
@Entity(
    tableName = "schedules",
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["studentId"]),
        Index(value = ["cloudId"]),
        Index(value = ["syncStatus"])
    ]
)
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Firestore document ID
    val cloudId: String? = null,
    
    // Schedule data
    val studentId: Long,
    val dayOfWeek: DayOfWeek,
    val startTime: String, // Format: HH:mm
    val endTime: String,   // Format: HH:mm
    val isCompleted: Boolean = false,
    val completedDate: Long? = null, // Timestamp when class was marked as completed
    
    // Sync fields
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val lastModifiedTimestamp: Long = System.currentTimeMillis(),
    val pendingDelete: Boolean = false
)
