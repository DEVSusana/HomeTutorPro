package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.repository.StudentRepository

import com.devsusana.hometutorpro.domain.entities.Student
import com.devsusana.hometutorpro.domain.usecases.IGetStudentByIdUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetStudentByIdUseCase @Inject constructor(private val repository: StudentRepository) :
    IGetStudentByIdUseCase {
    override operator fun invoke(professorId: String, studentId: String): Flow<Student?> = repository.getStudentById(professorId, studentId)
}
