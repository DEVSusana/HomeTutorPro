package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.core.auth.SecureAuthManager
import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.AgentStudentDetail
import com.devsusana.hometutorpro.domain.entities.AgentStudentSummary
import com.devsusana.hometutorpro.domain.entities.PaymentType
import com.devsusana.hometutorpro.domain.entities.SueOperationResult
import com.devsusana.hometutorpro.domain.entities.SuePendingAction
import com.devsusana.hometutorpro.domain.usecases.IAddToBalanceUseCase
import com.devsusana.hometutorpro.domain.usecases.IQueryStudentsForAgentUseCase
import com.devsusana.hometutorpro.domain.usecases.IRegisterPaymentUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
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
    private lateinit var secureAuthManager: SecureAuthManager
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

    @Before
    fun setup() {
        queryStudentsUseCase = mockk()
        registerPaymentUseCase = mockk()
        addToBalanceUseCase = mockk()
        secureAuthManager = mockk()

        studentTools = StudentTools(
            queryStudentsUseCase = queryStudentsUseCase,
            registerPaymentUseCase = registerPaymentUseCase,
            addToBalanceUseCase = addToBalanceUseCase,
            secureAuthManager = secureAuthManager
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
    fun `extractRelevantStudent does not match very short names (less than 3 chars)`() = runTest {
        val shortNameStudentSummary = AgentStudentSummary("Li Wang", "Chino", "Adultos", 25.0, 0.0, true, null)
        coEvery { queryStudentsUseCase.getAllStudents() } returns listOf(shortNameStudentSummary)

        // "Li" is 2 chars — should NOT match (threshold is 3 chars for exact match)
        val result = studentTools.extractRelevantStudent("quiero la información de Li Wang")

        // "Li" has 2 chars → first-name branch requires >= 3 → no match from exact pass
        // partial fallback requires >= 4 → also no match
        assertNull(result)
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
        every { secureAuthManager.getUserId() } returns null

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
        every { secureAuthManager.getUserId() } returns "prof-1"
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

    @Test
    fun `executeRegisterPayment returns Execute Error on domain error`() = runTest {
        every { secureAuthManager.getUserId() } returns "prof-1"
        coEvery {
            registerPaymentUseCase(any(), any(), any(), any())
        } returns Result.Error(DomainError.InvalidAmount)

        val action = SuePendingAction.RegisterPayment(
            studentName = "María García",
            studentId = "stu-1",
            amount = -5.0,
            paymentType = PaymentType.EFFECTIVE
        )

        val result = studentTools.executeRegisterPayment(action)

        assertTrue(result is SueOperationResult.Execute.Error)
        assertEquals(DomainError.InvalidAmount, (result as SueOperationResult.Execute.Error).domainError)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // prepareAddBalance / executeAddBalance
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `prepareAddBalance returns Prepare Success with correct amount`() = runTest {
        coEvery { queryStudentsUseCase.searchByName("Juan") } returns listOf(juanDetail)

        val result = studentTools.prepareAddBalance("Juan", 15.5)

        assertTrue(result is SueOperationResult.Prepare.Success)
        val action = (result as SueOperationResult.Prepare.Success).action as SuePendingAction.AddBalance
        assertEquals(15.5, action.amount, 0.001)
        assertEquals("Juan López", action.studentName)
    }

    @Test
    fun `executeAddBalance returns Execute Success on success`() = runTest {
        every { secureAuthManager.getUserId() } returns "prof-1"
        coEvery { addToBalanceUseCase(any(), any(), any()) } returns Result.Success(Unit)

        val action = SuePendingAction.AddBalance(
            studentName = "Juan López",
            studentId = "stu-2",
            amount = 15.5
        )

        val result = studentTools.executeAddBalance(action)

        assertTrue(result is SueOperationResult.Execute.Success)
    }
}
