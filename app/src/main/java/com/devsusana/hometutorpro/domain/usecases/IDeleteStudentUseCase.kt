package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result

interface IDeleteStudentUseCase {
    suspend operator fun invoke(professorId: String, studentId: String): Result<Unit, DomainError>
}
