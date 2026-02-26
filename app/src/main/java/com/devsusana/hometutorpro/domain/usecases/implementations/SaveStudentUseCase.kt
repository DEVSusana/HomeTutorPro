package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.Student
import com.devsusana.hometutorpro.domain.repository.StudentRepository
import com.devsusana.hometutorpro.domain.usecases.ISaveStudentUseCase

import javax.inject.Inject

/**
 * Default implementation of [ISaveStudentUseCase].
 */
class SaveStudentUseCase @Inject constructor(private val repository: StudentRepository) :
    ISaveStudentUseCase {
    override suspend operator fun invoke(professorId: String, student: Student): Result<String, DomainError> = repository.saveStudent(professorId, student)
}
