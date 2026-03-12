package com.devsusana.hometutorpro.presentation.student_detail.delegates

import android.app.Application
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.PaymentType
import com.devsusana.hometutorpro.domain.usecases.IAddToBalanceUseCase
import com.devsusana.hometutorpro.domain.usecases.IRegisterPaymentUseCase
import com.devsusana.hometutorpro.domain.usecases.ISaveStudentUseCase
import com.devsusana.hometutorpro.presentation.student_detail.StudentDetailState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class StudentFinanceDelegate @Inject constructor(
    private val registerPaymentUseCase: IRegisterPaymentUseCase,
    private val addToBalanceUseCase: IAddToBalanceUseCase,
    private val saveStudentUseCase: ISaveStudentUseCase,
    private val application: Application
) : IStudentFinanceDelegate {

    override fun registerPayment(
        professorId: String,
        studentId: String,
        amount: Double,
        paymentType: PaymentType,
        state: MutableStateFlow<StudentDetailState>,
        scope: CoroutineScope
    ) {
        scope.launch {
            state.value = state.value.copy(isLoading = true)
            when (val result = registerPaymentUseCase(professorId, studentId, amount, paymentType)) {
                is Result.Success -> {
                    // Optimistic UI update: reflect the DB changes immediately in memory
                    // so the UI shows the correct balance and lastPaymentDate without
                    // waiting for the Room Flow to re-emit.
                    val currentStudent = state.value.student
                    if (currentStudent != null) {
                        val updatedStudent = currentStudent.copy(
                            pendingBalance = currentStudent.pendingBalance - amount,
                            lastPaymentDate = System.currentTimeMillis()
                        )
                        state.value = state.value.copy(
                            student = updatedStudent,
                            isLoading = false,
                            successMessage = application.getString(R.string.student_detail_success_payment_registered, amount)
                        )
                    } else {
                        state.value = state.value.copy(
                            isLoading = false,
                            successMessage = application.getString(R.string.student_detail_success_payment_registered, amount)
                        )
                    }
                }
                is Result.Error -> {
                    handlePaymentError(result.error, state)
                }
            }
        }
    }

    override fun updateBalance(
        professorId: String,
        studentId: String,
        newBalance: Double,
        state: MutableStateFlow<StudentDetailState>,
        scope: CoroutineScope
    ) {
        scope.launch {
            val currentStudent = state.value.student ?: return@launch
            state.value = state.value.copy(isLoading = true)
            
            val updatedStudent = currentStudent.copy(pendingBalance = newBalance)
            
            when (saveStudentUseCase(professorId, updatedStudent)) {
                is Result.Success -> {
                    state.value = state.value.copy(
                        isLoading = false,
                        isBalanceEditable = false,
                        successMessage = application.getString(R.string.student_detail_success_student_saved)
                    )
                }
                is Result.Error -> {
                    state.value = state.value.copy(
                        isLoading = false,
                        errorMessage = application.getString(R.string.student_detail_error_update_balance_failed)
                    )
                }
            }
        }
    }

    override fun startClass(
        professorId: String,
        studentId: String,
        duration: Int,
        state: MutableStateFlow<StudentDetailState>,
        scope: CoroutineScope
    ) {
        scope.launch {
            if (duration <= 0) {
                state.value = state.value.copy(errorMessage = application.getString(R.string.student_detail_error_unexpected))
                return@launch
            }

            val currentStudent = state.value.student ?: return@launch
            // Use DB-sourced pricePerHour from the student entity
            val verifiedPrice = currentStudent.pricePerHour
            val amountToAdd = (duration.toDouble() / 60.0) * verifiedPrice
            state.value = state.value.copy(isLoading = true)

            // Use atomic addToBalance instead of full entity overwrite.
            // This prevents race conditions where a concurrent registerPayment
            // updated the DB balance but the in-memory student still has the old value.
            when (addToBalanceUseCase(professorId, studentId, amountToAdd)) {
                is Result.Success -> {
                    // Optimistic UI update
                    val updatedStudent = currentStudent.copy(
                        pendingBalance = currentStudent.pendingBalance + amountToAdd,
                        lastClassDate = System.currentTimeMillis()
                    )
                    state.value = state.value.copy(
                        student = updatedStudent,
                        isLoading = false,
                        showStartClassDialog = false,
                        successMessage = application.getString(R.string.student_detail_success_class_started, amountToAdd)
                    )
                }
                is Result.Error -> {
                    state.value = state.value.copy(
                        isLoading = false,
                        errorMessage = application.getString(R.string.student_detail_error_update_balance_failed)
                    )
                }
            }
        }
    }

    private fun handlePaymentError(error: DomainError, state: MutableStateFlow<StudentDetailState>) {
        val message = when (error) {
            DomainError.InvalidAmount -> application.getString(R.string.payment_error_invalid_amount)
            else -> application.getString(R.string.student_detail_error_payment_failed)
        }
        state.value = state.value.copy(isLoading = false, errorMessage = message)
    }
}
