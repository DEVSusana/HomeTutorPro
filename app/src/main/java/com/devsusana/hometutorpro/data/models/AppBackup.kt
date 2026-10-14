package com.devsusana.hometutorpro.data.models

import com.devsusana.hometutorpro.data.local.entities.*
import kotlinx.serialization.Serializable

@Serializable
data class AppBackup(
    val version: Int = 1,
    val students: List<StudentEntity> = emptyList(),
    val schedules: List<ScheduleEntity> = emptyList(),
    val exceptions: List<ScheduleExceptionEntity> = emptyList(),
    val resources: List<ResourceEntity> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)
