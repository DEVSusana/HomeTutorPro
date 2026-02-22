package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.ScheduleException
import com.devsusana.hometutorpro.domain.repository.ScheduleExceptionRepository
import javax.inject.Inject

import com.devsusana.hometutorpro.domain.entities.ExceptionType
import com.devsusana.hometutorpro.domain.repository.StudentRepository
import com.devsusana.hometutorpro.domain.usecases.ISaveScheduleExceptionUseCase
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId

class SaveScheduleExceptionUseCase @Inject constructor(
    private val repository: ScheduleExceptionRepository,
    private val studentRepository: StudentRepository
) : ISaveScheduleExceptionUseCase {
    override suspend operator fun invoke(
        professorId: String,
        studentId: String,
        exception: ScheduleException
    ): Result<Unit, DomainError> {
        // Check for conflicts if it's a RESCHEDULED exception or an EXTRA class
        if ((exception.type == ExceptionType.RESCHEDULED || exception.type == ExceptionType.EXTRA) 
            && exception.newStartTime.isNotEmpty() && exception.newEndTime.isNotEmpty()) {
            // 1. Get all students
            val students = studentRepository.getStudents(professorId).first()
            
            // Parse the exception date to get the day of week
            val exceptionDate = Instant.ofEpochMilli(exception.date)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            val exceptionDayOfWeek = exception.newDayOfWeek ?: exceptionDate.dayOfWeek

            // 2. Check against all regular schedules for that day
            for (student in students) {
                val studentSchedules = studentRepository.getSchedules(professorId, student.id).first()
                for (existingSchedule in studentSchedules) {
                    if (existingSchedule.dayOfWeek == exceptionDayOfWeek) {
                         // Check time overlap
                        if (isTimeOverlap(exception.newStartTime, exception.newEndTime, existingSchedule.startTime, existingSchedule.endTime)) {
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
            
            // TODO: Ideally we should also check against OTHER exceptions (e.g. another rescheduled class to this time), 
            // but for MVP checking against regular schedule is the most critical part.
        }

        return repository.saveException(professorId, studentId, exception)
    }

    private fun isTimeOverlap(start1: String, end1: String, start2: String, end2: String): Boolean {
        return try {
            val s1 = timeToMinutes(start1)
            val e1 = timeToMinutes(end1)
            val s2 = timeToMinutes(start2)
            val e2 = timeToMinutes(end2)
            
            // Check if ranges overlap (Strict overlap: one starts before the other ends)
            s1 < e2 && s2 < e1
        } catch (e: Exception) {
            false
        }
    }
    
    private fun timeToMinutes(time: String): Int {
        val parts = time.split(":")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }
}
