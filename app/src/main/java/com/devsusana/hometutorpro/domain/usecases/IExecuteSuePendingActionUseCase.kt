package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.SueOperationResult
import com.devsusana.hometutorpro.domain.entities.SuePendingAction

/**
 * Use case to execute a pending action confirmed by the user in SUE.
 */
interface IExecuteSuePendingActionUseCase {
    /**
     * Executes the given confirmed [action].
     */
    suspend operator fun invoke(action: SuePendingAction): SueOperationResult.Execute

    /**
     * Retrieves available free slots for a conflict resolution scenario.
     */
    suspend fun getConflictFreeSlots(dayOfWeek: Int?): SueOperationResult
}
