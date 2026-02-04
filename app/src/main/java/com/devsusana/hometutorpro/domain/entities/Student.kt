package com.devsusana.hometutorpro.domain.entities

/**
 * Minimized student model, excluding sensitive data ("learning problems")
 * for GDPR compliance in the MVP.
 */
data class Student(
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
    val pendingBalance: Double = 0.0, // Financial Module
    val isActive: Boolean = true,
    val lastClassDate: Long? = null,
    val educationalAttention: String = "", // Renamed from specialNeeds
    val lastPaymentDate: Long? = null, // MVP Requirement: Last Payment Date
    val notes: String = "", // Professor notes about the student
    val color: Int? = null // Customizable Student Color
)
