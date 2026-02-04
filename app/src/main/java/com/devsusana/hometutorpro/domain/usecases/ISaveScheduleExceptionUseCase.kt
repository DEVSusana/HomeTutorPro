package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.ScheduleException

interface ISaveScheduleExceptionUseCase {
    suspend operator fun invoke(
        professorId: String,
        studentId: String,
        exception: ScheduleException
    ): Result<Unit, DomainError>
}
