package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.Schedule

interface ICheckScheduleConflictUseCase {
    operator fun invoke(newSchedule: Schedule, existingSchedules: List<Schedule>): Boolean
}
