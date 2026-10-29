package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.ActiveSession

/**
 * Contract for consolidating an active tutoring session into student accounting once it
 * reaches the minimum billable duration threshold defined in Section 6.B of BUSINESS_RULES.md.
 */
interface IConsolidateActiveSessionUseCase {
    /**
     * Consolidates [session] (or the current active session if null).
     *
     * @param session Optional explicit active session to consolidate.
     * @return [Result.Success] with true if billed, false if skipped, or [Result.Error] on failure.
     */
    suspend operator fun invoke(session: ActiveSession? = null): Result<Boolean, DomainError>
}
