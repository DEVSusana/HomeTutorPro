package com.devsusana.hometutorpro.data.local.models

import com.devsusana.hometutorpro.domain.entities.AgentBalanceSummary
import com.devsusana.hometutorpro.domain.entities.AgentScheduleDetail
import com.devsusana.hometutorpro.domain.entities.AgentScheduleSummary
import com.devsusana.hometutorpro.domain.entities.AgentStudentDetail
import com.devsusana.hometutorpro.domain.entities.AgentStudentSummary

/**
 * Data layer projection DTO for student summary queries executed by Room.
 */
data class AgentStudentSummaryDto(
    val name: String,
    val subjects: String,
    val course: String,
    val pricePerHour: Double,
    val pendingBalance: Double,
    val isActive: Boolean,
    val lastPaymentDate: Long? = null
) {
    fun toDomain(): AgentStudentSummary = AgentStudentSummary(
        name = name,
        subjects = subjects,
        course = course,
        pricePerHour = pricePerHour,
        pendingBalance = pendingBalance,
        isActive = isActive,
        lastPaymentDate = lastPaymentDate
    )
}

/**
 * Data layer projection DTO for student balances executed by Room.
 */
data class AgentBalanceSummaryDto(
    val name: String,
    val pendingBalance: Double
) {
    fun toDomain(): AgentBalanceSummary = AgentBalanceSummary(
        name = name,
        pendingBalance = pendingBalance
    )
}

/**
 * Data layer projection DTO for detailed student searches executed by Room.
 */
data class AgentStudentDetailDto(
    val studentId: String,
    val name: String,
    val subjects: String,
    val course: String,
    val pricePerHour: Double = 0.0,
    val address: String = "",
    val studentPhone: String = "",
    val parentPhones: String = "",
    val studentEmail: String? = null,
    val pendingBalance: Double = 0.0,
    val lastPaymentDate: Long? = null,
    val notes: String = ""
) {
    fun toDomain(): AgentStudentDetail = AgentStudentDetail(
        studentId = studentId,
        name = name,
        subjects = subjects,
        course = course,
        pricePerHour = pricePerHour,
        address = address,
        studentPhone = studentPhone,
        parentPhones = parentPhones,
        studentEmail = studentEmail,
        pendingBalance = pendingBalance,
        lastPaymentDate = lastPaymentDate,
        notes = notes
    )
}

/**
 * Data layer projection DTO for schedule summary queries executed by Room.
 */
data class AgentScheduleSummaryDto(
    val studentName: String,
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String
) {
    fun toDomain(): AgentScheduleSummary = AgentScheduleSummary(
        studentName = studentName,
        dayOfWeek = dayOfWeek,
        startTime = startTime,
        endTime = endTime
    )
}

/**
 * Data layer projection DTO for full schedule detail queries executed by Room.
 */
data class AgentScheduleDetailDto(
    val scheduleId: String,
    val studentId: String,
    val studentName: String,
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String
) {
    fun toDomain(): AgentScheduleDetail = AgentScheduleDetail(
        scheduleId = scheduleId,
        studentId = studentId,
        studentName = studentName,
        dayOfWeek = dayOfWeek,
        startTime = startTime,
        endTime = endTime
    )
}
