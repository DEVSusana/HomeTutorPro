package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.Student
import kotlinx.coroutines.flow.Flow

interface IGetStudentByIdUseCase {
    operator fun invoke(professorId: String, studentId: String): Flow<Student?>
}
