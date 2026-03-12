package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.repository.StudentRepository
import com.devsusana.hometutorpro.domain.usecases.IToggleStudentActiveUseCase
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Use case implementation that toggles the active/inactive status of a student.
 * Fetches the student, flips [isActive], and persists the change.
 */
class ToggleStudentActiveUseCase @Inject constructor(
    private val studentRepository: StudentRepository
) : IToggleStudentActiveUseCase {

    override suspend operator fun invoke(
        professorId: String,
        studentId: String
    ): Result<Unit, DomainError> {
        val student = studentRepository.getStudentById(professorId, studentId).first()
            ?: return Result.Error(DomainError.StudentNotFound)

        val updatedStudent = student.copy(isActive = !student.isActive)
        return when (studentRepository.saveStudent(professorId, updatedStudent)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> Result.Error(DomainError.Unknown)
        }
    }
}
