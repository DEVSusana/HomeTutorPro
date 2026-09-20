package com.devsusana.hometutorpro.presentation.dashboard

import com.devsusana.hometutorpro.domain.entities.CalendarOccurrence
import com.devsusana.hometutorpro.domain.entities.Schedule
import com.devsusana.hometutorpro.domain.entities.StudentSummary
import com.devsusana.hometutorpro.presentation.weekly_schedule.WeeklyScheduleItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class DashboardStateTest {

    @Test
    fun `default values are correct`() {
        val state = DashboardState()

        assertEquals(0, state.activeStudentsCount)
        assertEquals(0, state.todayPendingClassesCount)
        assertEquals(0.0, state.totalPendingIncome, 0.001)
        assertEquals(0, state.classesThisWeek)
        assertNull(state.nextClass)
        assertTrue(state.isLoading)
        assertEquals("", state.userName)
        assertFalse(state.showExceptionDialog)
        assertNull(state.selectedSchedule)
        assertTrue(state.allSchedules.isEmpty())
        assertNull(state.successMessage)
        assertNull(state.errorMessage)
    }

    @Test
    fun `custom values and copy work as expected`() {
        val schedule = Schedule(
            id = "sched1",
            studentId = "stud1",
            dayOfWeek = DayOfWeek.MONDAY,
            startTime = "10:00",
            endTime = "11:00"
        )
        val student = StudentSummary(
            id = "stud1",
            name = "Alice",
            subjects = "Math",
            color = 0xFF123456.toInt(),
            pendingBalance = 0.0,
            pricePerHour = 20.0,
            isActive = true,
            lastClassDate = null
        )
        val regularItem = WeeklyScheduleItem.Regular(
            occurrence = CalendarOccurrence(
                schedule = schedule,
                student = student,
                exception = null,
                date = LocalDate.of(2026, 9, 21)
            )
        )

        val state = DashboardState(
            activeStudentsCount = 5,
            todayPendingClassesCount = 2,
            totalPendingIncome = 150.0,
            classesThisWeek = 10,
            nextClass = regularItem,
            isLoading = false,
            userName = "Susana",
            showExceptionDialog = true,
            selectedSchedule = regularItem,
            allSchedules = listOf(regularItem),
            successMessage = "Success",
            errorMessage = "Error"
        )

        assertEquals(5, state.activeStudentsCount)
        assertEquals(2, state.todayPendingClassesCount)
        assertEquals(150.0, state.totalPendingIncome, 0.001)
        assertEquals(10, state.classesThisWeek)
        assertEquals(regularItem, state.nextClass)
        assertFalse(state.isLoading)
        assertEquals("Susana", state.userName)
        assertTrue(state.showExceptionDialog)
        assertEquals(regularItem, state.selectedSchedule)
        assertEquals(1, state.allSchedules.size)
        assertEquals("Success", state.successMessage)
        assertEquals("Error", state.errorMessage)

        val copiedState = state.copy(isLoading = true, activeStudentsCount = 6)
        assertTrue(copiedState.isLoading)
        assertEquals(6, copiedState.activeStudentsCount)
        assertEquals("Susana", copiedState.userName)
    }
}
