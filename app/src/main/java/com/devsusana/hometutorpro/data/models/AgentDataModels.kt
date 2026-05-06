package com.devsusana.hometutorpro.data.models

/**
 * Lightweight data model for student summaries returned by [AgentContextDao].
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
    val name: String,
    val subjects: String,
    val course: String,
    val pendingBalance: Double,
    val lastPaymentDate: Long? = null
)
