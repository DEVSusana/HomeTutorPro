package com.devsusana.hometutorpro.presentation.utils

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorUtilsTest {

    @Test
    fun `studentColors palette contains expected number of colors`() {
        assertEquals(16, ColorUtils.studentColors.size)
    }

    @Test
    fun `getStudentColor with empty studentId returns Gray`() {
        val color = ColorUtils.getStudentColor("")
        assertEquals(Color.Gray, color)
    }

    @Test
    fun `getStudentColor with valid studentId returns deterministic color from palette`() {
        val color1 = ColorUtils.getStudentColor("student_123")
        val color2 = ColorUtils.getStudentColor("student_123")
        
        assertEquals(color1, color2)
        assertTrue(ColorUtils.studentColors.contains(color1))
    }

    @Test
    fun `getStudentColor returns valid colors for multiple student IDs`() {
        val ids = listOf("student_a", "student_b", "student_c", "12345", "uuid-xyz-987")
        for (id in ids) {
            val color = ColorUtils.getStudentColor(id)
            assertTrue(ColorUtils.studentColors.contains(color))
        }
    }
}
