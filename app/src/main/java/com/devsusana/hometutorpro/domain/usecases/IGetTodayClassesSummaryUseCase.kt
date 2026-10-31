package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.TodayClassesSummary

/**
 * Use case to retrieve the summary and progress of classes scheduled for today.
 */
interface IGetTodayClassesSummaryUseCase {
    /**
     * Computes and returns the [TodayClassesSummary] for the authenticated tutor.
     *
     * @return [TodayClassesSummary] containing total and completed classes for today.
     */
    suspend operator fun invoke(): TodayClassesSummary
}
