package com.devsusana.hometutorpro.presentation.utils

import androidx.compose.ui.graphics.Color
import kotlin.math.absoluteValue

object ColorUtils {
    // A palette of pleasant, distinct colors
    val studentColors = listOf(
        Color(0xFFE57373), // Red 300
        Color(0xFFF06292), // Pink 300
        Color(0xFFBA68C8), // Purple 300
        Color(0xFF9575CD), // Deep Purple 300
        Color(0xFF7986CB), // Indigo 300
        Color(0xFF64B5F6), // Blue 300
        Color(0xFF4FC3F7), // Light Blue 300
        Color(0xFF4DD0E1), // Cyan 300
        Color(0xFF4DB6AC), // Teal 300
        Color(0xFF81C784), // Green 300
        Color(0xFFAED581), // Light Green 300
        Color(0xFFFFD54F), // Amber 300
        Color(0xFFFFB74D), // Orange 300
        Color(0xFFFF8A65), // Deep Orange 300
        Color(0xFFA1887F), // Brown 300
        Color(0xFF90A4AE)  // Blue Grey 300
    )

    fun getStudentColor(studentId: String): Color {
        if (studentId.isEmpty()) return Color.Gray
        val hash = studentId.hashCode().absoluteValue
        return studentColors[hash % studentColors.size]
    }
}
