package com.devsusana.hometutorpro.data.local.models

/**
 * Data layer projection DTO for student summary queries executed by Room.
 */
data class StudentSummaryProjection(
    val name: String,
    val subjects: String,
    val course: String,
    val pricePerHour: Double,
    val pendingBalance: Double,
    val isActive: Boolean,
    val lastPaymentDate: Long? = null
)

/**
 * Data layer projection DTO for student balances executed by Room.
 */
data class BalanceSummaryProjection(
    val name: String,
    val pendingBalance: Double
)

/**
 * Data layer projection DTO for detailed student searches executed by Room.
 */
data class StudentDetailProjection(
    val studentId: String,
    val name: String,
    val subjects: String,
    val course: String,
    val pricePerHour: Double,
    val pendingBalance: Double,
    val educationalAttention: String,
    val notes: String,
    val lastPaymentDate: Long? = null,
    val parentPhones: String,
    val studentPhone: String,
    val studentEmail: String? = null,
    val address: String
)

/**
 * Data layer projection DTO for schedule summary queries executed by Room.
 */
data class ScheduleSummaryProjection(
    val studentName: String,
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String
)

/**
 * Data layer projection DTO for detailed schedule queries executed by Room.
 */
data class ScheduleDetailProjection(
    val scheduleId: Long,
    val studentId: Long,
    val studentName: String,
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String
)
