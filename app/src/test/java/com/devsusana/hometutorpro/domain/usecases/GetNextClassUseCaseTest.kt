package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.CalendarOccurrence
import com.devsusana.hometutorpro.domain.entities.Schedule
import com.devsusana.hometutorpro.domain.entities.StudentSummary
import com.devsusana.hometutorpro.domain.entities.User
import com.devsusana.hometutorpro.domain.usecases.implementations.GetNextClassUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class GetNextClassUseCaseTest {

    private val getCurrentUserUseCase: IGetCurrentUserUseCase = mockk()
    private val getStudentsUseCase: IGetStudentsUseCase = mockk()
    private val getAllSchedulesUseCase: IGetAllSchedulesUseCase = mockk()
    private val getScheduleExceptionsUseCase: IGetScheduleExceptionsUseCase = mockk()
    private val generateCalendarOccurrencesUseCase: IGenerateCalendarOccurrencesUseCase = mockk()

    private val useCase = GetNextClassUseCase(
        getCurrentUserUseCase,
        getStudentsUseCase,
        getAllSchedulesUseCase,
        getScheduleExceptionsUseCase,
        generateCalendarOccurrencesUseCase
    )

    @Test
    fun `invoke should return null when user is null`() = runTest {
        val userStateFlow = MutableStateFlow<User?>(null)
        every { getCurrentUserUseCase() } returns userStateFlow

        val result = useCase()

        assertNull(result)
    }

    @Test
    fun `invoke should return next class occurrence`() = runTest {
        val user = User(uid = "user123", email = "test@example.com", displayName = "Prof")
        val students = listOf(
            StudentSummary(
                id = "stud1",
                name = "Juan",
                subjects = "Math",
                color = null,
                pendingBalance = 0.0,
                pricePerHour = 10.0,
                isActive = true,
                lastClassDate = null
            )
        )
        val schedules = listOf(Schedule(id = "sch1", studentId = "stud1", startTime = "18:00", endTime = "19:00"))

        val userStateFlow = MutableStateFlow<User?>(user)
        every { getCurrentUserUseCase() } returns userStateFlow
        every { getStudentsUseCase(user.uid) } returns flowOf(students)
        every { getAllSchedulesUseCase(user.uid) } returns flowOf(schedules)
        every { getScheduleExceptionsUseCase(user.uid, "stud1") } returns flowOf(emptyList())

        val occurrence = CalendarOccurrence(
            schedule = schedules[0],
            student = students[0],
            exception = null,
            date = LocalDate.now().plusDays(1)
        )

        coEvery {
            generateCalendarOccurrencesUseCase(
                students = students,
                schedules = schedules,
                exceptions = emptyList(),
                startDate = any(),
                endDate = any()
            )
        } returns listOf(occurrence)

        val result = useCase()

        assertEquals(occurrence, result)
    }

    @Test
    fun `invoke should exclude rescheduled source slot on original date and pick target slot`() = runTest {
        val user = User(uid = "user123", email = "test@example.com", displayName = "Prof")
        val students = listOf(
            StudentSummary(
                id = "stud1",
                name = "Juan",
                subjects = "Math",
                color = null,
                pendingBalance = 0.0,
                pricePerHour = 10.0,
                isActive = true,
                lastClassDate = null
            )
        )
        val schedule = Schedule(
            id = "sch1",
            studentId = "stud1",
            dayOfWeek = java.time.DayOfWeek.MONDAY,
            startTime = "18:00",
            endTime = "19:00"
        )
        val userStateFlow = MutableStateFlow<User?>(user)
        every { getCurrentUserUseCase() } returns userStateFlow
        every { getStudentsUseCase(user.uid) } returns flowOf(students)
        every { getAllSchedulesUseCase(user.uid) } returns flowOf(listOf(schedule))
        every { getScheduleExceptionsUseCase(user.uid, "stud1") } returns flowOf(emptyList())

        val today = LocalDate.now()
        // Source occurrence on original day marked as rescheduled to a different day
        val sourceOccurrence = CalendarOccurrence(
            schedule = schedule,
            student = students[0],
            exception = com.devsusana.hometutorpro.domain.entities.ScheduleException(
                id = "exc1",
                studentId = "stud1",
                originalScheduleId = "sch1",
                date = 1000L,
                type = com.devsusana.hometutorpro.domain.entities.ExceptionType.RESCHEDULED,
                newStartTime = "16:00",
                newEndTime = "17:00",
                newDayOfWeek = today.dayOfWeek.plus(2) // Different day
            ),
            date = today
        )
        // Target occurrence on the rescheduled date
        val targetOccurrence = CalendarOccurrence(
            schedule = Schedule(
                id = "RESCHEDULED_exc1",
                studentId = "stud1",
                dayOfWeek = today.dayOfWeek.plus(2),
                startTime = "16:00",
                endTime = "17:00"
            ),
            student = students[0],
            exception = sourceOccurrence.exception,
            date = today.plusDays(2)
        )

        coEvery {
            generateCalendarOccurrencesUseCase(
                students = students,
                schedules = listOf(schedule),
                exceptions = emptyList(),
                startDate = any(),
                endDate = any()
            )
        } returns listOf(sourceOccurrence, targetOccurrence)

        val result = useCase()

        // Should NOT pick sourceOccurrence; must pick targetOccurrence
        assertEquals(targetOccurrence, result)
    }
}
