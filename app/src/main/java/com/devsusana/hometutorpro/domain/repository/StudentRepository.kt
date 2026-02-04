package com.devsusana.hometutorpro.domain.repository

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.PaymentType
import com.devsusana.hometutorpro.domain.entities.Schedule
import com.devsusana.hometutorpro.domain.entities.Student
import kotlinx.coroutines.flow.Flow

/**
 * Firestore Repository Interface
 * Handles CRUD (Create, Read, Update) logic and nested data structures.
 */
interface StudentRepository {
    fun getStudents(professorId: String): Flow<List<Student>>
    fun getStudentById(professorId: String, studentId: String): Flow<Student?>
    suspend fun saveStudent(professorId: String, student: Student): Result<String, DomainError>
    suspend fun registerPayment(
        professorId: String,
        studentId: String,
        amountPaid: Double,
        paymentType: PaymentType
    ): Result<Unit, DomainError>

    fun getSchedules(professorId: String, studentId: String): Flow<List<Schedule>>
    fun getAllSchedules(professorId: String): Flow<List<Schedule>>
    suspend fun saveSchedule(professorId: String, studentId: String, schedule: Schedule): Result<Unit, DomainError>
    suspend fun deleteSchedule(professorId: String, studentId: String, scheduleId: String): Result<Unit, DomainError>
    suspend fun toggleScheduleCompletion(professorId: String, scheduleId: String): Result<Unit, DomainError>
    suspend fun deleteStudent(professorId: String, studentId: String): Result<Unit, DomainError>
}
