package com.devsusana.hometutorpro.presentation.student_detail.delegates

import com.devsusana.hometutorpro.domain.entities.PaymentType
import com.devsusana.hometutorpro.presentation.student_detail.StudentDetailState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow

interface IStudentFinanceDelegate {
    fun registerPayment(
        professorId: String,
        studentId: String,
        amount: Double,
        paymentType: PaymentType,
        state: MutableStateFlow<StudentDetailState>,
        scope: CoroutineScope
    )

    fun updateBalance(
        professorId: String,
        studentId: String,
        newBalance: Double,
        state: MutableStateFlow<StudentDetailState>,
        scope: CoroutineScope
    )

    /**
     * Starts a class for the student, atomically adding the computed cost to the pending balance.
     *
     * @param professorId the professor's ID
     * @param studentId the student's ID
     * @param duration the duration of the class in minutes
     * @param state the mutable state flow for the student detail screen
     * @param scope the coroutine scope to launch the operation in
     */
    fun startClass(
        professorId: String,
        studentId: String,
        duration: Int,
        state: MutableStateFlow<StudentDetailState>,
        scope: CoroutineScope
    )
}
