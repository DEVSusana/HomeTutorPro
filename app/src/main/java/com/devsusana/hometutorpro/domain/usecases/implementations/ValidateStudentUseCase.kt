package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.Student
import com.devsusana.hometutorpro.domain.usecases.IValidateStudentUseCase
import javax.inject.Inject

class ValidateStudentUseCase @Inject constructor() : IValidateStudentUseCase {
    override operator fun invoke(student: Student): Result<Unit, DomainError> {
        if (student.name.isBlank()) {
            return Result.Error(DomainError.StudentNameRequired)
        }
        if (student.pricePerHour <= 0) {
            return Result.Error(DomainError.InvalidPrice)
        }
        if (student.pendingBalance < -10000 || student.pendingBalance > 100000) {
            return Result.Error(DomainError.InvalidBalance)
        }
        return Result.Success(Unit)
    }
}
