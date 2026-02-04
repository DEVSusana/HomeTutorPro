package com.devsusana.hometutorpro.data.mappers

import com.devsusana.hometutorpro.data.mappers.toData
import com.devsusana.hometutorpro.data.mappers.toDomain
import com.devsusana.hometutorpro.data.models.ScheduleDataModel
import com.devsusana.hometutorpro.domain.entities.Schedule
import org.junit.Assert.*
import org.junit.Test
import java.time.DayOfWeek

/**
 * Unit tests for Schedule mapper functions.
 * 
 * Tests bidirectional conversion between ScheduleDataModel and Schedule domain entity.
 */
class ScheduleMapperTest {

    @Test
    fun `toDomain should convert ScheduleDataModel to Schedule correctly`() {
        // Given
        val dataModel = ScheduleDataModel(
            id = "schedule123",
            dayOfWeek = "MONDAY",
            startTime = "09:00",
            endTime = "10:00"
        )

        // When
        val domain = dataModel.toDomain("student1")

        // Then
        assertEquals(dataModel.id, domain.id)
        assertEquals("student1", domain.studentId)
        assertEquals(DayOfWeek.MONDAY, domain.dayOfWeek)
        assertEquals(dataModel.startTime, domain.startTime)
        assertEquals(dataModel.endTime, domain.endTime)
    }

    @Test
    fun `toData should convert Schedule to ScheduleDataModel correctly`() {
        // Given
        val domain = Schedule(
            id = "schedule456",
            studentId = "student2",
            dayOfWeek = DayOfWeek.WEDNESDAY,
            startTime = "14:00",
            endTime = "15:30"
        )

        // When
        val dataModel = domain.toData()

        // Then
        assertEquals(domain.id, dataModel.id)
        // studentId is not in DataModel
        assertEquals("WEDNESDAY", dataModel.dayOfWeek)
        assertEquals(domain.startTime, dataModel.startTime)
        assertEquals(domain.endTime, dataModel.endTime)
    }

    @Test
    fun `round trip conversion should preserve all data`() {
        // Given
        val original = Schedule(
            id = "schedule789",
            studentId = "student3",
            dayOfWeek = DayOfWeek.FRIDAY,
            startTime = "16:00",
            endTime = "17:00"
        )

        // When
        val roundTrip = original.toData().toDomain("student3")

        // Then
        assertEquals(original.id, roundTrip.id)
        assertEquals(original.studentId, roundTrip.studentId)
        assertEquals(original.dayOfWeek, roundTrip.dayOfWeek)
        assertEquals(original.startTime, roundTrip.startTime)
        assertEquals(original.endTime, roundTrip.endTime)
    }

    @Test
    fun `toDomain should handle all days of week correctly`() {
        // Test all days
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
            val dataModel = ScheduleDataModel(
                id = "test",
                dayOfWeek = stringDay,
                startTime = "10:00",
                endTime = "11:00"
            )

            // When
            val domain = dataModel.toDomain("student1")

            // Then
            assertEquals(enumDay, domain.dayOfWeek)
        }
    }

    @Test
    fun `toData should handle different time formats`() {
        // Given
        val domain = Schedule(
            id = "schedule999",
            studentId = "student1",
            dayOfWeek = DayOfWeek.SATURDAY,
            startTime = "08:30",
            endTime = "09:45"
        )

        // When
        val dataModel = domain.toData()

        // Then
        assertEquals("08:30", dataModel.startTime)
        assertEquals("09:45", dataModel.endTime)
    }
}
