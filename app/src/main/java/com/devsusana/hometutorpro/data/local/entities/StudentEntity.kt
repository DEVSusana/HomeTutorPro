package com.devsusana.hometutorpro.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Room entity for Student.
 * Unified version with sync fields for offline-first architecture.
 */
@Serializable
@Entity(
    tableName = "students",
    indices = [
        Index(value = ["name"]),
        Index(value = ["cloudId"], unique = true),
        Index(value = ["syncStatus"]),
        Index(value = ["professorId"])
    ]
)
data class StudentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Multi-user security
    val professorId: String,
    
    // Firestore document ID (null for new local-only entities)
    val cloudId: String? = null,
    
    // Student data
    val name: String,
    val age: Int,
    val address: String,
    val parentPhones: String,
    val studentPhone: String,
    val studentEmail: String?,
    val subjects: String,
    val course: String,
    val pricePerHour: Double,
    val pendingBalance: Double = 0.0,
    val educationalAttention: String,
    val lastPaymentDate: Long?,
    val notes: String = "",
    val color: Int? = null,
    val isActive: Boolean = true,
    val lastClassDate: Long? = null,
    
    // Sync fields
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val lastModifiedTimestamp: Long = System.currentTimeMillis(),
    val pendingDelete: Boolean = false
)
