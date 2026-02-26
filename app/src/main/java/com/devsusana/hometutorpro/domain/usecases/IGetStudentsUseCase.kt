package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.StudentSummary
import kotlinx.coroutines.flow.Flow

/**
 * Use case contract for GetStudents operations.
 */
interface IGetStudentsUseCase {
    /**
     * Executes the use case.
     */
    operator fun invoke(professorId: String): Flow<List<StudentSummary>>
}