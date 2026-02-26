package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.Student
import kotlinx.coroutines.flow.Flow

/**
 * Use case contract for GetStudentById operations.
 */
interface IGetStudentByIdUseCase {
    /**
     * Executes the use case.
     */
    operator fun invoke(professorId: String, studentId: String): Flow<Student?>
}
