package com.devsusana.hometutorpro.presentation.utils

import com.devsusana.hometutorpro.R
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek

class DayOfWeekUtilsTest {

    @Test
    fun `getResourceId returns correct string resource ID for each day of week`() {
        assertEquals(R.string.day_monday, DayOfWeekUtils.getResourceId(DayOfWeek.MONDAY))
        assertEquals(R.string.day_tuesday, DayOfWeekUtils.getResourceId(DayOfWeek.TUESDAY))
        assertEquals(R.string.day_wednesday, DayOfWeekUtils.getResourceId(DayOfWeek.WEDNESDAY))
        assertEquals(R.string.day_thursday, DayOfWeekUtils.getResourceId(DayOfWeek.THURSDAY))
        assertEquals(R.string.day_friday, DayOfWeekUtils.getResourceId(DayOfWeek.FRIDAY))
        assertEquals(R.string.day_saturday, DayOfWeekUtils.getResourceId(DayOfWeek.SATURDAY))
        assertEquals(R.string.day_sunday, DayOfWeekUtils.getResourceId(DayOfWeek.SUNDAY))
    }
}
