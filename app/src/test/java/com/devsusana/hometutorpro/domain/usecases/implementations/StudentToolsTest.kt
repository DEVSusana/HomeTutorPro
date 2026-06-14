package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.entities.User
import com.devsusana.hometutorpro.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.AgentStudentDetail
import com.devsusana.hometutorpro.domain.entities.AgentStudentSummary
import com.devsusana.hometutorpro.domain.entities.PaymentType
import com.devsusana.hometutorpro.domain.entities.SueOperationResult
import com.devsusana.hometutorpro.domain.entities.SuePendingAction
import com.devsusana.hometutorpro.domain.entities.Student
import com.devsusana.hometutorpro.domain.usecases.IAddToBalanceUseCase
import com.devsusana.hometutorpro.domain.usecases.IQueryStudentsForAgentUseCase
import com.devsusana.hometutorpro.domain.usecases.IRegisterPaymentUseCase
import com.devsusana.hometutorpro.domain.usecases.ISaveStudentUseCase
import com.devsusana.hometutorpro.domain.usecases.IDeleteStudentUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetStudentByIdUseCase
import com.devsusana.hometutorpro.domain.usecases.IScheduleClassEndNotificationUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [StudentTools].
 *
 * All dependencies are mocked so tests run in complete isolation.
 */
class StudentToolsTest {

    private lateinit var queryStudentsUseCase: IQueryStudentsForAgentUseCase
    private lateinit var registerPaymentUseCase: IRegisterPaymentUseCase
    private lateinit var addToBalanceUseCase: IAddToBalanceUseCase
    private lateinit var saveStudentUseCase: ISaveStudentUseCase
    private lateinit var deleteStudentUseCase: IDeleteStudentUseCase
    private lateinit var getStudentByIdUseCase: IGetStudentByIdUseCase
    private lateinit var scheduleClassEndNotificationUseCase: IScheduleClassEndNotificationUseCase
    private lateinit var authRepository: AuthRepository
    private lateinit var studentTools: StudentTools

    private val mariaDetail = AgentStudentDetail(
        studentId = "stu-1",
        name = "María García",
        subjects = "Matemáticas",
        course = "3º ESO",
        pendingBalance = 40.0,
        lastPaymentDate = null
    )

    private val mariaSummary = AgentStudentSummary(
        name = "María García",
        subjects = "Matemáticas",
        course = "3º ESO",
        pricePerHour = 15.0,
        pendingBalance = 40.0,
        isActive = true,
        lastPaymentDate = null
    )

    private val juanDetail = AgentStudentDetail(
        studentId = "stu-2",
        name = "Juan López",
        subjects = "Inglés",
        course = "1º Bachillerato",
        pendingBalance = 0.0,
        lastPaymentDate = System.currentTimeMillis()
    )

    private val juanSummary = AgentStudentSummary(
        name = "Juan López",
        subjects = "Inglés",
        course = "1º Bachillerato",
        pricePerHour = 20.0,
        pendingBalance = 0.0,
        isActive = true,
        lastPaymentDate = System.currentTimeMillis()
    )

    private val fullStudent = Student(
        id = "stu-1",
        professorId = "prof-1",
        name = "María García",
        subjects = "Matemáticas",
        course = "3º ESO",
        pricePerHour = 15.0,
        pendingBalance = 40.0,
        isActive = true
    )

