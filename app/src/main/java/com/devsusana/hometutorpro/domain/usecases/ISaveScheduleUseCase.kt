package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.Schedule

interface ISaveScheduleUseCase {
    suspend operator fun invoke(professorId: String, studentId: String, schedule: Schedule): Result<Unit, DomainError>
}
