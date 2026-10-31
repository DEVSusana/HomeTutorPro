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
        val studentsMap = students.associateBy { it.id }

        // 1. Generate regular occurrences
        schedules.forEach { schedule ->
            // Use details already present in schedule (JOIN optimization)
            val studentName = schedule.studentName
            if (studentName != null && schedule.studentIsActive == true) {
                val matchedStudent = studentsMap[schedule.studentId]
                val student = StudentSummary(
                    id = schedule.studentId,
                    name = studentName,
                    subjects = schedule.studentSubjects ?: matchedStudent?.subjects ?: "",
                    color = schedule.studentColor ?: matchedStudent?.color,
                    pendingBalance = schedule.studentPendingBalance ?: matchedStudent?.pendingBalance ?: 0.0,
                    pricePerHour = schedule.studentPricePerHour ?: matchedStudent?.pricePerHour ?: 0.0,
                    isActive = true,
                    lastClassDate = matchedStudent?.lastClassDate
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

        // 2. Add extra classes and rescheduled classes (standalone exceptions or moved to another day)
        exceptions.filter { 
            it.type == ExceptionType.EXTRA || 
            (it.type == ExceptionType.RESCHEDULED && 
             it.newStartTime.isNotBlank() && 
             (!consumedExceptionIds.contains(it.id) || (it.newDayOfWeek != null && it.newDayOfWeek != schedules.find { s -> s.id == it.originalScheduleId }?.dayOfWeek)))
        }.forEach { exception ->
            val student = students.find { it.id == exception.studentId }
            if (student != null && student.isActive) {
                val origDate = Instant.ofEpochMilli(exception.date).atZone(ZoneId.systemDefault()).toLocalDate()
                val targetDate = if (exception.newDayOfWeek != null && exception.newDayOfWeek != origDate.dayOfWeek) {
                    val diff = (exception.newDayOfWeek.value - origDate.dayOfWeek.value).toLong()
                    origDate.plusDays(diff)
                } else {
                    origDate
                }
                
                if (!targetDate.isBefore(startDate) && !targetDate.isAfter(endDate)) {
                    val dummySchedule = Schedule(
                        id = if (exception.type == ExceptionType.EXTRA) "${ScheduleType.EXTRA_ID}_${exception.id}" else "RESCHEDULED_${exception.id}",
                        studentId = student.id,
                        dayOfWeek = exception.newDayOfWeek ?: targetDate.dayOfWeek,
                        startTime = exception.newStartTime,
                        endTime = exception.newEndTime
                    )
                    allOccurrences.add(CalendarOccurrence(dummySchedule, student, exception, targetDate))
                }
            }
        }

        allOccurrences.sortedWith(
            compareBy<CalendarOccurrence> { it.date }.thenBy { it.startTime }
        )
    }
}
