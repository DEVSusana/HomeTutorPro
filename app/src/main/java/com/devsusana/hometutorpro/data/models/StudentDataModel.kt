package com.devsusana.hometutorpro.data.models

/**
 * Data model for Firestore Student document.
 * Used in the data layer for Firestore operations.
 */
data class StudentDataModel(
    val id: String = "",
    val professorId: String = "",
    val name: String = "",
    val age: Int = 0,
    val address: String = "",
    val parentPhones: String = "",
    val studentPhone: String = "",
    val studentEmail: String? = null,
    val subjects: String = "",
    val course: String = "",
    val pricePerHour: Double = 0.0,
    val pendingBalance: Double = 0.0,
    val educationalAttention: String = "",
    val lastPaymentDate: Long? = null,
    val color: Int? = null,
    val isActive: Boolean = true,
    val lastClassDate: Long? = null
)
