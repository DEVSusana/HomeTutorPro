package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.Schedule
import kotlinx.coroutines.flow.Flow

interface IGetSchedulesUseCase {
    operator fun invoke(professorId: String, studentId: String): Flow<List<Schedule>>
}
