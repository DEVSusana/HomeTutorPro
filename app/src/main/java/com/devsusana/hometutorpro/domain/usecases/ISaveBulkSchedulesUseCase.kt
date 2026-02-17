package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.BulkScheduleResult
import com.devsusana.hometutorpro.domain.entities.Schedule
interface ISaveBulkSchedulesUseCase {
    suspend operator fun invoke(
        professorId: String,
        studentId: String,
        schedules: List<Schedule>,
        pendingSchedules: List<Schedule> = emptyList()
    ): BulkScheduleResult
}
