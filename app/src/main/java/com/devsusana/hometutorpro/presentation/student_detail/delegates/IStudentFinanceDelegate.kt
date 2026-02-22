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

    fun startClass(
        professorId: String,
        studentId: String,
        duration: Int,
        pricePerHour: Double,
        state: MutableStateFlow<StudentDetailState>,
        scope: CoroutineScope
    )
}
