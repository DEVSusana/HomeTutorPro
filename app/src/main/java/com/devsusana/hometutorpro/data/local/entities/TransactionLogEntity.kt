package com.devsusana.hometutorpro.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "transaction_logs",
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
        Index("professorId"),
        Index("timestamp")
    ]
)
data class TransactionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val professorId: String,
    val studentId: Long,
    val type: String, // "PAYMENT" or "BALANCE_ADD"
    val amount: Double,
    val paymentType: String?, // "EFFECTIVE", "BIZUM" or null
    val timestamp: Long
)
