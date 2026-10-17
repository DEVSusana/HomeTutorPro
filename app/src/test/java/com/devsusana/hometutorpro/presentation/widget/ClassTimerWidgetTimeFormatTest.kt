package com.devsusana.hometutorpro.presentation.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalTime

class ClassTimerWidgetTimeFormatTest {

    @Test
    fun `parseTimeSafely should parse 24-hour time HH-mm`() {
        val time = ClassTimerWidget.parseTimeSafely("16:30")
        assertNotNull(time)
        assertEquals(LocalTime.of(16, 30), time)
    }

    @Test
    fun `parseTimeSafely should parse 24-hour time with seconds HH-mm-ss`() {
        val time = ClassTimerWidget.parseTimeSafely("09:15:30")
        assertNotNull(time)
        assertEquals(LocalTime.of(9, 15, 30), time)
    }

    @Test
    fun `parseTimeSafely should strip accidental milliseconds without crashing`() {
        val time = ClassTimerWidget.parseTimeSafely("14:00:00.456")
        assertNotNull(time)
        assertEquals(LocalTime.of(14, 0, 0), time)
    }

    @Test
    fun `parseTimeSafely should parse 12-hour AM-PM strings`() {
        val timePm = ClassTimerWidget.parseTimeSafely("04:00 PM")
        assertNotNull(timePm)
        assertEquals(LocalTime.of(16, 0), timePm)

        val timeAm = ClassTimerWidget.parseTimeSafely("09:30 AM")
        assertNotNull(timeAm)
        assertEquals(LocalTime.of(9, 30), timeAm)
    }

    @Test
    fun `parseTimeSafely should handle whitespace gracefully`() {
        val time = ClassTimerWidget.parseTimeSafely("   18:45   ")
        assertNotNull(time)
        assertEquals(LocalTime.of(18, 45), time)
    }
}
