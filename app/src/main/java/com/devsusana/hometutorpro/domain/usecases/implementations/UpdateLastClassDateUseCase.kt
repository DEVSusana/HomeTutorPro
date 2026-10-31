package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.repository.StudentRepository
import com.devsusana.hometutorpro.domain.usecases.IUpdateLastClassDateUseCase
import javax.inject.Inject

/**
 * Implementation of [IUpdateLastClassDateUseCase] that updates the student's lastClassDate
 * atomically without modifying pendingBalance or other student fields.
 */
class UpdateLastClassDateUseCase @Inject constructor(
    private val studentRepository: StudentRepository
) : IUpdateLastClassDateUseCase {
    override suspend operator fun invoke(
        professorId: String,
        studentId: String,
        classDate: Long
    ): Result<Unit, DomainError> {
        return studentRepository.updateLastClassDate(professorId, studentId, classDate)
    }
}
