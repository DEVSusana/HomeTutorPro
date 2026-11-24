package com.devsusana.hometutorpro.domain.usecases.implementations.sue

import com.devsusana.hometutorpro.domain.entities.AgentBalanceSummary
import com.devsusana.hometutorpro.domain.entities.AgentStudentDetail
import com.devsusana.hometutorpro.domain.entities.PaymentType
import com.devsusana.hometutorpro.domain.entities.SueOperationResult
import com.devsusana.hometutorpro.domain.entities.SuePendingAction
import com.devsusana.hometutorpro.domain.usecases.implementations.StudentTools
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SueFinanceIntentHandlerTest {

    private lateinit var studentTools: StudentTools
    private lateinit var context: SueConversationContext
    private lateinit var handler: SueFinanceIntentHandler

    @Before
    fun setUp() {
        studentTools = mockk(relaxed = true)
        context = SueConversationContext()
        handler = SueFinanceIntentHandler(studentTools, context)
    }

    @Test
    fun `handleRegisterPayment prompts for student when missing`() = runTest {
        val result = handler.handleRegisterPayment("registra un pago de 20 euros")
        assertTrue(result is SueOperationResult.Prepare.Error)
        assertEquals(SueOperationResult.ErrorType.STUDENT_NOT_FOUND, (result as SueOperationResult.Prepare.Error).errorType)
        assertEquals(SlotType.STUDENT_NAME, context.activeIncompleteIntent?.missingSlot)
    }

    @Test
    fun `handleRegisterPayment prompts for amount when student is present but amount missing`() = runTest {
        context.lastMentionedStudentName = "Ana"
        val result = handler.handleRegisterPayment("registra un pago de ana")
        assertTrue(result is SueOperationResult.Prepare.Error)
        assertEquals(SlotType.AMOUNT, context.activeIncompleteIntent?.missingSlot)
    }

    @Test
    fun `handleRegisterPayment uses BIZUM when query mentions bizum and EFFECTIVE otherwise`() = runTest {
        context.lastMentionedStudentName = "Ana"
        context.lastMentionedAmount = 25.0
        val pending = SuePendingAction.RegisterPayment("1", "Ana", 25.0, PaymentType.BIZUM)
        coEvery { studentTools.prepareRegisterPayment("Ana", 25.0, PaymentType.BIZUM) } returns SueOperationResult.Prepare.Success(pending)
        coEvery { studentTools.prepareRegisterPayment("Ana", 25.0, PaymentType.EFFECTIVE) } returns SueOperationResult.Prepare.Success(pending.copy(paymentType = PaymentType.EFFECTIVE))

        val bizumResult = handler.handleRegisterPayment("ana pago 25 por bizum")
        assertTrue(bizumResult is SueOperationResult.Prepare.Success)
        coVerify { studentTools.prepareRegisterPayment("Ana", 25.0, PaymentType.BIZUM) }

        val cashResult = handler.handleRegisterPayment("ana pago 25 en efectivo")
        assertTrue(cashResult is SueOperationResult.Prepare.Success)
        coVerify { studentTools.prepareRegisterPayment("Ana", 25.0, PaymentType.EFFECTIVE) }
    }

    @Test
    fun `handleAddBalance handles missing slots and prepares balance addition`() = runTest {
        val noStudent = handler.handleAddBalance()
        assertEquals(SlotType.STUDENT_NAME, context.activeIncompleteIntent?.missingSlot)
        assertTrue(noStudent is SueOperationResult.Prepare.Error)

        context.lastMentionedStudentName = "Darío"
        val noAmount = handler.handleAddBalance()
        assertEquals(SlotType.AMOUNT, context.activeIncompleteIntent?.missingSlot)
        assertTrue(noAmount is SueOperationResult.Prepare.Error)

        context.lastMentionedAmount = 30.0
        val pending = SuePendingAction.AddBalance("2", "Darío", 30.0)
        coEvery { studentTools.prepareAddBalance("Darío", 30.0) } returns SueOperationResult.Prepare.Success(pending)
        val ok = handler.handleAddBalance()
        assertTrue(ok is SueOperationResult.Prepare.Success)
    }

    @Test
    fun `handleQueryStudentsWithBalance delegates to studentTools`() = runTest {
        val expected = SueOperationResult.StudentsWithBalance(listOf(AgentBalanceSummary("Ana", 40.0)))
        coEvery { studentTools.getStudentsWithBalance() } returns expected
        assertEquals(expected, handler.handleQueryStudentsWithBalance())
    }

    @Test
    fun `handleQueryStudentBalance handles missing student, zero balance, positive balance, and not found`() = runTest {
        val missing = handler.handleQueryStudentBalance() as SueOperationResult.ReadSuccess
        assertTrue(missing.message.contains("qué alumno"))

        context.lastMentionedStudentName = "Ana"
        coEvery { studentTools.searchStudent("Ana") } returns SueOperationResult.StudentDetails(
            "Ana",
            listOf(AgentStudentDetail("1", "Ana", "Mates", "4º ESO", pendingBalance = 0.0))
        )
        val zero = handler.handleQueryStudentBalance() as SueOperationResult.ReadSuccess
        assertTrue(zero.message.contains("al corriente"))

        coEvery { studentTools.searchStudent("Ana") } returns SueOperationResult.StudentDetails(
            "Ana",
            listOf(AgentStudentDetail("1", "Ana", "Mates", "4º ESO", pendingBalance = 45.5))
        )
        val positive = handler.handleQueryStudentBalance() as SueOperationResult.ReadSuccess
        assertTrue(positive.message.contains("45"))
        assertEquals(45.5, context.lastMentionedAmount ?: 0.0, 0.01)

        coEvery { studentTools.searchStudent("Ana") } returns SueOperationResult.StudentDetails("Ana", emptyList())
        val notFound = handler.handleQueryStudentBalance() as SueOperationResult.ReadSuccess
        assertTrue(notFound.message.contains("No he encontrado"))
    }
}
