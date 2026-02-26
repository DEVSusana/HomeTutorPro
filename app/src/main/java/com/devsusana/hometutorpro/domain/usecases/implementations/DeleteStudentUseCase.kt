package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.repository.StudentRepository
import com.devsusana.hometutorpro.domain.usecases.IDeleteStudentUseCase

import javax.inject.Inject

/**
 * Default implementation of [IDeleteStudentUseCase].
 */
class DeleteStudentUseCase @Inject constructor(private val repository: StudentRepository) :
    IDeleteStudentUseCase {
    override suspend operator fun invoke(professorId: String, studentId: String): Result<Unit, DomainError> {
        return repository.deleteStudent(professorId, studentId)
    }
}
