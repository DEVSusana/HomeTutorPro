package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.ActiveSession
import com.devsusana.hometutorpro.domain.entities.CalendarOccurrence
import com.devsusana.hometutorpro.domain.entities.ExceptionType
import com.devsusana.hometutorpro.domain.entities.Schedule
import com.devsusana.hometutorpro.domain.entities.ScheduleException
import com.devsusana.hometutorpro.domain.entities.StudentSummary
import com.devsusana.hometutorpro.domain.entities.User
import com.devsusana.hometutorpro.domain.usecases.implementations.GetTodayClassesSummaryUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class GetTodayClassesSummaryUseCaseTest {

    private val getCurrentUserUseCase: IGetCurrentUserUseCase = mockk()
    private val getStudentsUseCase: IGetStudentsUseCase = mockk()
    private val getAllSchedulesUseCase: IGetAllSchedulesUseCase = mockk()
    private val getAllScheduleExceptionsUseCase: IGetAllScheduleExceptionsUseCase = mockk()
    private val generateCalendarOccurrencesUseCase: IGenerateCalendarOccurrencesUseCase = mockk()
    private val getActiveSessionUseCase: IGetActiveSessionUseCase = mockk(relaxed = true)

    private val useCase = GetTodayClassesSummaryUseCase(
        getCurrentUserUseCase,
        getStudentsUseCase,
        getAllSchedulesUseCase,
        getAllScheduleExceptionsUseCase,
        generateCalendarOccurrencesUseCase,
        getActiveSessionUseCase
    )

    private fun createStudent(
        id: String,
        name: String,
        lastClassDate: Long? = null
    ): StudentSummary = StudentSummary(
        id = id,
        name = name,
        subjects = "Math",
        color = null,
        pendingBalance = 0.0,
        pricePerHour = 10.0,
        isActive = true,
        lastClassDate = lastClassDate
    )

    @Test
    fun `invoke should return empty summary when user is null`() = runTest {
        val userStateFlow = MutableStateFlow<User?>(null)
        every { getCurrentUserUseCase() } returns userStateFlow

        val result = useCase()

        assertEquals(0, result.totalClassesToday)
        assertEquals(0, result.completedClassesToday)
        assertEquals(0, result.currentClassIndex)
        assertFalse(result.isAllCompleted)
    }

    @Test
    fun `invoke should calculate total and completed classes correctly`() = runTest {
        val user = User(uid = "user123", email = "test@example.com", displayName = "Prof")
        val userStateFlow = MutableStateFlow<User?>(user)
        every { getCurrentUserUseCase() } returns userStateFlow

        val today = LocalDate.now()
        val todayEpochMillis = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val student1 = createStudent(id = "s1", name = "Pablo", lastClassDate = todayEpochMillis)
        val student2 = createStudent(id = "s2", name = "Lucia", lastClassDate = null)
        val students = listOf(student1, student2)

        val schedule1 = Schedule(id = "sch1", studentId = "s1", professorId = "user123", startTime = "10:00", endTime = "11:00")
        val schedule2 = Schedule(id = "sch2", studentId = "s2", professorId = "user123", startTime = "23:00", endTime = "23:59")
        val schedules = listOf(schedule1, schedule2)

        every { getStudentsUseCase("user123") } returns flowOf(students)
        every { getAllSchedulesUseCase("user123") } returns flowOf(schedules)
        every { getAllScheduleExceptionsUseCase("user123") } returns flowOf(emptyList())

        val occurrence1 = CalendarOccurrence(schedule = schedule1, student = student1, date = today)
        val occurrence2 = CalendarOccurrence(schedule = schedule2, student = student2, date = today)

        coEvery {
            generateCalendarOccurrencesUseCase(
                students = students,
                schedules = schedules,
                exceptions = emptyList(),
                startDate = today,
                endDate = today
            )
        } returns listOf(occurrence1, occurrence2)

        coEvery { getActiveSessionUseCase() } returns null

        val result = useCase()

        assertEquals(2, result.totalClassesToday)
        assertEquals(1, result.completedClassesToday)
        assertEquals(2, result.currentClassIndex)
        assertFalse(result.isAllCompleted)
    }

    @Test
    fun `invoke should mark as completed when all classes completed`() = runTest {
        val user = User(uid = "user123", email = "test@example.com", displayName = "Prof")
        every { getCurrentUserUseCase() } returns MutableStateFlow(user)

        val today = LocalDate.now()
        val todayEpochMillis = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val student1 = createStudent(id = "s1", name = "Pablo", lastClassDate = todayEpochMillis)
        val students = listOf(student1)
        val schedule1 = Schedule(id = "sch1", studentId = "s1", professorId = "user123", startTime = "09:00", endTime = "10:00")
        val schedules = listOf(schedule1)

        every { getStudentsUseCase("user123") } returns flowOf(students)
        every { getAllSchedulesUseCase("user123") } returns flowOf(schedules)
        every { getAllScheduleExceptionsUseCase("user123") } returns flowOf(emptyList())

        val occurrence1 = CalendarOccurrence(schedule = schedule1, student = student1, date = today)
        coEvery {
            generateCalendarOccurrencesUseCase(
                students = students,
                schedules = schedules,
                exceptions = emptyList(),
                startDate = today,
                endDate = today
            )
        } returns listOf(occurrence1)

        coEvery { getActiveSessionUseCase() } returns null

        val result = useCase()

        assertEquals(1, result.totalClassesToday)
        assertEquals(1, result.completedClassesToday)
        assertEquals(1, result.currentClassIndex)
        assertTrue(result.isAllCompleted)
    }

    @Test
    fun `invoke should not mark class as completed if active session is ongoing for that student`() = runTest {
        val user = User(uid = "user123", email = "test@example.com", displayName = "Prof")
        every { getCurrentUserUseCase() } returns MutableStateFlow(user)

        val today = LocalDate.now()
        val student1 = createStudent(id = "s1", name = "Pablo", lastClassDate = null)
        val students = listOf(student1)
        val schedule1 = Schedule(id = "sch1", studentId = "s1", professorId = "user123", startTime = "08:00", endTime = "09:00")
        val schedules = listOf(schedule1)

        every { getStudentsUseCase("user123") } returns flowOf(students)
        every { getAllSchedulesUseCase("user123") } returns flowOf(schedules)
        every { getAllScheduleExceptionsUseCase("user123") } returns flowOf(emptyList())

        val occurrence1 = CalendarOccurrence(schedule = schedule1, student = student1, date = today)
        coEvery {
            generateCalendarOccurrencesUseCase(
                students = students,
                schedules = schedules,
                exceptions = emptyList(),
                startDate = today,
                endDate = today
            )
        } returns listOf(occurrence1)

        // Session is ongoing for s1
        coEvery { getActiveSessionUseCase() } returns ActiveSession(
            studentId = "s1",
            studentName = "Pablo",
            startTimeMillis = System.currentTimeMillis() - 1000,
            durationMinutes = 60,
            isOngoing = true,
            tutorId = "user123"
        )

        val result = useCase()

        assertEquals(1, result.totalClassesToday)
        assertEquals(0, result.completedClassesToday)
        assertEquals(1, result.currentClassIndex)
        assertFalse(result.isAllCompleted)
    }

    @Test
    fun `invoke should ignore cancelled occurrences`() = runTest {
        val user = User(uid = "user123", email = "test@example.com", displayName = "Prof")
        every { getCurrentUserUseCase() } returns MutableStateFlow(user)

        val today = LocalDate.now()
        val student1 = createStudent(id = "s1", name = "Pablo")
        val students = listOf(student1)
        val schedule1 = Schedule(id = "sch1", studentId = "s1", professorId = "user123", startTime = "15:00", endTime = "16:00")
        val schedules = listOf(schedule1)

        val exception = ScheduleException(
            id = "exc1",
            studentId = "s1",
            type = ExceptionType.CANCELLED
        )

        every { getStudentsUseCase("user123") } returns flowOf(students)
        every { getAllSchedulesUseCase("user123") } returns flowOf(schedules)
        every { getAllScheduleExceptionsUseCase("user123") } returns flowOf(listOf(exception))

        val occurrence1 = CalendarOccurrence(schedule = schedule1, student = student1, exception = exception, date = today)
        coEvery {
            generateCalendarOccurrencesUseCase(
                students = students,
                schedules = schedules,
                exceptions = listOf(exception),
                startDate = today,
                endDate = today
            )
        } returns listOf(occurrence1)

        val result = useCase()

        assertEquals(0, result.totalClassesToday)
        assertEquals(0, result.completedClassesToday)
    }
}
