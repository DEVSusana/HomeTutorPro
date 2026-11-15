package com.devsusana.hometutorpro.domain.usecases.implementations.sue

import com.devsusana.hometutorpro.domain.entities.PaymentType
import com.devsusana.hometutorpro.domain.entities.SueOperationResult
import com.devsusana.hometutorpro.domain.usecases.implementations.StudentTools
import javax.inject.Inject

/**
 * Handles finance-related intents such as registering payments, adding balance, and querying debt.
 */
class SueFinanceIntentHandler @Inject constructor(
    private val studentTools: StudentTools,
    private val conversationContext: SueConversationContext
) {

    suspend fun handleRegisterPayment(lower: String): SueOperationResult {
        val studentName = conversationContext.lastMentionedStudentName
        return if (studentName == null) {
            conversationContext.setAwaitingSlot(IntentType.REGISTER_PAYMENT, SlotType.STUDENT_NAME)
            SueOperationResult.Prepare.Error(
                SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
                "¿De qué alumno quieres registrar el pago?"
            )
        } else {
            val amount = conversationContext.lastMentionedAmount
            if (amount == null) {
                conversationContext.setAwaitingSlot(IntentType.REGISTER_PAYMENT, SlotType.AMOUNT)
                SueOperationResult.Prepare.Error(
                    SueOperationResult.ErrorType.UNKNOWN,
                    "¿De cuánto es el pago de $studentName?"
                )
            } else {
                val paymentType = if (lower.contains("bizum")) PaymentType.BIZUM else PaymentType.EFFECTIVE
                studentTools.prepareRegisterPayment(studentName, amount, paymentType)
            }
        }
    }

    suspend fun handleAddBalance(): SueOperationResult {
        val studentName = conversationContext.lastMentionedStudentName
        return if (studentName == null) {
            conversationContext.setAwaitingSlot(IntentType.ADD_BALANCE, SlotType.STUDENT_NAME)
            SueOperationResult.Prepare.Error(
                SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
                "¿A qué alumno le quieres sumar saldo?"
            )
        } else {
            val amount = conversationContext.lastMentionedAmount
            if (amount == null) {
                conversationContext.setAwaitingSlot(IntentType.ADD_BALANCE, SlotType.AMOUNT)
                SueOperationResult.Prepare.Error(
                    SueOperationResult.ErrorType.UNKNOWN,
                    "¿Cuánto saldo deseas sumarle a $studentName?"
                )
            } else {
                studentTools.prepareAddBalance(studentName, amount)
            }
        }
    }

    suspend fun handleQueryStudentsWithBalance(): SueOperationResult {
        return studentTools.getStudentsWithBalance()
    }

    suspend fun handleQueryStudentBalance(): SueOperationResult {
        val studentName = conversationContext.lastMentionedStudentName
            ?: return SueOperationResult.ReadSuccess("¿De qué alumno quieres saber el saldo pendiente?")

        val details = studentTools.searchStudent(studentName)
        return if (details is SueOperationResult.StudentDetails && details.students.isNotEmpty()) {
            val student = details.students.first()
            conversationContext.lastMentionedStudentName = student.name
            conversationContext.lastMentionedAmount = student.pendingBalance
            if (student.pendingBalance <= 0.0) {
                SueOperationResult.ReadSuccess("${student.name} está al corriente de pago. No tiene ningún saldo pendiente.")
            } else {
                SueOperationResult.ReadSuccess("${student.name} tiene un saldo pendiente de ${"%.2f".format(student.pendingBalance)} €.")
            }
        } else {
            SueOperationResult.ReadSuccess("No he encontrado ningún alumno llamado \"$studentName\".")
        }
    }
}
