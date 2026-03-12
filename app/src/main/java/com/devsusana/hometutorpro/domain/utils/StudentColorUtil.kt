package com.devsusana.hometutorpro.domain.utils

import com.devsusana.hometutorpro.domain.entities.StudentSummary

/**
 * Utility for managing and automatically assigning vibrant, accessible colors to students.
 * Provides a wide range of Material Design inspired colors to avoid collisions.
 */
object StudentColorUtil {

    // A wide palette of 20 distinct, vibrant colors suitable for cards and calendar events
    val PRESET_COLORS = listOf(
        // Reds / Pinks
        0xFFD32F2F.toInt(), // Red
        0xFFC2185B.toInt(), // Pink
        0xFFE91E63.toInt(), // Light Pink
        
        // Purples
        0xFF7B1FA2.toInt(), // Purple
        0xFF512DA8.toInt(), // Deep Purple
        0xFF673AB7.toInt(), // Light Purple
        
        // Blues
        0xFF303F9F.toInt(), // Indigo
        0xFF1976D2.toInt(), // Blue
        0xFF0288D1.toInt(), // Light Blue
        0xFF0097A7.toInt(), // Cyan
        
        // Greens
        0xFF00796B.toInt(), // Teal
        0xFF388E3C.toInt(), // Green
        0xFF689F38.toInt(), // Light Green
        
        // Yellows / Oranges
        0xFFFBC02D.toInt(), // Yellow
        0xFFFFA000.toInt(), // Amber
        0xFFF57C00.toInt(), // Orange
        0xFFE64A19.toInt(), // Deep Orange
        
        // Browns / Greys
        0xFF5D4037.toInt(), // Brown
        0xFF616161.toInt(), // Grey
        0xFF455A64.toInt()  // Blue Grey
    )

    /**
     * Determines the optimal color for a new student by finding the preset color
     * that is currently used the least number of times among all students.
     * 
     * @param existingStudents List of all current student summaries belonging to the professor
     * @return An integer representing the color
     */
    fun getLeastUsedColor(existingStudents: List<StudentSummary>): Int {
        if (existingStudents.isEmpty()) {
            return PRESET_COLORS.first()
        }

        // Count how many times each preset color is used
        val colorUsageCounts = mutableMapOf<Int, Int>()
        
        // Initialize all preset colors with 0 usage
        PRESET_COLORS.forEach { colorUsageCounts[it] = 0 }
        
        // Count actual usage
        existingStudents.forEach { student ->
            student.color?.let { color ->
                // If it's a preset color, increment its count
                if (colorUsageCounts.containsKey(color)) {
                    colorUsageCounts[color] = colorUsageCounts[color]!! + 1
                }
            }
        }
        
        // Find the color with the minimum usage count. 
        // If there's a tie, minByOrNull returns the first one in iteration order.
        val leastUsedEntry = colorUsageCounts.minByOrNull { it.value }
        
        return leastUsedEntry?.key ?: PRESET_COLORS.first()
    }
}
