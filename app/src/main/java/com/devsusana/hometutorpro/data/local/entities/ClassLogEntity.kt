package com.devsusana.hometutorpro.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "class_logs",
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
        Index("date")
    ]
)
data class ClassLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val professorId: String,
    val studentId: Long,
    val scheduleId: String,
    val date: Long, // Epoch millis of the class date
    val startTime: String,
    val endTime: String,
    val isExtra: Boolean
)
