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

        // 1. Generate regular occurrences
        schedules.forEach { schedule ->
            // Use details already present in schedule (JOIN optimization)
            if (schedule.studentName != null && schedule.studentIsActive == true) {
                val student = StudentSummary(
                    id = schedule.studentId,
                    name = schedule.studentName!!,
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
                        
                        allOccurrences.add(CalendarOccurrence(schedule, student, exception, currentDate))
                    }
                    currentDate = currentDate.plusDays(1)
                }
            }
        }

        // 2. Add extra classes (standalone exceptions)
        exceptions.filter { it.type == ExceptionType.EXTRA && it.originalScheduleId == ScheduleType.EXTRA_ID }.forEach { extraException ->
            val student = students.find { it.id == extraException.studentId }
            if (student != null && student.isActive) {
                val extraDate = Instant.ofEpochMilli(extraException.date).atZone(ZoneId.systemDefault()).toLocalDate()
                if (!extraDate.isBefore(startDate) && !extraDate.isAfter(endDate)) {
                    val dummySchedule = Schedule(
                        id = "${ScheduleType.EXTRA_ID}_${extraException.id}",
                        studentId = student.id,
                        dayOfWeek = extraDate.dayOfWeek,
                        startTime = extraException.newStartTime,
                        endTime = extraException.newEndTime
                    )
                    allOccurrences.add(CalendarOccurrence(dummySchedule, student, extraException, extraDate))
                }
            }
        }

        allOccurrences.sortedWith(
            compareBy<CalendarOccurrence> { it.date }.thenBy { it.startTime }
        )
    }
}
