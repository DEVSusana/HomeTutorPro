package com.devsusana.hometutorpro.data.mappers

import com.devsusana.hometutorpro.data.mappers.toData
import com.devsusana.hometutorpro.data.mappers.toDomain
import com.devsusana.hometutorpro.data.models.ScheduleExceptionDataModel
import com.devsusana.hometutorpro.domain.entities.ExceptionType
import com.devsusana.hometutorpro.domain.entities.ScheduleException
import org.junit.Assert.*
import org.junit.Test
import java.time.DayOfWeek

/**
 * Unit tests for ScheduleException mapper functions.
 * 
 * Tests bidirectional conversion between ScheduleExceptionDataModel and ScheduleException domain entity.
 * Handles ExceptionType and DayOfWeek enum conversions.
 */
class ScheduleExceptionMapperTest {

    @Test
    fun `toDomain should convert ScheduleExceptionDataModel to ScheduleException correctly`() {
        // Given
        val dataModel = ScheduleExceptionDataModel(
            id = "exception123",
            studentId = "student456",
            date = 1234567890L,
            type = "CANCELLED",
            originalScheduleId = "schedule789",
            newStartTime = "",
            newEndTime = "",
            newDayOfWeek = null,
            reason = "Holiday"
        )

        // When
        val domain = dataModel.toDomain()

        // Then
        assertEquals(dataModel.id, domain.id)
        assertEquals(dataModel.studentId, domain.studentId)
        assertEquals(dataModel.date, domain.date)
        assertEquals(ExceptionType.CANCELLED, domain.type)
        assertEquals(dataModel.originalScheduleId, domain.originalScheduleId)
        assertEquals(dataModel.reason, domain.reason)
        assertEquals("", domain.newStartTime)
        assertEquals("", domain.newEndTime)
        assertNull(domain.newDayOfWeek)
    }

    @Test
    fun `toData should convert ScheduleException to ScheduleExceptionDataModel correctly`() {
        // Given
        val domain = ScheduleException(
            id = "exception456",
            studentId = "student789",
            date = 9876543210L,
            type = ExceptionType.RESCHEDULED,
            originalScheduleId = "schedule101",
            newStartTime = "10:00",
            newEndTime = "11:00",
            newDayOfWeek = DayOfWeek.FRIDAY,
            reason = "Student requested change"
        )

        // When
        val dataModel = domain.toData()

        // Then
        assertEquals(domain.id, dataModel.id)
        assertEquals(domain.studentId, dataModel.studentId)
        assertEquals(domain.date, dataModel.date)
        assertEquals("RESCHEDULED", dataModel.type)
        assertEquals(domain.originalScheduleId, dataModel.originalScheduleId)
        assertEquals(domain.newStartTime, dataModel.newStartTime)
        assertEquals(domain.newEndTime, dataModel.newEndTime)
        assertEquals("FRIDAY", dataModel.newDayOfWeek)
        assertEquals(domain.reason, dataModel.reason)
    }

    @Test
    fun `round trip conversion should preserve all data for cancelled exception`() {
        // Given
        val original = ScheduleException(
            id = "exception999",
            studentId = "student999",
            date = 1111111111L,
            type = ExceptionType.CANCELLED,
            originalScheduleId = "schedule999",
            newStartTime = "",
            newEndTime = "",
            newDayOfWeek = null,
            reason = "Cancelled due to illness"
        )

        // When
        val roundTrip = original.toData().toDomain()

        // Then
        assertEquals(original.id, roundTrip.id)
        assertEquals(original.studentId, roundTrip.studentId)
        assertEquals(original.date, roundTrip.date)
        assertEquals(original.type, roundTrip.type)
        assertEquals(original.originalScheduleId, roundTrip.originalScheduleId)
        assertEquals(original.reason, roundTrip.reason)
        assertEquals("", roundTrip.newStartTime)
        assertEquals("", roundTrip.newEndTime)
        assertNull(roundTrip.newDayOfWeek)
    }

    @Test
    fun `round trip conversion should preserve all data for rescheduled exception`() {
        // Given
        val original = ScheduleException(
            id = "exception888",
            studentId = "student888",
            date = 2222222222L,
            type = ExceptionType.RESCHEDULED,
            originalScheduleId = "schedule888",
            newStartTime = "15:00",
            newEndTime = "16:00",
            newDayOfWeek = DayOfWeek.TUESDAY,
            reason = "Rescheduled to next week"
        )

        // When
        val roundTrip = original.toData().toDomain()

        // Then
        assertEquals(original.id, roundTrip.id)
        assertEquals(original.studentId, roundTrip.studentId)
        assertEquals(original.date, roundTrip.date)
        assertEquals(original.type, roundTrip.type)
        assertEquals(original.originalScheduleId, roundTrip.originalScheduleId)
        assertEquals(original.newStartTime, roundTrip.newStartTime)
        assertEquals(original.newEndTime, roundTrip.newEndTime)
        assertEquals(original.newDayOfWeek, roundTrip.newDayOfWeek)
        assertEquals(original.reason, roundTrip.reason)
    }

    @Test
    fun `toDomain should handle invalid ExceptionType with default CANCELLED`() {
        // Given
        val dataModel = ScheduleExceptionDataModel(
            id = "test1",
            studentId = "student1",
            date = 0L,
            type = "INVALID_TYPE",
            originalScheduleId = "schedule1",
            newStartTime = "",
            newEndTime = "",
            newDayOfWeek = null,
            reason = "Test"
        )

        // When
        val domain = dataModel.toDomain()

        // Then
        assertEquals(ExceptionType.CANCELLED, domain.type)
    }

    @Test
    fun `toDomain should handle invalid DayOfWeek with null`() {
        // Given
        val dataModel = ScheduleExceptionDataModel(
            id = "test2",
            studentId = "student2",
            date = 0L,
            type = "RESCHEDULED",
            originalScheduleId = "schedule2",
            newStartTime = "10:00",
            newEndTime = "11:00",
            newDayOfWeek = "INVALID_DAY",
            reason = "Test"
        )

        // When
        val domain = dataModel.toDomain()

        // Then
        assertNull(domain.newDayOfWeek)
    }

    @Test
    fun `toDomain should handle all DayOfWeek values correctly`() {
        val days = listOf(
            "MONDAY" to DayOfWeek.MONDAY,
            "TUESDAY" to DayOfWeek.TUESDAY,
            "WEDNESDAY" to DayOfWeek.WEDNESDAY,
            "THURSDAY" to DayOfWeek.THURSDAY,
            "FRIDAY" to DayOfWeek.FRIDAY,
            "SATURDAY" to DayOfWeek.SATURDAY,
            "SUNDAY" to DayOfWeek.SUNDAY
        )

        days.forEach { (stringDay, enumDay) ->
            // Given
            val dataModel = ScheduleExceptionDataModel(
                id = "test",
                studentId = "student",
                date = 0L,
                type = "RESCHEDULED",
                originalScheduleId = "schedule",
                newStartTime = "10:00",
                newEndTime = "11:00",
                newDayOfWeek = stringDay,
                reason = "Test"
            )

            // When
            val domain = dataModel.toDomain()

            // Then
            assertEquals(enumDay, domain.newDayOfWeek)
        }
    }
}
