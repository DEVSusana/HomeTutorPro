package com.devsusana.hometutorpro.domain.repository

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.ScheduleException
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for ScheduleException data operations.
 */
interface ScheduleExceptionRepository {
    fun getExceptions(professorId: String, studentId: String): Flow<List<ScheduleException>>
    suspend fun saveException(professorId: String, studentId: String, exception: ScheduleException): Result<Unit, DomainError>
    suspend fun deleteException(professorId: String, studentId: String, exceptionId: String): Result<Unit, DomainError>
    suspend fun cleanupDuplicates(): Result<Unit, DomainError>
}
