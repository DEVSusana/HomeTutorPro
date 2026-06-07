package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.entities.AgentStudentDetail
import com.devsusana.hometutorpro.domain.entities.PaymentType
import com.devsusana.hometutorpro.domain.entities.SueOperationResult
import com.devsusana.hometutorpro.domain.entities.SuePendingAction
import com.devsusana.hometutorpro.domain.entities.Student
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.devsusana.hometutorpro.domain.usecases.IAddToBalanceUseCase
import com.devsusana.hometutorpro.domain.usecases.IQueryStudentsForAgentUseCase
import com.devsusana.hometutorpro.domain.usecases.IRegisterPaymentUseCase
import com.devsusana.hometutorpro.domain.usecases.ISaveStudentUseCase
import com.devsusana.hometutorpro.domain.usecases.IDeleteStudentUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetStudentByIdUseCase
import com.devsusana.hometutorpro.domain.usecases.IScheduleClassEndNotificationUseCase
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tool definitions for student-related queries and finance operations.
 *
 * All functions are `suspend` to comply with AGENTS.md Rule 1 (Coroutines),
 * executing database I/O on background threads.
 */
@Singleton
class StudentTools @Inject constructor(
    private val queryStudentsUseCase: IQueryStudentsForAgentUseCase,
    private val registerPaymentUseCase: IRegisterPaymentUseCase,
    private val addToBalanceUseCase: IAddToBalanceUseCase,
    private val saveStudentUseCase: ISaveStudentUseCase,
    private val deleteStudentUseCase: IDeleteStudentUseCase,
    private val getStudentByIdUseCase: IGetStudentByIdUseCase,
    private val scheduleClassEndNotificationUseCase: IScheduleClassEndNotificationUseCase,
    private val authRepository: AuthRepository
) {

    /**
     * Returns a summary of all students.
     */
    suspend fun getAllStudentsSummary(): SueOperationResult {
        val students = queryStudentsUseCase.getAllStudents()
        return SueOperationResult.AllStudentsSummary(students)
    }

    /**
     * Searches students by name and returns their details.
     *
     * @param query Partial or full student name.
     */
    suspend fun searchStudent(query: String): SueOperationResult {
        val results = queryStudentsUseCase.searchByName(query)
        return SueOperationResult.StudentDetails(query, results)
    }

    /**
     * Returns students with non-zero pending balance.
     */
    suspend fun getStudentsWithBalance(): SueOperationResult {
        val students = queryStudentsUseCase.getStudentsWithBalance()
        return SueOperationResult.StudentsWithBalance(students)
    }

    /**
     * Extracts a matched student detail from a natural language query.
     *
     * Uses a two-pass matching strategy:
     * 1. Exact first-name word-boundary match.
     * 2. Partial match fallback (min 4 chars).
     */
    suspend fun extractRelevantStudent(query: String): AgentStudentDetail? {
        val students = queryStudentsUseCase.getAllStudents()
        val lowerQuery = query.lowercase()

        val exactMatch = students.find { student ->
            val firstName = student.name.substringBefore(" ").lowercase()
            firstName.length >= 3 && Regex("\\b${Regex.escape(firstName)}\\b").containsMatchIn(lowerQuery)
        }

        val matchedStudent = exactMatch
            ?: students.find { student ->
                val firstName = student.name.substringBefore(" ").lowercase()
                firstName.length >= 4 && lowerQuery.contains(firstName)
            }

        return matchedStudent?.let { student ->
            queryStudentsUseCase.searchByName(student.name).firstOrNull()
        }
    }

    /**
     * Returns the count of active students.
     */
    suspend fun getActiveStudentCount(): SueOperationResult {
        val count = queryStudentsUseCase.getActiveStudentCount()
        return SueOperationResult.ActiveStudentCount(count)
    }

    /**
     * Prepares a RegisterPayment action for user confirmation.
     */
    suspend fun prepareRegisterPayment(
        studentName: String,
        amount: Double,
        paymentType: PaymentType
    ): SueOperationResult.Prepare {
        val results = queryStudentsUseCase.searchByName(studentName)
        val match = results.firstOrNull { it.name.lowercase().contains(studentName.lowercase()) }

        if (match == null) {
            return SueOperationResult.Prepare.Error(SueOperationResult.ErrorType.STUDENT_NOT_FOUND)
        }

        val action = SuePendingAction.RegisterPayment(
            studentName = match.name,
            studentId = match.studentId,
            amount = amount,
            paymentType = paymentType
        )
        return SueOperationResult.Prepare.Success(action)
    }

    /**
     * Executes a confirmed RegisterPayment action.
     */
    suspend fun executeRegisterPayment(action: SuePendingAction.RegisterPayment): SueOperationResult.Execute {
        val professorId = authRepository.currentUser.value?.uid
            ?: return SueOperationResult.Execute.AuthError

        return when (val result = registerPaymentUseCase(
            professorId = professorId,
            studentId = action.studentId,
            amountPaid = action.amount,
            paymentType = action.paymentType
        )) {
            is Result.Success -> SueOperationResult.Execute.Success(action)
            is Result.Error -> SueOperationResult.Execute.Error(result.error)
        }
    }

    /**
     * Prepares an AddBalance action for user confirmation.
     */
    suspend fun prepareAddBalance(
        studentName: String,
        amount: Double
    ): SueOperationResult.Prepare {
        val results = queryStudentsUseCase.searchByName(studentName)
        val match = results.firstOrNull { it.name.lowercase().contains(studentName.lowercase()) }

        if (match == null) {
            return SueOperationResult.Prepare.Error(SueOperationResult.ErrorType.STUDENT_NOT_FOUND)
        }

        val action = SuePendingAction.AddBalance(
            studentName = match.name,
            studentId = match.studentId,
            amount = amount
        )
        return SueOperationResult.Prepare.Success(action)
    }

    /**
     * Executes a confirmed AddBalance action.
     */
    suspend fun executeAddBalance(action: SuePendingAction.AddBalance): SueOperationResult.Execute {
        val professorId = authRepository.currentUser.value?.uid
            ?: return SueOperationResult.Execute.AuthError

        return when (val result = addToBalanceUseCase(
            professorId = professorId,
            studentId = action.studentId,
            amount = action.amount
        )) {
            is Result.Success -> SueOperationResult.Execute.Success(action)
            is Result.Error -> SueOperationResult.Execute.Error(result.error)
        }
    }

    /**
     * Prepares a StartClass action for user confirmation.
     */
    suspend fun prepareStartClass(
        studentName: String,
        durationMinutes: Int
    ): SueOperationResult.Prepare {
        val results = queryStudentsUseCase.searchByName(studentName)
        val match = results.firstOrNull { it.name.lowercase().contains(studentName.lowercase()) }

        if (match == null) {
            return SueOperationResult.Prepare.Error(SueOperationResult.ErrorType.STUDENT_NOT_FOUND)
        }

        val action = SuePendingAction.StartClass(
            studentName = match.name,
            studentId = match.studentId,
            durationMinutes = durationMinutes
        )
        return SueOperationResult.Prepare.Success(action)
    }

    /**
     * Executes a confirmed StartClass action.
     */
    suspend fun executeStartClass(action: SuePendingAction.StartClass): SueOperationResult.Execute {
        val professorId = authRepository.currentUser.value?.uid
            ?: return SueOperationResult.Execute.AuthError

        val student = getStudentByIdUseCase(professorId, action.studentId).first()
            ?: return SueOperationResult.Execute.Error(DomainError.StudentNotFound)

        val priceToAdd = (action.durationMinutes / 60.0) * student.pricePerHour
        val newBalance = student.pendingBalance + priceToAdd
        val updatedStudent = student.copy(pendingBalance = newBalance)

        return when (val result = saveStudentUseCase(professorId, updatedStudent)) {
            is Result.Success -> {
                scheduleClassEndNotificationUseCase(student.name, action.durationMinutes.toLong())
                SueOperationResult.Execute.Success(action)
            }
            is Result.Error -> SueOperationResult.Execute.Error(result.error)
        }
    }

    /**
     * Prepares a CreateStudent action for user confirmation.
     */
    suspend fun prepareCreateStudent(
        name: String,
        course: String,
        subjects: String,
        pricePerHour: Double
    ): SueOperationResult.Prepare {
        if (name.isBlank()) {
            return SueOperationResult.Prepare.Error(
                SueOperationResult.ErrorType.UNKNOWN,
                "El nombre del alumno no puede estar vacío."
            )
        }
        if (pricePerHour <= 0) {
            return SueOperationResult.Prepare.Error(
                SueOperationResult.ErrorType.UNKNOWN,
                "El precio por hora debe ser mayor que cero."
            )
        }
        val action = SuePendingAction.CreateStudent(
            name = name,
            course = course,
            subjects = subjects,
            pricePerHour = pricePerHour
        )
        return SueOperationResult.Prepare.Success(action)
    }

    /**
     * Executes a confirmed CreateStudent action.
     */
    suspend fun executeCreateStudent(action: SuePendingAction.CreateStudent): SueOperationResult.Execute {
        val professorId = authRepository.currentUser.value?.uid
            ?: return SueOperationResult.Execute.AuthError

        val student = Student(
            professorId = professorId,
            name = action.name,
            course = action.course.ifBlank { "Other" },
            subjects = action.subjects.ifBlank { "General" },
            pricePerHour = action.pricePerHour,
            address = "No address", // Business rule mandatory field
            studentEmail = "${action.name.lowercase().replace(" ", "")}@example.com", // Business rule mandatory field
            isActive = true
        )

        return when (val result = saveStudentUseCase(professorId, student)) {
            is Result.Success -> SueOperationResult.Execute.Success(action)
            is Result.Error -> SueOperationResult.Execute.Error(result.error)
        }
    }

    /**
     * Prepares a DeleteStudent action for user confirmation.
     */
    suspend fun prepareDeleteStudent(studentName: String): SueOperationResult.Prepare {
        val results = queryStudentsUseCase.searchByName(studentName)
        val match = results.firstOrNull { it.name.lowercase().contains(studentName.lowercase()) }

        if (match == null) {
            return SueOperationResult.Prepare.Error(SueOperationResult.ErrorType.STUDENT_NOT_FOUND)
        }

        val action = SuePendingAction.DeleteStudent(
            studentName = match.name,
            studentId = match.studentId
        )
        return SueOperationResult.Prepare.Success(action)
    }

    /**
     * Executes a confirmed DeleteStudent action.
     */
    suspend fun executeDeleteStudent(action: SuePendingAction.DeleteStudent): SueOperationResult.Execute {
        val professorId = authRepository.currentUser.value?.uid
            ?: return SueOperationResult.Execute.AuthError

        return when (val result = deleteStudentUseCase(professorId, action.studentId)) {
            is Result.Success -> SueOperationResult.Execute.Success(action)
            is Result.Error -> SueOperationResult.Execute.Error(result.error)
        }
    }
}

