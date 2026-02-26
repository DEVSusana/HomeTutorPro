package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.repository.StudentRepository

import com.devsusana.hometutorpro.domain.entities.StudentSummary
import com.devsusana.hometutorpro.domain.usecases.IGetStudentsUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Default implementation of [IGetStudentsUseCase].
 */
class GetStudentsUseCase @Inject constructor(private val repository: StudentRepository) :
    IGetStudentsUseCase {
    override operator fun invoke(professorId: String): Flow<List<StudentSummary>> = repository.getStudents(professorId)
}
