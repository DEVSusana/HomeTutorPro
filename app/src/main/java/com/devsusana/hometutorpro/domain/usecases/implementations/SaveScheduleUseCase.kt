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
        // 1. Get all students to check for conflicts
        val students = repository.getStudents(professorId).first()
        
        // 2. Check for conflicts with any student's schedule
        for (student in students) {
            // Ignore inactive students for conflict detection (e.g. students on vacation)
            if (!student.isActive) continue

            // Skip checking against the same schedule if we are updating it (though schedule IDs might be different if it's a new one)
            // But we need to check against OTHER schedules of the SAME student too, unless it's the exact same schedule ID being updated.
            
            val studentSchedules = repository.getSchedules(professorId, student.id).first()
            for (existingSchedule in studentSchedules) {
                // Skip if it's the exact same schedule we are updating
                if (existingSchedule.id == schedule.id && student.id == studentId) continue

                if (existingSchedule.dayOfWeek == schedule.dayOfWeek) {
                    // Check time overlap
                    if (isTimeOverlap(schedule.startTime, schedule.endTime, existingSchedule.startTime, existingSchedule.endTime)) {
                        return Result.Error(
                            DomainError.ConflictingStudent(
                                studentName = student.name,
                                time = "${existingSchedule.startTime} - ${existingSchedule.endTime}"
                            )
                        )
                    }
                }
            }
        }

        return repository.saveSchedule(professorId, studentId, schedule)
    }

    private fun isTimeOverlap(start1: String, end1: String, start2: String, end2: String): Boolean {
        // Simple string comparison works for "HH:mm" format if they are 24h and padded (e.g. "09:00")
        // Assuming format is always "HH:mm"
        return start1 < end2 && start2 < end1
    }
}
