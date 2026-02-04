package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.repository.StudentRepository

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.usecases.IDeleteScheduleUseCase
import javax.inject.Inject

class DeleteScheduleUseCase @Inject constructor(private val repository: StudentRepository) :
    IDeleteScheduleUseCase {
    override suspend operator fun invoke(professorId: String, studentId: String, scheduleId: String): Result<Unit, DomainError> = repository.deleteSchedule(professorId, studentId, scheduleId)
}
