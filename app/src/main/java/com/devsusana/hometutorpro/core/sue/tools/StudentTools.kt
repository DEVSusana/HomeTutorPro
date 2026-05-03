package com.devsusana.hometutorpro.core.sue.tools

import com.devsusana.hometutorpro.domain.usecases.IQueryStudentsForAgentUseCase
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Koog-compatible tool definitions for student-related queries.
 *
 * These tools are registered with the Koog agent and allow the LLM
 * to call structured functions to query student data from the local
 * Room database.
 *
 * Each tool returns a formatted text string that the LLM can directly
 * include in its response to the user.
 */
@Singleton
class StudentTools @Inject constructor(
    private val queryStudentsUseCase: IQueryStudentsForAgentUseCase
) {

    /**
     * Returns a text summary of all students (name, subjects, course, price, balance, status).
     * Intended for broad queries like "list my students" or "how many students do I have".
     */
    fun getAllStudentsSummary(): String = runBlocking {
        val students = queryStudentsUseCase.getAllStudents()
        if (students.isEmpty()) {
            return@runBlocking "No students found."
        }

        buildString {
            appendLine("Students (${students.size} total):")
            students.forEach { student ->
                val status = if (student.isActive) "Active" else "Inactive"
                appendLine("- ${student.name}: ${student.subjects} | ${student.course} | " +
                    "${student.pricePerHour}€/h | Balance: ${student.pendingBalance}€ | $status")
            }
        }
    }

    /**
     * Searches students by name and returns details.
     * Intended for targeted queries like "tell me about María".
     */
    fun searchStudent(query: String): String = runBlocking {
        val results = queryStudentsUseCase.searchByName(query)
        if (results.isEmpty()) {
            return@runBlocking "No students found matching '$query'."
        }

        buildString {
            appendLine("Search results for '$query' (${results.size} found):")
            results.forEach { student ->
                appendLine("- ${student.name}: ${student.subjects} | ${student.course} | " +
                    "Balance: ${student.pendingBalance}€")
            }
        }
    }

    /**
     * Returns students with non-zero pending balance.
     * Intended for financial queries like "who owes me money".
     */
    fun getStudentsWithBalance(): String = runBlocking {
        val students = queryStudentsUseCase.getStudentsWithBalance()
        if (students.isEmpty()) {
            return@runBlocking "No students have pending balance."
        }

        buildString {
            appendLine("Students with pending balance (${students.size}):")
            students.forEach { student ->
                appendLine("- ${student.name}: ${student.pendingBalance}€")
            }
        }
    }

    /**
     * Returns the count of active students.
     * Intended for summary queries like "how many active students do I have".
     */
    fun getActiveStudentCount(): String = runBlocking {
        val count = queryStudentsUseCase.getActiveStudentCount()
        "You currently have $count active students."
    }
}
