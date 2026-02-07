package com.devsusana.hometutorpro.presentation.student_list.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.domain.entities.Student
import com.devsusana.hometutorpro.presentation.student_list.StudentFilter
import com.devsusana.hometutorpro.presentation.student_list.StudentListState
import com.devsusana.hometutorpro.presentation.student_list.StudentSortOption
import com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentListContent(
    state: StudentListState,
    onStudentClick: (String) -> Unit,
    onAddStudentClick: () -> Unit,
    onWeeklyScheduleClick: () -> Unit,
    onLogout: () -> Unit,
    onSearchQueryChange: (String) -> Unit = {},
    onFilterChange: (StudentFilter) -> Unit = {},
    onSortChange: (StudentSortOption) -> Unit = {}
) {
    Scaffold(
        modifier = Modifier.testTag("student_list_screen"),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.my_students)) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddStudentClick,
                modifier = Modifier.testTag("add_student_button"),
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 12.dp
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.student_list_add_student))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search and Sort Row
            StudentSearchBar(
                query = state.searchQuery,
                onQueryChange = onSearchQueryChange,
                currentSortOption = state.sortBy,
                onSortOptionChange = onSortChange
            )
            
            // Filter Chips
            StudentFilterChips(
                selectedFilter = state.selectedFilter,
                onFilterChange = onFilterChange
            )

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (state.filteredAndSortedStudents.isEmpty()) {
                // Empty State
                StudentListEmptyState(
                    isSearchActive = state.searchQuery.isNotEmpty(),
                    onClearSearch = { onSearchQueryChange("") }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        top = 8.dp,
                        bottom = 80.dp, // Space for FAB
                        start = 12.dp,
                        end = 12.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.filteredAndSortedStudents) { student ->
                        ModernStudentCard(
                            student = student,
                            onClick = { onStudentClick(student.id) }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Student List - Empty")
@Composable
fun StudentListContentPreviewEmpty() {
    HomeTutorProTheme {
        StudentListContent(
            state = StudentListState(),
            onStudentClick = {},
            onAddStudentClick = {},
            onWeeklyScheduleClick = {},
            onLogout = {}
        )
    }
}

@Preview(showBackground = true, name = "Student List - With Students")
@Composable
fun StudentListContentPreviewWithStudents() {
    val students = listOf(
        Student(id = "1", name = "Susana Gonzalez", age = 25, course = "Kotlin", pendingBalance = 150.0, studentEmail = "susana@email.com"),
        Student(id = "2", name = "John Doe", age = 30, course = "Android", pendingBalance = 0.0,  studentEmail = "john@email.com")
    )
    HomeTutorProTheme {
        StudentListContent(
            state = StudentListState(students = students),
            onStudentClick = {},
            onAddStudentClick = {},
            onWeeklyScheduleClick = {},
            onLogout = {}
        )
    }
}

@Preview(showBackground = true, name = "Student List - Loading")
@Composable
fun StudentListContentPreviewLoading() {
    HomeTutorProTheme {
        StudentListContent(
            state = StudentListState(isLoading = true),
            onStudentClick = {},
            onAddStudentClick = {},
            onWeeklyScheduleClick = {},
            onLogout = {}
        )
    }
}
