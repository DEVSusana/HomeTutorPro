package com.devsusana.hometutorpro.data.mappers

import com.devsusana.hometutorpro.data.local.entities.*
import com.devsusana.hometutorpro.domain.entities.*
import java.time.DayOfWeek

/**
 * Mapper functions to convert between Entity (Room) and Domain models.
 */

// Student Mappers
fun StudentEntity.toDomain(): Student {
    return Student(
        id = id.toString(),
        name = name,
        age = age,
        address = address,
        parentPhones = parentPhones,
        studentPhone = studentPhone,
        studentEmail = studentEmail,
        subjects = subjects,
        course = course,
        pricePerHour = pricePerHour,
        pendingBalance = pendingBalance,
        educationalAttention = educationalAttention,
        lastPaymentDate = lastPaymentDate,
        notes = notes,
        color = color,
        isActive = isActive,
        lastClassDate = lastClassDate
    )
}

fun Student.toEntity(existingId: Long = 0L, syncStatus: SyncStatus = SyncStatus.SYNCED): StudentEntity {
    return StudentEntity(
        id = existingId,
        cloudId = null,
        name = name,
        age = age,
        address = address,
        parentPhones = parentPhones,
        studentPhone = studentPhone,
        studentEmail = studentEmail,
        subjects = subjects,
        course = course,
        pricePerHour = pricePerHour,
        pendingBalance = pendingBalance,
        educationalAttention = educationalAttention,
        lastPaymentDate = lastPaymentDate,
        notes = notes,
        color = color,
        isActive = isActive,
        lastClassDate = lastClassDate,
        syncStatus = syncStatus,
        lastModifiedTimestamp = System.currentTimeMillis(),
        pendingDelete = false
    )
}

// Schedule Mappers
fun ScheduleEntity.toDomain(studentName: String? = null): Schedule {
    return Schedule(
        id = id.toString(),
        studentId = studentId.toString(),
        dayOfWeek = dayOfWeek,
        startTime = startTime,
        endTime = endTime,
        isCompleted = isCompleted,
        completedDate = completedDate,
        studentName = studentName
    )
}

fun Schedule.toEntity(studentId: Long, existingId: Long = 0L, syncStatus: SyncStatus = SyncStatus.SYNCED): ScheduleEntity {
    return ScheduleEntity(
        id = existingId,
        studentId = studentId,
        cloudId = null,
        dayOfWeek = dayOfWeek,
        startTime = startTime,
        endTime = endTime,
        isCompleted = isCompleted,
        completedDate = completedDate,
        syncStatus = syncStatus,
        lastModifiedTimestamp = System.currentTimeMillis(),
        pendingDelete = false
    )
}

// ScheduleException Mappers
fun ScheduleExceptionEntity.toDomain(): ScheduleException {
    return ScheduleException(
        id = id.toString(),
        studentId = studentId.toString(),
        date = exceptionDate,
        type = if (isCancelled) ExceptionType.CANCELLED else ExceptionType.RESCHEDULED,
        originalScheduleId = originalScheduleId,
        newStartTime = newStartTime ?: "",
        newEndTime = newEndTime ?: "",
        newDayOfWeek = newDayOfWeek,
        reason = reason
    )
}

fun ScheduleException.toEntity(studentId: Long, existingId: Long = 0L, syncStatus: SyncStatus = SyncStatus.SYNCED): ScheduleExceptionEntity {
    return ScheduleExceptionEntity(
        id = existingId,
        studentId = studentId,
        cloudId = null,
        originalScheduleId = originalScheduleId,
        exceptionDate = date,
        reason = reason,
        isCancelled = type == ExceptionType.CANCELLED,
        newStartTime = newStartTime.takeIf { it.isNotEmpty() },
        newEndTime = newEndTime.takeIf { it.isNotEmpty() },
        newDayOfWeek = newDayOfWeek,
        syncStatus = syncStatus,
        lastModifiedTimestamp = System.currentTimeMillis(),
        pendingDelete = false
    )
}

// Resource Mappers
fun ResourceEntity.toDomain(): Resource {
    return Resource(
        id = id.toString(),
        professorId = "", // Not stored in entity, managed at repository level
        name = name,
        url = cloudStoragePath ?: localFilePath,
        type = fileType,
        uploadDate = java.util.Date(uploadDate)
    )
}

fun Resource.toEntity(existingId: Long = 0L, syncStatus: SyncStatus = SyncStatus.SYNCED): ResourceEntity {
    return ResourceEntity(
        id = existingId,
        cloudId = null,
        name = name,
        localFilePath = url, // Store URL in localFilePath for now
        cloudStoragePath = null,
        fileType = type,
        uploadDate = uploadDate.time,
        syncStatus = syncStatus,
        lastModifiedTimestamp = System.currentTimeMillis(),
        pendingDelete = false
    )
}

// SharedResource Mappers
fun SharedResourceEntity.toDomain(): SharedResource {
    return SharedResource(
        id = id.toString(),
        studentId = studentId.toString(),
        fileName = fileName,
        fileType = fileType,
        fileSizeBytes = fileSizeBytes,
        sharedVia = try {
            ShareMethod.valueOf(sharedVia)
        } catch (e: Exception) {
            ShareMethod.EMAIL // Default fallback
        },
        sharedAt = sharedAt,
        notes = notes
    )
}

fun SharedResource.toEntity(existingId: Long = 0L, studentId: Long, syncStatus: SyncStatus = SyncStatus.SYNCED): SharedResourceEntity {
    return SharedResourceEntity(
        id = existingId,
        studentId = studentId,
        cloudId = null,
        fileName = fileName,
        fileType = fileType,
        fileSizeBytes = fileSizeBytes,
        sharedVia = sharedVia.name,
        sharedAt = sharedAt,
        notes = notes,
        syncStatus = syncStatus,
        lastModifiedTimestamp = System.currentTimeMillis(),
        pendingDelete = false
    )
}
