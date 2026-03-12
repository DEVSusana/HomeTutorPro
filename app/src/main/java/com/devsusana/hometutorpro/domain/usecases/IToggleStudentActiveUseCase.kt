package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result

/**
 * Use case contract for toggling the active/inactive status of a student.
 */
interface IToggleStudentActiveUseCase {
    /**
     * Toggles the active status of the student identified by [studentId].
     * @return [Result.Success] with [Unit] on success, or [Result.Error] with a [DomainError].
     */
    suspend operator fun invoke(professorId: String, studentId: String): Result<Unit, DomainError>
}
