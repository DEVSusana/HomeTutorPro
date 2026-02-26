package com.devsusana.hometutorpro.domain.entities

import com.devsusana.hometutorpro.domain.core.DomainError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Date

class DomainEntitiesTest {

    @Test
    fun `student should preserve provided fields`() {
        val student = Student(
            id = "1",
            professorId = "prof",
            name = "Ana",
            age = 12,
            pricePerHour = 20.0,
            pendingBalance = 5.0,
            isActive = false
        )

        assertEquals("1", student.id)
        assertEquals("prof", student.professorId)
        assertEquals("Ana", student.name)
        assertEquals(12, student.age)
        assertEquals(20.0, student.pricePerHour, 0.0)
        assertEquals(5.0, student.pendingBalance, 0.0)
        assertFalse(student.isActive)
    }

    @Test
    fun `schedule should expose default times`() {
        val schedule = Schedule()

        assertEquals("09:00", schedule.startTime)
        assertEquals("10:00", schedule.endTime)
    }

    @Test
    fun `schedule type should expose extra id`() {
        assertEquals("EXTRA", ScheduleType.EXTRA_ID)
    }

    @Test
    fun `calendar occurrence should use exception times when provided`() {
        val schedule = Schedule(startTime = "09:00", endTime = "10:00")
        val exception = ScheduleException(newStartTime = "11:00", newEndTime = "12:00")
        val occurrence = CalendarOccurrence(
            schedule = schedule,
            student = StudentSummary(
                id = "1",
                name = "Ana",
                subjects = "Math",
                color = null,
                pendingBalance = 0.0,
                pricePerHour = 20.0,
                isActive = true,
                lastClassDate = null
            ),
            exception = exception,
            date = LocalDate.now()
        )

        assertEquals("11:00", occurrence.startTime)
        assertEquals("12:00", occurrence.endTime)
    }

    @Test
    fun `calendar occurrence should mark extra schedules`() {
        val extraSchedule = Schedule(id = "EXTRA_1")
        val occurrence = CalendarOccurrence(
            schedule = extraSchedule,
            student = StudentSummary(
                id = "1",
                name = "Ana",
                subjects = "Math",
                color = null,
                pendingBalance = 0.0,
                pricePerHour = 20.0,
                isActive = true,
                lastClassDate = null
            ),
            exception = null,
            date = LocalDate.now()
        )

        assertTrue(occurrence.isExtra)
    }

    @Test
    fun `shared resource should have generated id and timestamp`() {
        val resource = SharedResource(
            studentId = "student1",
            fileName = "file.pdf",
            fileType = "pdf",
            fileSizeBytes = 1000,
            sharedVia = ShareMethod.EMAIL
        )

        assertTrue(resource.id.isNotBlank())
        assertTrue(resource.sharedAt > 0)
    }

    @Test
    fun `resource should keep upload date`() {
        val date = Date()
        val resource = Resource(uploadDate = date)

        assertEquals(date, resource.uploadDate)
    }

    @Test
    fun `user should keep defaults for working hours`() {
        val user = User(uid = "1", email = null, displayName = null)

        assertEquals("08:00", user.workingStartTime)
        assertEquals("23:00", user.workingEndTime)
    }

    @Test
    fun `bulk schedule result should default to empty values`() {
        val result = BulkScheduleResult()

        assertTrue(result.processedSchedules.isEmpty())
        assertTrue(result.errors.isEmpty())
        assertFalse(result.isSuccessful)
    }

    @Test
    fun `payment type should contain expected values`() {
        assertTrue(PaymentType.values().contains(PaymentType.EFFECTIVE))
        assertTrue(PaymentType.values().contains(PaymentType.BIZUM))
    }

    @Test
    fun `schedule exception should preserve type and day`() {
        val exception = ScheduleException(
            id = "ex1",
            type = ExceptionType.RESCHEDULED,
            newDayOfWeek = DayOfWeek.FRIDAY
        )

        assertEquals(ExceptionType.RESCHEDULED, exception.type)
        assertEquals(DayOfWeek.FRIDAY, exception.newDayOfWeek)
    }

    @Test
    fun `student summary should be immutable data`() {
        val summary = StudentSummary(
            id = "1",
            name = "Ana",
            subjects = "Math",
            color = 123,
            pendingBalance = 0.0,
            pricePerHour = 20.0,
            isActive = true,
            lastClassDate = 10L
        )

        val copied = summary.copy(name = "Ana")

        assertEquals(summary, copied)
        assertNotEquals(summary.copy(name = "Ana B"), copied)
    }

    @Test
    fun `bulk schedule result should allow error mapping`() {
        val result = BulkScheduleResult(errors = mapOf(0 to DomainError.ScheduleConflict))

        assertNotNull(result.errors[0])
    }
}
