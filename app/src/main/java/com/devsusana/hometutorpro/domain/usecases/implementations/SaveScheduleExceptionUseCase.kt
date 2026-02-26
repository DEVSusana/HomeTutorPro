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

/**
 * Use case implementation for savescheduleexception operations.
 */
class SaveScheduleExceptionUseCase @Inject constructor(
    private val repository: ScheduleExceptionRepository,
    private val studentRepository: StudentRepository
) : ISaveScheduleExceptionUseCase {
    override suspend operator fun invoke(
        professorId: String,
        studentId: String,
        exception: ScheduleException
    ): Result<Unit, DomainError> {
        val exceptionDate = Instant.ofEpochMilli(exception.date)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        // PREVENT DUPLICATES: Check if an exception already exists for this original schedule and date
        var exceptionToSave = exception
        if (exception.id.isEmpty()) {
            val existingExceptions = repository.getExceptions(professorId, studentId).first()
            val alreadyExists = existingExceptions.find { 
                it.originalScheduleId == exception.originalScheduleId && 
                Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate() == exceptionDate
            }
            
            if (alreadyExists != null) {
                // Reuse the existing ID to perform an update instead of an insert
                exceptionToSave = exception.copy(id = alreadyExists.id)
            }
        }

        // Check for conflicts if it's a RESCHEDULED exception or an EXTRA class
        if ((exceptionToSave.type == ExceptionType.RESCHEDULED || exceptionToSave.type == ExceptionType.EXTRA) 
            && exceptionToSave.newStartTime.isNotEmpty() && exceptionToSave.newEndTime.isNotEmpty()) {
            // 1. Get all students
            val students = studentRepository.getStudents(professorId).first()
            
            // Parse the exception date to get the day of week for comparison
            val exceptionDayOfWeek = exceptionToSave.newDayOfWeek ?: exceptionDate.dayOfWeek

            // 2. Check against all students' schedules and exceptions
            for (student in students) {
                // Check regular schedules
                val studentSchedules = studentRepository.getSchedules(professorId, student.id).first()
                val studentExceptions = repository.getExceptions(professorId, student.id).first()

                for (existingSchedule in studentSchedules) {
                    if (existingSchedule.dayOfWeek == exceptionDayOfWeek) {
                         // Check time overlap
                        if (isTimeOverlap(exceptionToSave.newStartTime, exceptionToSave.newEndTime, existingSchedule.startTime, existingSchedule.endTime)) {
                            // Check if THIS regular schedule is cancelled or rescheduled (moved away) for THIS date
                            val isFreeSlot = studentExceptions.any { 
                                it.originalScheduleId == existingSchedule.id && 
                                (it.type == ExceptionType.CANCELLED || it.type == ExceptionType.RESCHEDULED) &&
                                Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate() == exceptionDate
                            }
                            
                            if (!isFreeSlot) {
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

                // 3. Check against other exceptions (Rescheduled or Extra classes) for THIS date
                for (existingException in studentExceptions) {
                    // Skip if it's the same exception we are saving
                    if (existingException.id == exceptionToSave.id) continue

                    val existingExcDate = Instant.ofEpochMilli(existingException.date)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()

                    if (existingExcDate == exceptionDate && 
                        (existingException.type == ExceptionType.RESCHEDULED || existingException.type == ExceptionType.EXTRA)) {
                        
                        if (isTimeOverlap(exceptionToSave.newStartTime, exceptionToSave.newEndTime, existingException.newStartTime, existingException.newEndTime)) {
                            return Result.Error(
                                DomainError.ConflictingStudent(
                                    studentName = student.name,
                                    time = "${existingException.newStartTime} - ${existingException.newEndTime}"
                                )
                            )
                        }
                    }
                }
            }
        }

        return repository.saveException(professorId, studentId, exceptionToSave)
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
