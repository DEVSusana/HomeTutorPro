package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.Schedule

/**
 * Use case contract for CheckScheduleConflict operations.
 */
interface ICheckScheduleConflictUseCase {
    /**
     * Executes the use case.
     */
    operator fun invoke(newSchedule: Schedule, existingSchedules: List<Schedule>): Boolean
}
