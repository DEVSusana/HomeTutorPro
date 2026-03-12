package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.Student
import com.devsusana.hometutorpro.domain.repository.StudentRepository
import com.devsusana.hometutorpro.domain.usecases.ISaveStudentUseCase
import com.devsusana.hometutorpro.domain.utils.StudentColorUtil
import kotlinx.coroutines.flow.first

import javax.inject.Inject

/**
 * Default implementation of [ISaveStudentUseCase].
 */
class SaveStudentUseCase @Inject constructor(private val repository: StudentRepository) :
    ISaveStudentUseCase {
    override suspend operator fun invoke(professorId: String, student: Student): Result<String, DomainError> {
        var studentToSave = student
        
        // Auto-generate color if missing for new students (ID is empty or purely numeric placeholder)
        if (studentToSave.color == null && (studentToSave.id.isEmpty() || studentToSave.id == "0")) {
            val existingStudents = repository.getStudents(professorId).first()
            val leastUsedColor = StudentColorUtil.getLeastUsedColor(existingStudents)
            studentToSave = studentToSave.copy(color = leastUsedColor)
        }
        
        return repository.saveStudent(professorId, studentToSave)
    }
}
