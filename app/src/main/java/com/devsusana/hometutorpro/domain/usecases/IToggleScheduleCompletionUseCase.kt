package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result

interface IToggleScheduleCompletionUseCase {
    suspend operator fun invoke(professorId: String, scheduleId: String): Result<Unit, DomainError>
}
