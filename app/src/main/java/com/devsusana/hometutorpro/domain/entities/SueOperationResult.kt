package com.devsusana.hometutorpro.domain.entities

import com.devsusana.hometutorpro.domain.core.DomainError
import java.time.LocalDate

/**
 * Clean, immutable domain structures representing the result of a business query
 * or action executed by the Sue AI Agent tools.
 *
 * Mapped to localized, human-friendly presentation strings in the UI/Presentation layer
 * to support multi-language internationalization (GDPR compliance, Clean Architecture).
 */
sealed interface SueOperationResult {

    // ──────────────────────────────────────────────────────────────────────────
    // READ Operation Results
    // ──────────────────────────────────────────────────────────────────────────

    /** Results containing all scheduled classes. */
    data class WeeklySchedule(val schedules: List<AgentScheduleSummary>) : SueOperationResult

    /** Results containing schedules for a specific day and optional time slot. */
    data class DaySchedule(
        val dayOfWeek: Int,
        val timeFilter: String?,
        val schedules: List<AgentScheduleSummary>
    ) : SueOperationResult

    /** Results containing details about the next upcoming class. */
    data class NextClass(
        val schedule: AgentScheduleSummary?,
        val occurrenceDate: LocalDate?
    ) : SueOperationResult

    /** Results containing weekdays (1..5) with no classes scheduled. */
    data class FreeSlots(val freeDays: List<Int>) : SueOperationResult

    /** Results containing summaries of all students. */
    data class AllStudentsSummary(val students: List<AgentStudentSummary>) : SueOperationResult

    /** Results containing search hits for a specific student query. */
    data class StudentDetails(val query: String, val students: List<AgentStudentDetail>) : SueOperationResult

    /** Results containing students with non-zero pending balance. */
    data class StudentsWithBalance(val students: List<AgentBalanceSummary>) : SueOperationResult

    /** Results containing the count of active students. */
    data class ActiveStudentCount(val count: Int) : SueOperationResult

    // ──────────────────────────────────────────────────────────────────────────
    // WRITE Operation Results — Step 1: Preparation (Confirmation Request)
    // ──────────────────────────────────────────────────────────────────────────

    sealed interface Prepare : SueOperationResult {
        /** Ready for user confirmation. */
        data class Success(val action: SuePendingAction) : Prepare

        /** Preparation failed (e.g. entity not found). */
        data class Error(val errorType: ErrorType, val details: String? = null) : Prepare
    }

    // ──────────────────────────────────────────────────────────────────────────
    // WRITE Operation Results — Step 2: Execution (Database write)
    // ──────────────────────────────────────────────────────────────────────────

    sealed interface Execute : SueOperationResult {
        /** Execution succeeded and changes are written. */
        data class Success(val action: SuePendingAction) : Execute

        /** Execution failed because of a domain-layer error (e.g. DB write failed). */
        data class Error(val domainError: DomainError) : Execute

        /** Execution failed because no authenticated session was found. */
        data object AuthError : Execute
    }

    /** Error types during action preparation. */
    enum class ErrorType {
        STUDENT_NOT_FOUND,
        CLASS_NOT_FOUND,
        AUTH_ERROR,
        UNKNOWN
    }
}
