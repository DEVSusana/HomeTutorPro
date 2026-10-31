package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result

/**
 * Interface defining contract to update only the last class date of a student.
 */
interface IUpdateLastClassDateUseCase {
    suspend operator fun invoke(
        professorId: String,
        studentId: String,
        classDate: Long
    ): Result<Unit, DomainError>
}
