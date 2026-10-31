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

import com.devsusana.hometutorpro.domain.entities.ActiveSession
import com.devsusana.hometutorpro.domain.usecases.IGetSharedResourcesUseCase
import com.devsusana.hometutorpro.domain.usecases.IStartActiveSessionUseCase
import com.devsusana.hometutorpro.domain.usecases.IStopActiveSessionUseCase
import com.devsusana.hometutorpro.domain.repository.AgentContextRepository
import com.devsusana.hometutorpro.domain.repository.DateTimeProvider
import com.devsusana.hometutorpro.domain.usecases.implementations.sue.ISpanishPhoneticMatcher
import com.devsusana.hometutorpro.domain.usecases.implementations.sue.SpanishPhoneticMatcher

/**
 * Tool definitions for student-related queries and finance operations.
 *
 * All functions are `suspend` to comply with AGENTS.md Rule 1 (Coroutines),
 * executing database I/O on background threads.
 */
class StudentTools @Inject constructor(
    private val queryStudentsUseCase: IQueryStudentsForAgentUseCase,
    private val registerPaymentUseCase: IRegisterPaymentUseCase,
    private val addToBalanceUseCase: IAddToBalanceUseCase,
    private val saveStudentUseCase: ISaveStudentUseCase,
    private val deleteStudentUseCase: IDeleteStudentUseCase,
    private val getStudentByIdUseCase: IGetStudentByIdUseCase,
    private val scheduleClassEndNotificationUseCase: IScheduleClassEndNotificationUseCase,
    private val authRepository: AuthRepository,
    private val getSharedResourcesUseCase: IGetSharedResourcesUseCase,
    private val agentContextRepository: AgentContextRepository,
    private val phoneticMatcher: ISpanishPhoneticMatcher,
    private val dateTimeProvider: DateTimeProvider? = null,
    private val startActiveSessionUseCase: IStartActiveSessionUseCase? = null,
    private val stopActiveSessionUseCase: IStopActiveSessionUseCase? = null
) {

    constructor(
        queryStudentsUseCase: IQueryStudentsForAgentUseCase,
        registerPaymentUseCase: IRegisterPaymentUseCase,
        addToBalanceUseCase: IAddToBalanceUseCase,
        saveStudentUseCase: ISaveStudentUseCase,
        deleteStudentUseCase: IDeleteStudentUseCase,
        getStudentByIdUseCase: IGetStudentByIdUseCase,
        scheduleClassEndNotificationUseCase: IScheduleClassEndNotificationUseCase,
        authRepository: AuthRepository,
        getSharedResourcesUseCase: IGetSharedResourcesUseCase,
        agentContextRepository: AgentContextRepository
    ) : this(
        queryStudentsUseCase = queryStudentsUseCase,
        registerPaymentUseCase = registerPaymentUseCase,
        addToBalanceUseCase = addToBalanceUseCase,
        saveStudentUseCase = saveStudentUseCase,
        deleteStudentUseCase = deleteStudentUseCase,
        getStudentByIdUseCase = getStudentByIdUseCase,
        scheduleClassEndNotificationUseCase = scheduleClassEndNotificationUseCase,
        authRepository = authRepository,
        getSharedResourcesUseCase = getSharedResourcesUseCase,
        agentContextRepository = agentContextRepository,
        phoneticMatcher = SpanishPhoneticMatcher(),
        dateTimeProvider = null,
        startActiveSessionUseCase = null,
        stopActiveSessionUseCase = null
    )

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
    private fun normalizeName(name: String): String = phoneticMatcher.normalizeName(name)

    private fun normalizeConsonants(name: String): String = phoneticMatcher.normalizeConsonants(name)

    suspend fun extractRelevantStudent(query: String): AgentStudentDetail? {
        val students = queryStudentsUseCase.getAllStudents()
        val normalizedQuery = normalizeName(query)
        val queryWords = normalizedQuery.split(Regex("\\s+"))

        val exactMatch = students.find { student ->
            val firstName = student.name.substringBefore(" ")
            val normFirst = normalizeName(firstName)
            normFirst.length >= 3 && Regex("\\b${Regex.escape(normFirst)}\\b").containsMatchIn(normalizedQuery)
        }

        val substringMatch = exactMatch
            ?: students.find { student ->
                val firstName = student.name.substringBefore(" ")
                val normFirst = normalizeName(firstName)
                normFirst.length >= 4 && queryWords.any { it.contains(normFirst) }
            }

        // Tercer paso: Coincidencia fonética por consonantes (consonant skeleton match)
        // Permite casar "Arantxa" con "Arntxa" comparando palabra por palabra
        val matchedStudent = substringMatch
            ?: students.find { student ->
                val firstName = student.name.substringBefore(" ")
                val normFirst = normalizeConsonants(firstName)
                normFirst.length >= 3 && queryWords.any { word ->
                    normalizeConsonants(word) == normFirst
                }
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
                val sessionStartTime = dateTimeProvider?.currentTimeMillis() ?: System.currentTimeMillis()

                startActiveSessionUseCase?.invoke(
                    ActiveSession(
                        studentId = student.id,
                        studentName = student.name,
                        startTimeMillis = sessionStartTime,
                        durationMinutes = action.durationMinutes.toLong(),
                        isOngoing = true,
                        tutorId = professorId
                    )
                )
                // Notification is best-effort; failure to schedule alarm must never cancel the ongoing session
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
            course = action.course.trim(),
            subjects = action.subjects.trim(),
            pricePerHour = action.pricePerHour,
            address = "No address", // Default placeholder for quick voice creation
            studentEmail = "${action.name.lowercase().replace(" ", "")}@example.com", // Default email placeholder for quick voice creation
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

    /**
     * Prepares an action to update a student's notes.
     */
    suspend fun prepareUpdateNotesAction(studentName: String, notes: String): SueOperationResult.Prepare {
        val results = queryStudentsUseCase.searchByName(studentName)
        val match = results.firstOrNull { it.name.lowercase().contains(studentName.lowercase()) }

        if (match == null) {
            return SueOperationResult.Prepare.Error(SueOperationResult.ErrorType.STUDENT_NOT_FOUND)
        }

        val action = SuePendingAction.UpdateStudentNotes(
            studentId = match.studentId,
            studentName = match.name,
            notes = notes
        )
        return SueOperationResult.Prepare.Success(action)
    }

    /**
     * Executes the update student notes action.
     */
    suspend fun executeUpdateNotesAction(action: SuePendingAction.UpdateStudentNotes): SueOperationResult.Execute {
        val professorId = authRepository.currentUser.value?.uid
            ?: return SueOperationResult.Execute.AuthError

        val studentFlow = getStudentByIdUseCase(professorId, action.studentId)
        val student = studentFlow.first()
            ?: return SueOperationResult.Execute.Error(DomainError.StudentNotFound)

        val updatedStudent = student.copy(notes = action.notes)
        return when (val result = saveStudentUseCase(professorId, updatedStudent)) {
            is Result.Success -> SueOperationResult.Execute.Success(action)
            is Result.Error -> SueOperationResult.Execute.Error(result.error)
        }
    }

    /**
     * Prepares an action to update a student's address (including portero / intercom number).
     */
    suspend fun prepareUpdateAddressAction(studentName: String, address: String): SueOperationResult.Prepare {
        val match = findStudentByName(studentName)
            ?: return SueOperationResult.Prepare.Error(SueOperationResult.ErrorType.STUDENT_NOT_FOUND)

        if (address.isBlank()) {
            return SueOperationResult.Prepare.Error(
                SueOperationResult.ErrorType.UNKNOWN,
                "La dirección no puede estar vacía."
            )
        }

        val finalAddress = if (match.address.isNotBlank() &&
            (address.startsWith("Portero:", ignoreCase = true) ||
             address.startsWith("Código:", ignoreCase = true) ||
             address.startsWith("Telefonillo:", ignoreCase = true) ||
             address.startsWith("Intercom:", ignoreCase = true) ||
             address.startsWith("Timbre:", ignoreCase = true)) &&
            !match.address.contains(address, ignoreCase = true)
        ) {
            "${match.address.trim().trimEnd(',')}, $address"
        } else {
            address
        }

        val action = SuePendingAction.UpdateStudentAddress(
            studentId = match.studentId,
            studentName = match.name,
            address = finalAddress
        )
        return SueOperationResult.Prepare.Success(action)
    }

    /**
     * Executes the update student address action.
     */
    suspend fun executeUpdateAddressAction(action: SuePendingAction.UpdateStudentAddress): SueOperationResult.Execute {
        val professorId = authRepository.currentUser.value?.uid
            ?: return SueOperationResult.Execute.AuthError

        val studentFlow = getStudentByIdUseCase(professorId, action.studentId)
        val student = studentFlow.first()
            ?: return SueOperationResult.Execute.Error(DomainError.StudentNotFound)

        val updatedStudent = student.copy(address = action.address)
        return when (val result = saveStudentUseCase(professorId, updatedStudent)) {
            is Result.Success -> SueOperationResult.Execute.Success(action)
            is Result.Error -> SueOperationResult.Execute.Error(result.error)
        }
    }

    /**
     * Prepares an action to update a student's hourly rate / price per hour.
     */
    suspend fun prepareUpdatePriceAction(studentName: String, pricePerHour: Double): SueOperationResult.Prepare {
        if (pricePerHour <= 0.0 || pricePerHour > 500.0) {
            return SueOperationResult.Prepare.Error(
                SueOperationResult.ErrorType.UNKNOWN,
                "El precio por hora debe ser un valor válido mayor que cero."
            )
        }

        val match = findStudentByName(studentName)
            ?: return SueOperationResult.Prepare.Error(SueOperationResult.ErrorType.STUDENT_NOT_FOUND)

        val action = SuePendingAction.UpdateStudentPrice(
            studentId = match.studentId,
            studentName = match.name,
            pricePerHour = pricePerHour
        )
        return SueOperationResult.Prepare.Success(action)
    }

    /**
     * Executes the update student price action.
     */
    suspend fun executeUpdatePriceAction(action: SuePendingAction.UpdateStudentPrice): SueOperationResult.Execute {
        val professorId = authRepository.currentUser.value?.uid
            ?: return SueOperationResult.Execute.AuthError

        val studentFlow = getStudentByIdUseCase(professorId, action.studentId)
        val student = studentFlow.first()
            ?: return SueOperationResult.Execute.Error(DomainError.StudentNotFound)

        val updatedStudent = student.copy(pricePerHour = action.pricePerHour)
        return when (val result = saveStudentUseCase(professorId, updatedStudent)) {
            is Result.Success -> SueOperationResult.Execute.Success(action)
            is Result.Error -> SueOperationResult.Execute.Error(result.error)
        }
    }

    /**
     * Prepares an action to update a student's phone number.
     */
    suspend fun prepareUpdatePhoneAction(studentName: String, phone: String): SueOperationResult.Prepare {
        val cleanPhone = phone.trim()
        if (cleanPhone.isBlank()) {
            return SueOperationResult.Prepare.Error(
                SueOperationResult.ErrorType.UNKNOWN,
                "El teléfono no puede estar vacío."
            )
        }

        val match = findStudentByName(studentName)
            ?: return SueOperationResult.Prepare.Error(SueOperationResult.ErrorType.STUDENT_NOT_FOUND)

        val action = SuePendingAction.UpdateStudentPhone(
            studentId = match.studentId,
            studentName = match.name,
            phone = cleanPhone
        )
        return SueOperationResult.Prepare.Success(action)
    }

    /**
     * Executes the update student phone action.
     */
    suspend fun executeUpdatePhoneAction(action: SuePendingAction.UpdateStudentPhone): SueOperationResult.Execute {
        val professorId = authRepository.currentUser.value?.uid
            ?: return SueOperationResult.Execute.AuthError

        val studentFlow = getStudentByIdUseCase(professorId, action.studentId)
        val student = studentFlow.first()
            ?: return SueOperationResult.Execute.Error(DomainError.StudentNotFound)

        val updatedStudent = student.copy(studentPhone = action.phone)
        return when (val result = saveStudentUseCase(professorId, updatedStudent)) {
            is Result.Success -> SueOperationResult.Execute.Success(action)
            is Result.Error -> SueOperationResult.Execute.Error(result.error)
        }
    }

    /**
     * Prepares an action to update a student's course.
     */
    suspend fun prepareUpdateCourseAction(studentName: String, course: String): SueOperationResult.Prepare {
        val cleanCourse = course.trim()
        if (cleanCourse.isBlank()) {
            return SueOperationResult.Prepare.Error(
                SueOperationResult.ErrorType.UNKNOWN,
                "El curso no puede estar vacío."
            )
        }

        val match = findStudentByName(studentName)
            ?: return SueOperationResult.Prepare.Error(SueOperationResult.ErrorType.STUDENT_NOT_FOUND)

        val action = SuePendingAction.UpdateStudentCourse(
            studentId = match.studentId,
            studentName = match.name,
            course = cleanCourse
        )
        return SueOperationResult.Prepare.Success(action)
    }

    private suspend fun findStudentByName(studentName: String): AgentStudentDetail? {
        val results = queryStudentsUseCase.searchByName(studentName)
        return results.firstOrNull { it.name.lowercase().contains(studentName.lowercase()) }
    }

    /**
     * Executes the update student course action.
     */
    suspend fun executeUpdateCourseAction(action: SuePendingAction.UpdateStudentCourse): SueOperationResult.Execute {
        val professorId = authRepository.currentUser.value?.uid
            ?: return SueOperationResult.Execute.AuthError

        val studentFlow = getStudentByIdUseCase(professorId, action.studentId)
        val student = studentFlow.first()
            ?: return SueOperationResult.Execute.Error(DomainError.StudentNotFound)

        val updatedStudent = student.copy(course = action.course)
        return when (val result = saveStudentUseCase(professorId, updatedStudent)) {
            is Result.Success -> SueOperationResult.Execute.Success(action)
            is Result.Error -> SueOperationResult.Execute.Error(result.error)
        }
    }

    /**
     * Returns the list of resources shared with a student.
     */
    suspend fun getSharedResources(studentName: String): List<com.devsusana.hometutorpro.domain.entities.SharedResource> {
        val professorId = authRepository.currentUser.value?.uid ?: return emptyList()
        val results = queryStudentsUseCase.searchByName(studentName)
        val match = results.firstOrNull { it.name.lowercase().contains(studentName.lowercase()) } ?: return emptyList()
        return getSharedResourcesUseCase(professorId, match.studentId).first()
    }

    /**
     * Returns all resources shared with any student for the current professor.
     */
    suspend fun getAllSharedResources(): List<com.devsusana.hometutorpro.domain.entities.SharedResource> {
        val professorId = authRepository.currentUser.value?.uid ?: return emptyList()
        return getSharedResourcesUseCase(professorId).first()
    }

    /**
     * Returns the list of completed class logs for a student.
     */
    suspend fun getClassLogs(studentName: String): List<com.devsusana.hometutorpro.domain.entities.AgentClassLog> {
        val results = queryStudentsUseCase.searchByName(studentName)
        val match = results.firstOrNull { it.name.lowercase().contains(studentName.lowercase()) } ?: return emptyList()
        return agentContextRepository.getClassLogsForStudent(match.studentId)
    }

    /**
     * Returns the list of transactions for a student.
     */
    suspend fun getTransactions(studentName: String): List<com.devsusana.hometutorpro.domain.entities.AgentTransactionLog> {
        val results = queryStudentsUseCase.searchByName(studentName)
        val match = results.firstOrNull { it.name.lowercase().contains(studentName.lowercase()) } ?: return emptyList()
        return agentContextRepository.getTransactionsForStudent(match.studentId)
    }

    /**
     * Returns all transactions for the current professor.
     */
    suspend fun getAllTransactions(): List<com.devsusana.hometutorpro.domain.entities.AgentTransactionLog> {
        return agentContextRepository.getAllTransactions()
    }

    /**
     * Returns all completed class logs for the current professor across every student.
     * Used to answer global historical queries like "¿cuántas clases he dado este mes?".
     */
    suspend fun getAllClassLogs(): List<com.devsusana.hometutorpro.domain.entities.AgentClassLog> {
        return agentContextRepository.getAllClassLogs()
    }
}


