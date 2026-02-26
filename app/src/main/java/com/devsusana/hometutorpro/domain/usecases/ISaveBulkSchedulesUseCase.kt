package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.BulkScheduleResult
import com.devsusana.hometutorpro.domain.entities.Schedule
/**
 * Use case contract for SaveBulkSchedules operations.
 */
interface ISaveBulkSchedulesUseCase {
    /**
     * Executes the use case.
     */
    /**
     * Executes the use case.
     */
    suspend operator fun invoke(
        professorId: String,
        studentId: String,
        schedules: List<Schedule>,
        pendingSchedules: List<Schedule> = emptyList()
    ): BulkScheduleResult
}
