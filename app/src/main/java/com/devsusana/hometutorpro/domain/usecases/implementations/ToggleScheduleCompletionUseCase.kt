package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.repository.StudentRepository
import com.devsusana.hometutorpro.domain.usecases.IToggleScheduleCompletionUseCase
import javax.inject.Inject

/**
 * Use case for toggling the completion status of a scheduled class.
 * Marks a class as completed/pending and triggers sync.
 */
/**
 * Use case implementation for toggleschedulecompletion operations.
 */
class ToggleScheduleCompletionUseCase @Inject constructor(
    private val studentRepository: StudentRepository
) : IToggleScheduleCompletionUseCase {
    override suspend operator fun invoke(professorId: String, scheduleId: String): Result<Unit, DomainError> {
        return studentRepository.toggleScheduleCompletion(professorId, scheduleId)
    }
}
