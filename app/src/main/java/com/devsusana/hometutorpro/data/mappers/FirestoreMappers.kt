package com.devsusana.hometutorpro.data.mappers

import com.devsusana.hometutorpro.data.local.entities.ScheduleEntity
import com.devsusana.hometutorpro.data.local.entities.ScheduleExceptionEntity
import com.devsusana.hometutorpro.data.local.entities.StudentEntity
import com.devsusana.hometutorpro.data.local.entities.SyncStatus
import com.devsusana.hometutorpro.core.auth.SecureAuthManager
import com.devsusana.hometutorpro.domain.repository.RemoteDocument
import com.google.firebase.firestore.DocumentSnapshot
import java.time.DayOfWeek

/**
 * Extension functions for converting Room entities to Firestore maps.
 * Centralized point of truth for Firestore mapping.
 */

fun StudentEntity.toFirestoreMap(secureAuthManager: SecureAuthManager): Map<String, Any?> {
    return mapOf(
        "name" to name,
        "age" to age,
        "address" to secureAuthManager.encryptPII(address),
        "parentPhones" to secureAuthManager.encryptPII(parentPhones),
        "studentPhone" to secureAuthManager.encryptPII(studentPhone),
        "studentEmail" to secureAuthManager.encryptPII(studentEmail),
        "subjects" to subjects,
        "course" to course,
        "pricePerHour" to pricePerHour,
        "pendingBalance" to pendingBalance,
        "educationalAttention" to educationalAttention,
        "lastPaymentDate" to lastPaymentDate,
        "color" to color,
        "lastModified" to lastModifiedTimestamp
    )
}

fun RemoteDocument.toStudentEntity(secureAuthManager: SecureAuthManager, professorId: String): StudentEntity {
    return StudentEntity(
        id = 0,
        professorId = professorId,
        cloudId = id,
        name = data["name"] as? String ?: "",
        age = (data["age"] as? Long)?.toInt() ?: 0,
        address = secureAuthManager.decryptPII(data["address"] as? String),
        parentPhones = secureAuthManager.decryptPII(data["parentPhones"] as? String),
        studentPhone = secureAuthManager.decryptPII(data["studentPhone"] as? String),
        studentEmail = secureAuthManager.decryptPII(data["studentEmail"] as? String),
        subjects = data["subjects"] as? String ?: "",
        course = data["course"] as? String ?: "",
        pricePerHour = (data["pricePerHour"] as? Double) ?: 0.0,
        pendingBalance = (data["pendingBalance"] as? Double) ?: 0.0,
        educationalAttention = data["educationalAttention"] as? String ?: "",
        lastPaymentDate = data["lastPaymentDate"] as? Long,
        color = (data["color"] as? Long)?.toInt(),
        lastModifiedTimestamp = (data["lastModified"] as? Long) ?: System.currentTimeMillis(),
        syncStatus = SyncStatus.SYNCED
    )
}

fun RemoteDocument.toScheduleEntity(studentId: Long, professorId: String): ScheduleEntity {
    return ScheduleEntity(
        id = 0,
        professorId = professorId,
        studentId = studentId,
        cloudId = id,
        dayOfWeek = DayOfWeek.valueOf(data["dayOfWeek"] as? String ?: "MONDAY"),
        startTime = data["startTime"] as? String ?: "",
        endTime = data["endTime"] as? String ?: "",
        isCompleted = data["isCompleted"] as? Boolean ?: false,
        completedDate = data["completedDate"] as? Long,
        lastModifiedTimestamp = (data["lastModified"] as? Long) ?: System.currentTimeMillis(),
        syncStatus = SyncStatus.SYNCED
    )
}

