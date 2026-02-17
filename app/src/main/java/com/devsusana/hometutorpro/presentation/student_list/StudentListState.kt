package com.devsusana.hometutorpro.presentation.student_list

import com.devsusana.hometutorpro.domain.entities.Student
import java.text.Collator
import java.util.Locale

enum class StudentFilter {
    ALL,
    WITH_BALANCE,
    ACTIVE,
    INACTIVE
}

enum class StudentSortOption {
    NAME,
    BALANCE,
    LAST_CLASS
}

data class StudentListState(
    val isLoading: Boolean = false,
    val students: List<Student> = emptyList(),
    val error: String? = null,
    val searchQuery: String = "",
    val selectedFilter: StudentFilter = StudentFilter.ALL,
    val sortBy: StudentSortOption = StudentSortOption.NAME
) {
    val filteredAndSortedStudents: List<Student>
        get() {
            var result = students
            
            // Apply search filter
            if (searchQuery.isNotEmpty()) {
                result = result.filter { student ->
                    student.name.contains(searchQuery, ignoreCase = true)
                }
            }
            
            // Apply category filter
            result = when (selectedFilter) {
                StudentFilter.ALL -> result
                StudentFilter.WITH_BALANCE -> result.filter { it.pendingBalance > 0 }
                StudentFilter.ACTIVE -> result.filter { it.isActive }
                StudentFilter.INACTIVE -> result.filter { !it.isActive }
            }
            
            // Apply sorting
            result = when (sortBy) {
                StudentSortOption.NAME -> {
                    val collator = Collator.getInstance(Locale("es", "ES")).apply {
                        strength = Collator.PRIMARY
                    }
                    result.sortedWith { s1, s2 -> collator.compare(s1.name, s2.name) }
                }
                StudentSortOption.BALANCE -> result.sortedByDescending { it.pendingBalance }
                StudentSortOption.LAST_CLASS -> result.sortedByDescending { it.lastClassDate ?: 0L }
            }
            
            return result
        }
}
