package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.Student

interface ISaveStudentUseCase {
    suspend operator fun invoke(professorId: String, student: Student): Result<String, DomainError>
}