    @Before
    fun setup() {
        queryStudentsUseCase = mockk()
        registerPaymentUseCase = mockk()
        addToBalanceUseCase = mockk()
        saveStudentUseCase = mockk()
        deleteStudentUseCase = mockk()
        getStudentByIdUseCase = mockk()
        scheduleClassEndNotificationUseCase = mockk(relaxed = true)
        authRepository = mockk()

        studentTools = StudentTools(
            queryStudentsUseCase = queryStudentsUseCase,
            registerPaymentUseCase = registerPaymentUseCase,
            addToBalanceUseCase = addToBalanceUseCase,
            saveStudentUseCase = saveStudentUseCase,
            deleteStudentUseCase = deleteStudentUseCase,
            getStudentByIdUseCase = getStudentByIdUseCase,
            scheduleClassEndNotificationUseCase = scheduleClassEndNotificationUseCase,
            authRepository = authRepository
        )
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getActiveStudentCount
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `getActiveStudentCount returns correct count`() = runTest {
        coEvery { queryStudentsUseCase.getActiveStudentCount() } returns 7

        val result = studentTools.getActiveStudentCount()

        assertTrue(result is SueOperationResult.ActiveStudentCount)
        assertEquals(7, (result as SueOperationResult.ActiveStudentCount).count)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // extractRelevantStudent
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `extractRelevantStudent returns match for first name in query`() = runTest {
        coEvery { queryStudentsUseCase.getAllStudents() } returns listOf(mariaSummary, juanSummary)
        coEvery { queryStudentsUseCase.searchByName("María García") } returns listOf(mariaDetail)

        val result = studentTools.extractRelevantStudent("información de María")

        assertEquals(mariaDetail, result)
    }

    @Test
    fun `extractRelevantStudent returns null when name not in query`() = runTest {
        coEvery { queryStudentsUseCase.getAllStudents() } returns listOf(mariaSummary, juanSummary)

        val result = studentTools.extractRelevantStudent("cuántos alumnos tengo")

        assertNull(result)
    }

    @Test
    fun `extractRelevantStudent returns match for phonetically similar name`() = runTest {
        val christianSummary = AgentStudentSummary(
            name = "Christian Smith",
            subjects = "Math",
            course = "ESO",
            pricePerHour = 20.0,
            pendingBalance = 0.0,
            isActive = true,
            lastPaymentDate = null
        )
        val christianDetail = AgentStudentDetail(
            studentId = "stu-3",
            name = "Christian Smith",
            subjects = "Math",
            course = "ESO",
            pendingBalance = 0.0,
            lastPaymentDate = null
        )
        coEvery { queryStudentsUseCase.getAllStudents() } returns listOf(mariaSummary, juanSummary, christianSummary)
        coEvery { queryStudentsUseCase.searchByName("Christian Smith") } returns listOf(christianDetail)

        val result = studentTools.extractRelevantStudent("mueve la clase de Cristian")

        assertEquals(christianDetail, result)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // prepareRegisterPayment
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `prepareRegisterPayment returns Prepare Error when student not found`() = runTest {
        coEvery { queryStudentsUseCase.searchByName("Desconocido") } returns emptyList()

        val result = studentTools.prepareRegisterPayment("Desconocido", 20.0, PaymentType.EFFECTIVE)

        assertTrue(result is SueOperationResult.Prepare.Error)
        assertEquals(SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
            (result as SueOperationResult.Prepare.Error).errorType)
    }

    @Test
    fun `prepareRegisterPayment returns Prepare Success with resolved action`() = runTest {
        coEvery { queryStudentsUseCase.searchByName("María") } returns listOf(mariaDetail)

        val result = studentTools.prepareRegisterPayment("María", 30.0, PaymentType.BIZUM)

        assertTrue(result is SueOperationResult.Prepare.Success)
        val action = (result as SueOperationResult.Prepare.Success).action
        assertTrue(action is SuePendingAction.RegisterPayment)
        val payment = action as SuePendingAction.RegisterPayment
        assertEquals("María García", payment.studentName)
        assertEquals(30.0, payment.amount, 0.001)
        assertEquals(PaymentType.BIZUM, payment.paymentType)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // executeRegisterPayment
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `executeRegisterPayment returns AuthError when no session`() = runTest {
        every { authRepository.currentUser } returns MutableStateFlow(null)

        val action = SuePendingAction.RegisterPayment(
            studentName = "María García",
            studentId = "stu-1",
            amount = 30.0,
            paymentType = PaymentType.EFFECTIVE
        )

        val result = studentTools.executeRegisterPayment(action)

        assertEquals(SueOperationResult.Execute.AuthError, result)
    }

    @Test
    fun `executeRegisterPayment returns Execute Success on success`() = runTest {
        every { authRepository.currentUser } returns MutableStateFlow(User(uid = "prof-1", email = "test@example.com", displayName = "Professor"))
        coEvery { registerPaymentUseCase(any(), any(), any(), any()) } returns Result.Success(Unit)

        val action = SuePendingAction.RegisterPayment(
            studentName = "María García",
            studentId = "stu-1",
            amount = 30.0,
            paymentType = PaymentType.EFFECTIVE
        )

        val result = studentTools.executeRegisterPayment(action)

        assertTrue(result is SueOperationResult.Execute.Success)
        assertEquals(action, (result as SueOperationResult.Execute.Success).action)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // prepareStartClass & executeStartClass
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `prepareStartClass returns Success when student exists`() = runTest {
        coEvery { queryStudentsUseCase.searchByName("María") } returns listOf(mariaDetail)

        val result = studentTools.prepareStartClass("María", 45)

        assertTrue(result is SueOperationResult.Prepare.Success)
        val action = (result as SueOperationResult.Prepare.Success).action as SuePendingAction.StartClass
        assertEquals("María García", action.studentName)
        assertEquals(45, action.durationMinutes)
    }

    @Test
    fun `executeStartClass updates balance and schedules notification`() = runTest {
        every { authRepository.currentUser } returns MutableStateFlow(User(uid = "prof-1", email = "test@example.com", displayName = "Professor"))
        coEvery { getStudentByIdUseCase("prof-1", "stu-1") } returns flowOf(fullStudent)
        coEvery { saveStudentUseCase("prof-1", any()) } returns Result.Success("stu-1")

        val action = SuePendingAction.StartClass(
            studentName = "María García",
            studentId = "stu-1",
            durationMinutes = 60
        )

        val result = studentTools.executeStartClass(action)

        assertTrue(result is SueOperationResult.Execute.Success)
        // 60 minutes of Maria (15.0 per hour) adds 15.0 to her balance (40.0 + 15.0 = 55.0)
        coVerify { saveStudentUseCase("prof-1", withArg {
            assertEquals(55.0, it.pendingBalance, 0.001)
        })}
        coVerify { scheduleClassEndNotificationUseCase("María García", 60L) }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // prepareCreateStudent & executeCreateStudent
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `prepareCreateStudent returns Success for valid data`() = runTest {
        val result = studentTools.prepareCreateStudent("Carlos", "ESO", "Física", 18.0)

        assertTrue(result is SueOperationResult.Prepare.Success)
        val action = (result as SueOperationResult.Prepare.Success).action as SuePendingAction.CreateStudent
        assertEquals("Carlos", action.name)
        assertEquals(18.0, action.pricePerHour, 0.001)
    }

    @Test
    fun `executeCreateStudent saves student with default valid values`() = runTest {
        every { authRepository.currentUser } returns MutableStateFlow(User(uid = "prof-1", email = "test@example.com", displayName = "Professor"))
        coEvery { saveStudentUseCase("prof-1", any()) } returns Result.Success("stu-new")

        val action = SuePendingAction.CreateStudent("Carlos", "ESO", "Física", 18.0)

        val result = studentTools.executeCreateStudent(action)

        assertTrue(result is SueOperationResult.Execute.Success)
        coVerify { saveStudentUseCase("prof-1", withArg {
            assertEquals("Carlos", it.name)
            assertEquals("ESO", it.course)
            assertEquals("Física", it.subjects)
            assertEquals(18.0, it.pricePerHour, 0.001)
            assertEquals("No address", it.address)
            assertEquals("carlos@example.com", it.studentEmail)
        })}
    }

    // ──────────────────────────────────────────────────────────────────────────
    // prepareDeleteStudent & executeDeleteStudent
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `prepareDeleteStudent returns Success for valid student`() = runTest {
        coEvery { queryStudentsUseCase.searchByName("María") } returns listOf(mariaDetail)

        val result = studentTools.prepareDeleteStudent("María")

        assertTrue(result is SueOperationResult.Prepare.Success)
        val action = (result as SueOperationResult.Prepare.Success).action as SuePendingAction.DeleteStudent
        assertEquals("María García", action.studentName)
    }

    @Test
    fun `executeDeleteStudent invokes deleteStudentUseCase`() = runTest {
        every { authRepository.currentUser } returns MutableStateFlow(User(uid = "prof-1", email = "test@example.com", displayName = "Professor"))
        coEvery { deleteStudentUseCase("prof-1", "stu-1") } returns Result.Success(Unit)

        val action = SuePendingAction.DeleteStudent("María García", "stu-1")

        val result = studentTools.executeDeleteStudent(action)

        assertTrue(result is SueOperationResult.Execute.Success)
        coVerify { deleteStudentUseCase("prof-1", "stu-1") }
    }
}

