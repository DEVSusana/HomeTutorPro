package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.CalendarOccurrence

interface IGetNextClassUseCase {
    suspend operator fun invoke(): CalendarOccurrence?
}
