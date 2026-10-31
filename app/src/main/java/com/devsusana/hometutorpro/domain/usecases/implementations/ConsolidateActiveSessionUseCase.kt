package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.constants.SessionConstants
import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.ActiveSession
import com.devsusana.hometutorpro.domain.usecases.IAddToBalanceUseCase
import com.devsusana.hometutorpro.domain.usecases.IConsolidateActiveSessionUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetActiveSessionUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetCurrentUserUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetStudentByIdUseCase
import com.devsusana.hometutorpro.domain.usecases.ISaveStudentUseCase
import com.devsusana.hometutorpro.domain.usecases.IStartActiveSessionUseCase
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

/**
 * Implementation of [IConsolidateActiveSessionUseCase].
 *
 * Enforces the 15-minute billing threshold defined in Section 6.B of BUSINESS_RULES.md.
 * Sessions stopped before 15 minutes are treated as accidental/cancelled and skip billing.
 */
class ConsolidateActiveSessionUseCase @Inject constructor(
    private val getActiveSessionUseCase: IGetActiveSessionUseCase,
    private val getCurrentUserUseCase: IGetCurrentUserUseCase,
    private val getStudentByIdUseCase: IGetStudentByIdUseCase,
    private val addToBalanceUseCase: IAddToBalanceUseCase,
    private val saveStudentUseCase: ISaveStudentUseCase,
    private val startActiveSessionUseCase: IStartActiveSessionUseCase
) : IConsolidateActiveSessionUseCase {

    override suspend operator fun invoke(session: ActiveSession?): Result<Boolean, DomainError> {
        val targetSession = session ?: getActiveSessionUseCase()
            ?: return Result.Success(false)

        if (!targetSession.isOngoing || targetSession.isBilled) {
            return Result.Success(false)
        }

        val elapsedMillis = System.currentTimeMillis() - targetSession.startTimeMillis
        if (elapsedMillis < SessionConstants.MIN_BILLABLE_DURATION_MILLIS) {
            return Result.Success(false)
        }

        val tutorId = targetSession.tutorId.takeIf { it.isNotBlank() }
            ?: getCurrentUserUseCase().value?.uid
            ?: return Result.Error(DomainError.UserNotFound)

        val student = getStudentByIdUseCase(tutorId, targetSession.studentId).firstOrNull()
            ?: return Result.Error(DomainError.StudentNotFound)

        val priceToAdd = (targetSession.durationMinutes / 60.0) * student.pricePerHour
        val consolidationResult = if (priceToAdd > 0) {
            when (val res = addToBalanceUseCase(tutorId, student.id, priceToAdd)) {
                is Result.Success -> Result.Success(true)
                is Result.Error -> Result.Error(res.error)
            }
        } else {
            // For zero-price sessions, fetch fresh student to avoid overwriting recent balance mutations
            val freshStudent = getStudentByIdUseCase(tutorId, student.id).firstOrNull() ?: student
            when (val res = saveStudentUseCase(tutorId, freshStudent.copy(lastClassDate = System.currentTimeMillis()))) {
                is Result.Success -> Result.Success(true)
                is Result.Error -> Result.Error(res.error)
            }
        }

        // Persist isBilled = true immediately upon successful billing to prevent double charging
        if (consolidationResult is Result.Success && consolidationResult.data) {
            startActiveSessionUseCase(targetSession.copy(isBilled = true))
        }

        return consolidationResult
    }
}
