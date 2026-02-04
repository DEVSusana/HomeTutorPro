package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.ScheduleException
import kotlinx.coroutines.flow.Flow

interface IGetScheduleExceptionsUseCase {
    operator fun invoke(professorId: String, studentId: String): Flow<List<ScheduleException>>
}
