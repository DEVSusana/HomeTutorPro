package com.devsusana.hometutorpro.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Room entity for ScheduleException in premium flavor.
 * Includes sync fields for offline-first architecture.
 */
@Serializable
@Entity(
    tableName = "schedule_exceptions",
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
        Index(value = ["exceptionDate"]),
        Index(value = ["cloudId"]),
        Index(value = ["syncStatus"]),
        Index(value = ["professorId"]),
        // Ensure only one exception per schedule and date for a professor
        Index(value = ["professorId", "originalScheduleId", "exceptionDate"], unique = true)
    ]
)
data class ScheduleExceptionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Multi-user security
    val professorId: String,
    
    // Firestore document ID
    val cloudId: String? = null,
    
    // Exception data
    val studentId: Long,
    val originalScheduleId: String,
    val exceptionDate: Long, // Timestamp in milliseconds
    val reason: String,
    val type: String, // CANCELLED, RESCHEDULED, EXTRA
    val newStartTime: String?, // Format: HH:mm (if rescheduled)
    val newEndTime: String?,   // Format: HH:mm (if rescheduled)
    val newDayOfWeek: java.time.DayOfWeek?,
    
    // Sync fields
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val lastModifiedTimestamp: Long = System.currentTimeMillis(),
    val pendingDelete: Boolean = false
)
