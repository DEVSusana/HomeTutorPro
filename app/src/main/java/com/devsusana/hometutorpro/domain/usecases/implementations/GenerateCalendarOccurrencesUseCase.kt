package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.entities.CalendarOccurrence
import com.devsusana.hometutorpro.domain.entities.ExceptionType
import com.devsusana.hometutorpro.domain.entities.Schedule
import com.devsusana.hometutorpro.domain.entities.ScheduleException
import com.devsusana.hometutorpro.domain.entities.ScheduleType
import com.devsusana.hometutorpro.domain.entities.StudentSummary
import com.devsusana.hometutorpro.domain.usecases.IGenerateCalendarOccurrencesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * Default implementation of [IGenerateCalendarOccurrencesUseCase].
 */
class GenerateCalendarOccurrencesUseCase @Inject constructor() : IGenerateCalendarOccurrencesUseCase {
    override suspend fun invoke(
        students: List<StudentSummary>,
        schedules: List<Schedule>,
        exceptions: List<ScheduleException>,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<CalendarOccurrence> = withContext(Dispatchers.Default) {
        val allOccurrences = mutableListOf<CalendarOccurrence>()
        
        // Map exceptions for faster lookup: "studentId_originalScheduleId_dateTimestamp"
        val exceptionsMap = exceptions.groupBy { 
            "${it.studentId}_${it.originalScheduleId}_${it.date}"
        }

        val consumedExceptionIds = mutableSetOf<String>()

        // 1. Generate regular occurrences
        schedules.forEach { schedule ->
            // Use details already present in schedule (JOIN optimization)
            val studentName = schedule.studentName
            if (studentName != null && schedule.studentIsActive == true) {
                val student = StudentSummary(
                    id = schedule.studentId,
                    name = studentName,
                    subjects = schedule.studentSubjects ?: "",
                    color = schedule.studentColor,
                    pendingBalance = schedule.studentPendingBalance ?: 0.0,
                    pricePerHour = schedule.studentPricePerHour ?: 0.0,
                    isActive = true,
                    lastClassDate = null
                )
                
                var currentDate = startDate
                while (!currentDate.isAfter(endDate)) {
                    if (currentDate.dayOfWeek == schedule.dayOfWeek) {
                        val dateTimestamp = currentDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        val key = "${student.id}_${schedule.id}_$dateTimestamp"
                        val exception = exceptionsMap[key]?.firstOrNull()
                        
                        if (exception != null && exception.id.isNotEmpty()) {
                            consumedExceptionIds.add(exception.id)
                        }
                        
                        allOccurrences.add(CalendarOccurrence(schedule, student, exception, currentDate))
                    }
                    currentDate = currentDate.plusDays(1)
                }
            }
        }

        // 2. Add extra classes and rescheduled classes (standalone exceptions)
        // Standalone exceptions are EXTRA classes or RESCHEDULED classes that 
        // weren't already attached to an original schedule slot on the same day.
        exceptions.filter { 
            it.type == ExceptionType.EXTRA || 
            (it.type == ExceptionType.RESCHEDULED && !consumedExceptionIds.contains(it.id))
        }.forEach { exception ->
            val student = students.find { it.id == exception.studentId }
            if (student != null && student.isActive) {
                val excDate = Instant.ofEpochMilli(exception.date).atZone(ZoneId.systemDefault()).toLocalDate()
                
                if (!excDate.isBefore(startDate) && !excDate.isAfter(endDate)) {
                    val dummySchedule = Schedule(
                        id = if (exception.type == ExceptionType.EXTRA) "${ScheduleType.EXTRA_ID}_${exception.id}" else "RESCHEDULED_${exception.id}",
                        studentId = student.id,
                        dayOfWeek = exception.newDayOfWeek ?: excDate.dayOfWeek,
                        startTime = exception.newStartTime,
                        endTime = exception.newEndTime
                    )
                    allOccurrences.add(CalendarOccurrence(dummySchedule, student, exception, excDate))
                }
            }
        }

        allOccurrences.sortedWith(
            compareBy<CalendarOccurrence> { it.date }.thenBy { it.startTime }
        )
    }
}
