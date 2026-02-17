package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.entities.Schedule
import com.devsusana.hometutorpro.domain.repository.StudentRepository

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.usecases.ISaveScheduleUseCase
import kotlinx.coroutines.flow.first

import javax.inject.Inject

class SaveScheduleUseCase @Inject constructor(private val repository: StudentRepository) :
    ISaveScheduleUseCase {
    override suspend operator fun invoke(professorId: String, studentId: String, schedule: Schedule): Result<Unit, DomainError> {
        // SQL-optimized conflict detection
        val conflictingSchedule = repository.getConflictingSchedule(
            dayOfWeek = schedule.dayOfWeek.value,
            startTime = schedule.startTime,
            endTime = schedule.endTime,
            scheduleId = schedule.id.takeIf { it.isNotEmpty() }
        )

        conflictingSchedule?.let { conflict ->
            return Result.Error(
                DomainError.ConflictingStudent(
                    studentName = conflict.studentName ?: "Unknown",
                    time = "${conflict.startTime} - ${conflict.endTime}"
                )
            )
        }

        return repository.saveSchedule(professorId, studentId, schedule)
    }
}
