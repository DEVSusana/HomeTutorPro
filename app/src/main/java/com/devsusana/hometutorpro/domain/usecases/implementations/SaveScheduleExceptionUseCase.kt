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
        if (exception.id.isEmpty() && exception.originalScheduleId.isNotEmpty() && exception.originalScheduleId != "EXTRA" && exception.type != ExceptionType.EXTRA) {
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
            // Batch fetch students, schedules, and exceptions upfront to eliminate N+1 queries
            val students = studentRepository.getStudents(professorId).first()
            val allSchedules = studentRepository.getAllSchedules(professorId).first()
            val allExceptions = repository.getAllExceptions(professorId)
            val studentsMap = students.associateBy { it.id }
            
            // Parse the exception date to get the day of week for comparison
            val exceptionDayOfWeek = exceptionToSave.newDayOfWeek ?: exceptionDate.dayOfWeek
            
            // Calculate targetDate based on the new day of week
            val daysDiff = exceptionDayOfWeek.value - exceptionDate.dayOfWeek.value
            val targetDate = exceptionDate.plusDays(daysDiff.toLong())

            // 1. Check against regular schedules
            for (existingSchedule in allSchedules) {
                // Skip B's own schedule that is being rescheduled (it's vacating that slot)
                if (existingSchedule.id == exceptionToSave.originalScheduleId) continue

                if (existingSchedule.dayOfWeek == exceptionDayOfWeek) {
                    // Check time overlap
                    if (isTimeOverlap(exceptionToSave.newStartTime, exceptionToSave.newEndTime, existingSchedule.startTime, existingSchedule.endTime)) {
                        // Check if THIS regular schedule is cancelled or rescheduled (moved away) for THIS target date
                        val isFreeSlot = allExceptions.any { exc ->
                            exc.originalScheduleId == existingSchedule.id &&
                            (exc.type == ExceptionType.CANCELLED || exc.type == ExceptionType.RESCHEDULED) &&
                            Instant.ofEpochMilli(exc.date).atZone(ZoneId.systemDefault()).toLocalDate() == targetDate
                        }

                        if (!isFreeSlot) {
                            val studentName = studentsMap[existingSchedule.studentId]?.name ?: "Alumno"
                            return Result.Error(
                                DomainError.ConflictingStudent(
                                    studentName = studentName,
                                    time = "${existingSchedule.startTime} - ${existingSchedule.endTime}"
                                )
                            )
                        }
                    }
                }
            }

            // 2. Check against other exceptions (Rescheduled or Extra classes) for THIS date
            for (existingException in allExceptions) {
                // Skip if it's the same exception we are saving
                if (existingException.id == exceptionToSave.id) continue

                val existingExcDate = Instant.ofEpochMilli(existingException.date)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    
                val existingExcTargetDate = if (existingException.newDayOfWeek != null) {
                    val diff = existingException.newDayOfWeek.value - existingExcDate.dayOfWeek.value
                    existingExcDate.plusDays(diff.toLong())
                } else {
                    existingExcDate
                }

                if (existingExcTargetDate == targetDate && 
                    (existingException.type == ExceptionType.RESCHEDULED || existingException.type == ExceptionType.EXTRA)) {
                    
                    if (isTimeOverlap(exceptionToSave.newStartTime, exceptionToSave.newEndTime, existingException.newStartTime, existingException.newEndTime)) {
                        val studentName = studentsMap[existingException.studentId]?.name ?: "Alumno"
                        return Result.Error(
                            DomainError.ConflictingStudent(
                                studentName = studentName,
                                time = "${existingException.newStartTime} - ${existingException.newEndTime}"
                            )
                        )
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
