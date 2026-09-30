package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.entities.CalendarOccurrence
import com.devsusana.hometutorpro.domain.entities.ExceptionType
import com.devsusana.hometutorpro.domain.entities.ScheduleException
import com.devsusana.hometutorpro.domain.usecases.IGetAllSchedulesUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetCurrentUserUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetScheduleExceptionsUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetStudentsUseCase
import com.devsusana.hometutorpro.domain.usecases.IGenerateCalendarOccurrencesUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetNextClassUseCase
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

class GetNextClassUseCase @Inject constructor(
    private val getCurrentUserUseCase: IGetCurrentUserUseCase,
    private val getStudentsUseCase: IGetStudentsUseCase,
    private val getAllSchedulesUseCase: IGetAllSchedulesUseCase,
    private val getScheduleExceptionsUseCase: IGetScheduleExceptionsUseCase,
    private val generateCalendarOccurrencesUseCase: IGenerateCalendarOccurrencesUseCase
) : IGetNextClassUseCase {

    override suspend fun invoke(): CalendarOccurrence? {
        val user = getCurrentUserUseCase().value ?: return null
        val students = getStudentsUseCase(user.uid).firstOrNull() ?: return null
        val schedules = getAllSchedulesUseCase(user.uid).firstOrNull() ?: return null

        val allExceptions = mutableListOf<ScheduleException>()
        students.filter { it.id.isNotEmpty() }.forEach { student ->
            getScheduleExceptionsUseCase(user.uid, student.id).firstOrNull()?.let {
                allExceptions.addAll(it)
            }
        }

        val today = LocalDate.now()
        val occurrences = generateCalendarOccurrencesUseCase(
            students = students,
            schedules = schedules,
            exceptions = allExceptions,
            startDate = today,
            endDate = today.plusDays(7)
        )

        return findNextClass(occurrences)
    }

    private fun findNextClass(occurrences: List<CalendarOccurrence>): CalendarOccurrence? {
        val now = java.time.LocalDateTime.now()
        val today = now.toLocalDate()
        val timeNow = now.toLocalTime()

        val validOccurrences = occurrences.filter { 
            it.exception?.type != ExceptionType.CANCELLED 
        }

        val sorted = validOccurrences.sortedWith(
            compareBy<CalendarOccurrence> { it.date }.thenBy { it.startTime }
        )

        val nextToday = sorted.filter { it.date == today }.find {
            val start = it.startTime
            try {
                LocalTime.parse(start).isAfter(timeNow)
            } catch (e: Exception) {
                false
            }
        }

        if (nextToday != null) return nextToday

        val future = sorted.filter { it.date.isAfter(today) }
        if (future.isNotEmpty()) return future.first()

        return null
    }
}