fun RemoteDocument.toScheduleExceptionEntity(studentId: Long, professorId: String): ScheduleExceptionEntity {
    return ScheduleExceptionEntity(
        id = 0,
        professorId = professorId,
        studentId = studentId,
        cloudId = id,
        originalScheduleId = data["originalScheduleId"] as? String ?: "",
        exceptionDate = data["exceptionDate"] as? Long ?: 0L,
        reason = data["reason"] as? String ?: "",
        type = (data["type"] as? String) ?: if (data["isCancelled"] as? Boolean ?: true) "CANCELLED" else "RESCHEDULED",
        newStartTime = data["newStartTime"] as? String,
        newEndTime = data["newEndTime"] as? String,
        newDayOfWeek = (data["newDayOfWeek"] as? String)?.let { DayOfWeek.valueOf(it) },
        lastModifiedTimestamp = (data["lastModified"] as? Long) ?: System.currentTimeMillis(),
        syncStatus = SyncStatus.SYNCED
    )
}

fun DocumentSnapshot.toStudentEntity(secureAuthManager: SecureAuthManager, professorId: String): StudentEntity {
    return StudentEntity(
        id = 0,
        professorId = professorId,
        cloudId = id,
        name = getString("name") ?: "",
        age = getLong("age")?.toInt() ?: 0,
        address = secureAuthManager.decryptPII(getString("address")),
        parentPhones = secureAuthManager.decryptPII(getString("parentPhones")),
        studentPhone = secureAuthManager.decryptPII(getString("studentPhone")),
        studentEmail = secureAuthManager.decryptPII(getString("studentEmail")),
        subjects = getString("subjects") ?: "",
        course = getString("course") ?: "",
        pricePerHour = getDouble("pricePerHour") ?: 0.0,
        pendingBalance = getDouble("pendingBalance") ?: 0.0,
        educationalAttention = getString("educationalAttention") ?: "",
        lastPaymentDate = getLong("lastPaymentDate"),
        color = getLong("color")?.toInt(),
        lastModifiedTimestamp = getLong("lastModified") ?: System.currentTimeMillis(),
        syncStatus = SyncStatus.SYNCED
    )
}

fun ScheduleEntity.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "localId" to id,
        "dayOfWeek" to dayOfWeek.name,
        "startTime" to startTime,
        "endTime" to endTime,
        "isCompleted" to isCompleted,
        "completedDate" to completedDate,
        "lastModified" to lastModifiedTimestamp
    )
}

fun DocumentSnapshot.toScheduleEntity(studentId: Long, professorId: String): ScheduleEntity {
    return ScheduleEntity(
        id = 0,
        professorId = professorId,
        studentId = studentId,
        cloudId = id,
        dayOfWeek = DayOfWeek.valueOf(getString("dayOfWeek") ?: "MONDAY"),
        startTime = getString("startTime") ?: "",
        endTime = getString("endTime") ?: "",
        isCompleted = getBoolean("isCompleted") ?: false,
        completedDate = getLong("completedDate"),
        lastModifiedTimestamp = getLong("lastModified") ?: System.currentTimeMillis(),
        syncStatus = SyncStatus.SYNCED
    )
}

fun ScheduleExceptionEntity.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "localId" to id,
        "originalScheduleId" to originalScheduleId,
        "exceptionDate" to exceptionDate,
        "reason" to reason,
        "type" to type,
        "newStartTime" to newStartTime,
        "newEndTime" to newEndTime,
        "newDayOfWeek" to newDayOfWeek?.name,
        "lastModified" to lastModifiedTimestamp
    )
}

fun DocumentSnapshot.toScheduleExceptionEntity(studentId: Long, professorId: String): ScheduleExceptionEntity {
    return ScheduleExceptionEntity(
        id = 0,
        professorId = professorId,
        studentId = studentId,
        cloudId = id,
        originalScheduleId = getString("originalScheduleId") ?: "",
        exceptionDate = getLong("exceptionDate") ?: 0L,
        reason = getString("reason") ?: "",
        type = getString("type") ?: if (getBoolean("isCancelled") ?: true) "CANCELLED" else "RESCHEDULED",
        newStartTime = getString("newStartTime"),
        newEndTime = getString("newEndTime"),
        newDayOfWeek = getString("newDayOfWeek")?.let { DayOfWeek.valueOf(it) },
        lastModifiedTimestamp = getLong("lastModified") ?: System.currentTimeMillis(),
        syncStatus = SyncStatus.SYNCED
    )
}
