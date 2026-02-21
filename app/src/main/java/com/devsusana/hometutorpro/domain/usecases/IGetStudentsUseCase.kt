package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.StudentSummary
import kotlinx.coroutines.flow.Flow

interface IGetStudentsUseCase {
    operator fun invoke(professorId: String): Flow<List<StudentSummary>>
}