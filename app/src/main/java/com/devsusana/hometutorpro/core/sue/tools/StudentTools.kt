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
            return@runBlocking "No se encontraron alumnos."
        }

        buildString {
            appendLine("Alumnos (${students.size} en total):")
            appendLine()
            students.forEach { student ->
                val status = if (student.isActive) "Activo" else "Inactivo"
                appendLine("• ${student.name}")
                appendLine("  Asignaturas: ${student.subjects}")
                appendLine("  Curso: ${student.course}")
                appendLine("  Precio: ${student.pricePerHour} euros la hora")
                appendLine("  Saldo pendiente: ${student.pendingBalance} euros")
                appendLine("  Estado: $status")
                appendLine()
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
            return@runBlocking "No se encontraron alumnos con el nombre '$query'."
        }

        buildString {
            appendLine("Resultados para '$query' (${results.size} encontrados):")
            appendLine()
            results.forEach { student ->
                appendLine("• ${student.name}")
                appendLine("  Asignaturas: ${student.subjects}")
                appendLine("  Curso: ${student.course}")
                appendLine("  Saldo pendiente: ${student.pendingBalance} euros")
                appendLine()
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
            return@runBlocking "Ningún alumno tiene saldo pendiente."
        }

        buildString {
            appendLine("Alumnos con saldo pendiente (${students.size}):")
            students.forEach { student ->
                appendLine("- ${student.name}: ${student.pendingBalance} euros")
            }
        }
    }

    /**
     * Returns the count of active students.
     * Intended for summary queries like "how many active students do I have".
     */
    fun getActiveStudentCount(): String = runBlocking {
        val count = queryStudentsUseCase.getActiveStudentCount()
        "Actualmente tienes $count alumnos activos."
    }
}
