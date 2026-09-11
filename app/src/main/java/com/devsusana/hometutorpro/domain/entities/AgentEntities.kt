package com.devsusana.hometutorpro.domain.entities

/**
 * Lightweight data model for student summaries returned by agent queries.
 * Projected from the `students` table, avoids loading unnecessary fields.
 */
data class AgentStudentSummary(
    val name: String,
    val subjects: String,
    val course: String,
    val pricePerHour: Double,
    val pendingBalance: Double,
    val isActive: Boolean,
    val lastPaymentDate: Long? = null
)

/**
 * Lightweight data model for schedule summaries used by the agent.
 * Joins `schedules` with `students` to include the student name.
 */
data class AgentScheduleSummary(
    val studentName: String,
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String
)

/**
 * Minimal data model exposing pending balances per student for the agent.
 */
data class AgentBalanceSummary(
    val name: String,
    val pendingBalance: Double
)

/**
 * Detailed student information returned when the agent searches by name.
 */
data class AgentStudentDetail(
    val studentId: String,
    val name: String,
    val subjects: String,
    val course: String,
    val pendingBalance: Double,
    val lastPaymentDate: Long? = null,
    val notes: String = ""
)

/**
 * Full schedule detail including IDs needed for creating schedule exception entries.
 * Used by the agent when it needs to cancel or reschedule a specific class occurrence.
 */
data class AgentScheduleDetail(
    val scheduleId: String,
    val studentId: String,
    val studentName: String,
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String
)

data class AgentClassLog(
    val studentId: String,
    val scheduleId: String,
    val date: Long,
    val startTime: String,
    val endTime: String,
    val isExtra: Boolean
)

data class AgentTransactionLog(
    val studentId: String,
    val type: String, // "PAYMENT" or "BALANCE_ADD"
    val amount: Double,
    val paymentType: String?, // "EFFECTIVE", "BIZUM" or null
    val timestamp: Long
)
