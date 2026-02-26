package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.Schedule
import com.devsusana.hometutorpro.domain.usecases.implementations.CheckScheduleConflictUseCase
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class CheckScheduleConflictUseCaseTest {

    private val useCase = CheckScheduleConflictUseCase()

    @Test
    fun `invoke should return true when schedules overlap on same day`() {
        val newSchedule = Schedule(
            id = "new",
            dayOfWeek = DayOfWeek.MONDAY,
            startTime = "10:00",
            endTime = "11:00"
        )
        val existingSchedules = listOf(
            Schedule(
                id = "existing",
                dayOfWeek = DayOfWeek.MONDAY,
                startTime = "10:30",
                endTime = "11:30"
            )
        )

        val result = useCase(newSchedule, existingSchedules)

        assertTrue(result)
    }

    @Test
    fun `invoke should return false when schedules are on different days`() {
        val newSchedule = Schedule(
            id = "new",
            dayOfWeek = DayOfWeek.MONDAY,
            startTime = "10:00",
            endTime = "11:00"
        )
        val existingSchedules = listOf(
            Schedule(
                id = "existing",
                dayOfWeek = DayOfWeek.TUESDAY,
                startTime = "10:30",
                endTime = "11:30"
            )
        )

        val result = useCase(newSchedule, existingSchedules)

        assertFalse(result)
    }

    @Test
    fun `invoke should ignore self when editing same schedule`() {
        val schedule = Schedule(
            id = "same",
            dayOfWeek = DayOfWeek.MONDAY,
            startTime = "10:00",
            endTime = "11:00"
        )
        val existingSchedules = listOf(
            schedule.copy(startTime = "10:30", endTime = "11:30")
        )

        val result = useCase(schedule, existingSchedules)

        assertFalse(result)
    }

    @Test
    fun `invoke should return false when times are invalid`() {
        val newSchedule = Schedule(
            id = "new",
            dayOfWeek = DayOfWeek.MONDAY,
            startTime = "invalid",
            endTime = "11:00"
        )
        val existingSchedules = listOf(
            Schedule(
                id = "existing",
                dayOfWeek = DayOfWeek.MONDAY,
                startTime = "10:30",
                endTime = "11:30"
            )
        )

        val result = useCase(newSchedule, existingSchedules)

        assertFalse(result)
    }
}
