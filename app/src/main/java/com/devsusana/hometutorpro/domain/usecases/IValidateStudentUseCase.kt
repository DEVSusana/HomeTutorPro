package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.Student

/**
 * Use case contract for ValidateStudent operations.
 */
interface IValidateStudentUseCase {
    /**
     * Executes the use case.
     */
    operator fun invoke(student: Student): Result<Unit, DomainError>
}
