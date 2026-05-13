package com.devsusana.hometutorpro.core.sue.tools

import com.devsusana.hometutorpro.core.auth.SecureAuthManager
import com.devsusana.hometutorpro.core.sue.SuePendingAction
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.PaymentType
import com.devsusana.hometutorpro.domain.usecases.IAddToBalanceUseCase
import com.devsusana.hometutorpro.domain.usecases.IQueryStudentsForAgentUseCase
import com.devsusana.hometutorpro.domain.usecases.IRegisterPaymentUseCase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Koog-compatible tool definitions for student-related queries and finance operations.
 *
 * All functions are `suspend` so they execute on the coroutine dispatcher
 * chosen by the caller (typically [Dispatchers.IO] via the ViewModel's
 * [viewModelScope]).
 */
@Singleton
class StudentTools @Inject constructor(
    private val queryStudentsUseCase: IQueryStudentsForAgentUseCase,
    private val registerPaymentUseCase: IRegisterPaymentUseCase,
    private val addToBalanceUseCase: IAddToBalanceUseCase,
    private val secureAuthManager: SecureAuthManager
) {

    /**
     * Returns a text summary of all students (name, subjects, course, price, balance, status).
     * Intended for broad queries like "list my students" or "how many students do I have".
     */
    suspend fun getAllStudentsSummary(): String {
        val students = queryStudentsUseCase.getAllStudents()
        if (students.isEmpty()) return "No se encontraron alumnos."

        return buildString {
            appendLine("Alumnos (${students.size} en total):")
            appendLine()
            students.forEach { student ->
                val status = if (student.isActive) "Activo" else "Inactivo"
                appendLine("• ${student.name}")
                appendLine("  Asignaturas: ${student.subjects}")
                appendLine("  Curso: ${student.course}")
                appendLine("  Precio: ${student.pricePerHour} euros la hora")
                appendLine("  Saldo pendiente: ${student.pendingBalance} euros")
                if (student.lastPaymentDate != null) {
                    val date = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                        .format(java.util.Date(student.lastPaymentDate))
                    appendLine("  Último pago: $date")
                } else {
                    appendLine("  Último pago: Nunca")
                }
                appendLine("  Estado: $status")
                appendLine()
            }
        }
    }

    /**
     * Searches students by name and returns their details.
     * Intended for targeted queries like "tell me about María".
     *
     * @param query Partial or full student name as spoken by the user.
     */
    suspend fun searchStudent(query: String): String {
        val results = queryStudentsUseCase.searchByName(query)
        if (results.isEmpty()) return "No se encontraron alumnos con el nombre '$query'."

        return buildString {
            val header = if (results.size == 1) results.first().name else "'$query'"
            appendLine("Información de $header (${results.size} encontrado${if (results.size != 1) "s" else ""}):")
            appendLine()
            results.forEach { student ->
                appendLine("• ${student.name}")
                appendLine("  Asignaturas: ${student.subjects}")
                appendLine("  Curso: ${student.course}")
                appendLine("  Saldo pendiente: ${student.pendingBalance} euros")
                if (student.lastPaymentDate != null) {
                    val date = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                        .format(java.util.Date(student.lastPaymentDate))
                    appendLine("  Último pago: $date")
                } else {
                    appendLine("  Último pago: Nunca")
                }
                appendLine()
            }
        }
    }

    /**
     * Returns students with non-zero pending balance.
     * Intended for financial queries like "who owes me money".
     */
    suspend fun getStudentsWithBalance(): String {
        val students = queryStudentsUseCase.getStudentsWithBalance()
        if (students.isEmpty()) return "Ningún alumno tiene saldo pendiente."

        return buildString {
            appendLine("Alumnos con saldo pendiente (${students.size}):")
            students.forEach { student ->
                appendLine("- ${student.name}: ${student.pendingBalance} euros")
            }
        }
    }

    /**
     * Checks if any student's name is mentioned in the query and returns their info.
     *
     * Uses a two-pass strategy to avoid false positives:
     * 1. Exact first-name word-boundary match (case-insensitive regex).
     * 2. Partial match fallback with a minimum of 4 characters.
     *
     * Returns null when no student name is detected — the caller MUST NOT fall
     * back to dumping all students when this returns null.
     *
     * @param query The lowercased user query.
     */
    suspend fun extractRelevantStudentContext(query: String): String? {
        val students = queryStudentsUseCase.getAllStudents()

        // Pass 1 — exact first-name word-boundary match
        val exactMatch = students.find { student ->
            val firstName = student.name.substringBefore(" ").lowercase()
            firstName.length >= 3 && Regex("\\b${Regex.escape(firstName)}\\b").containsMatchIn(query)
        }

        val matchedStudent = exactMatch
            ?: students.find { student ->
                // Pass 2 — partial match (higher minimum to reduce false positives)
                val firstName = student.name.substringBefore(" ").lowercase()
                firstName.length >= 4 && query.contains(firstName)
            }

        return matchedStudent?.let { searchStudent(it.name) }
    }

    /**
     * Returns the count of active students.
     * Intended for summary queries like "how many active students do I have".
     */
    suspend fun getActiveStudentCount(): String {
        val count = queryStudentsUseCase.getActiveStudentCount()
        return "Actualmente tienes $count alumnos activos."
    }
    /**
     * Prepares a RegisterPayment action for user confirmation.
     */
    suspend fun prepareRegisterPayment(
        studentName: String,
        amount: Double,
        paymentType: PaymentType
    ): Pair<SuePendingAction.RegisterPayment?, String> {
        val results = queryStudentsUseCase.searchByName(studentName)
        val match = results.firstOrNull { it.name.lowercase().contains(studentName.lowercase()) }

        if (match == null) {
            return Pair(null, "No he encontrado ningún alumno llamado $studentName para registrar el pago.")
        }

        val typeStr = if (paymentType == PaymentType.BIZUM) "por Bizum" else "en efectivo"
        val action = SuePendingAction.RegisterPayment(
            studentName = match.name,
            studentId = match.studentId,
            amount = amount,
            paymentType = paymentType
        )
        val confirmText = "¿Confirmas registrar un pago de $amount euros de ${match.name} $typeStr? " +
                "Di sí para confirmar o no para cancelar."
        
        return Pair(action, confirmText)
    }

    /**
     * Executes a confirmed RegisterPayment action.
     */
    suspend fun executeRegisterPayment(action: SuePendingAction.RegisterPayment): String {
        val professorId = secureAuthManager.getUserId()
            ?: return "No se pudo registrar el pago: no hay sesión activa."

        return when (val result = registerPaymentUseCase(
            professorId = professorId,
            studentId = action.studentId,
            amountPaid = action.amount,
            paymentType = action.paymentType
        )) {
            is Result.Success -> "Hecho. Se ha registrado el pago de ${action.amount} euros de ${action.studentName}."
            is Result.Error -> "No se pudo registrar el pago: ${result.error}."
        }
    }

    /**
     * Prepares an AddBalance action for user confirmation.
     */
    suspend fun prepareAddBalance(
        studentName: String,
        amount: Double
    ): Pair<SuePendingAction.AddBalance?, String> {
        val results = queryStudentsUseCase.searchByName(studentName)
        val match = results.firstOrNull { it.name.lowercase().contains(studentName.lowercase()) }

        if (match == null) {
            return Pair(null, "No he encontrado ningún alumno llamado $studentName para añadirle saldo.")
        }

        val action = SuePendingAction.AddBalance(
            studentName = match.name,
            studentId = match.studentId,
            amount = amount
        )
        val confirmText = "¿Confirmas sumar $amount euros a la deuda de ${match.name}? " +
                "Di sí para confirmar o no para cancelar."
        
        return Pair(action, confirmText)
    }

    /**
     * Executes a confirmed AddBalance action.
     */
    suspend fun executeAddBalance(action: SuePendingAction.AddBalance): String {
        val professorId = secureAuthManager.getUserId()
            ?: return "No se pudo actualizar el saldo: no hay sesión activa."

        return when (val result = addToBalanceUseCase(
            professorId = professorId,
            studentId = action.studentId,
            amount = action.amount
        )) {
            is Result.Success -> "Hecho. Se han sumado ${action.amount} euros a la cuenta de ${action.studentName}."
            is Result.Error -> "No se pudo actualizar el saldo: ${result.error}."
        }
    }
}
