package com.devsusana.hometutorpro.data.models

import com.devsusana.hometutorpro.data.local.entities.*
import kotlinx.serialization.Serializable

@Serializable
data class AppBackup(
    val version: Int,
    val students: List<StudentEntity>,
    val schedules: List<ScheduleEntity>,
    val exceptions: List<ScheduleExceptionEntity>,
    val resources: List<ResourceEntity>,
    val timestamp: Long = System.currentTimeMillis()
)
