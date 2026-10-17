package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.CalendarOccurrence

/**
 * Use case to determine the next scheduled tutoring class.
 */
interface IGetNextClassUseCase {
    suspend operator fun invoke(): CalendarOccurrence?
}
