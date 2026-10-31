package com.devsusana.hometutorpro.domain.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalTime

class ScheduleTimeParserTest {

    @Test
    fun `parseTimeSafely parses standard 24h format`() {
        assertEquals(LocalTime.of(16, 0), ScheduleTimeParser.parseTimeSafely("16:00"))
        assertEquals(LocalTime.of(9, 30), ScheduleTimeParser.parseTimeSafely("09:30"))
        assertEquals(LocalTime.of(9, 5), ScheduleTimeParser.parseTimeSafely("9:5"))
    }

    @Test
    fun `parseTimeSafely parses 24h with seconds`() {
        assertEquals(LocalTime.of(16, 0, 0), ScheduleTimeParser.parseTimeSafely("16:00:00"))
        assertEquals(LocalTime.of(16, 0, 0), ScheduleTimeParser.parseTimeSafely("16:00:00.123"))
    }

    @Test
    fun `parseTimeSafely parses 12h AM PM format`() {
        assertEquals(LocalTime.of(16, 0), ScheduleTimeParser.parseTimeSafely("04:00 PM"))
        assertEquals(LocalTime.of(9, 30), ScheduleTimeParser.parseTimeSafely("9:30 AM"))
    }

    @Test
    fun `parseTimeSafely returns null on invalid or empty string`() {
        assertNull(ScheduleTimeParser.parseTimeSafely(null))
        assertNull(ScheduleTimeParser.parseTimeSafely(""))
        assertNull(ScheduleTimeParser.parseTimeSafely("   "))
        assertNull(ScheduleTimeParser.parseTimeSafely("invalid"))
    }
}
