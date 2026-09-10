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
        val schedules = listOf(Schedule(id = "sch1", studentId = "stud1"))
        
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
}
