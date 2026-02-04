package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.Schedule
import kotlinx.coroutines.flow.Flow

interface IGetAllSchedulesUseCase {
    operator fun invoke(professorId: String): Flow<List<Schedule>>
}
