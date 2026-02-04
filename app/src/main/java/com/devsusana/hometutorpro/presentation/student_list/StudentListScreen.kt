package com.devsusana.hometutorpro.presentation.student_list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.devsusana.hometutorpro.presentation.student_list.components.StudentListContent

@Composable
fun StudentListScreen(
    viewModel: StudentListViewModel = hiltViewModel(),
    onStudentClick: (String) -> Unit,
    onAddStudentClick: () -> Unit,
    onWeeklyScheduleClick: () -> Unit,
    onLogout: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    StudentListContent(
        state = state,
        onStudentClick = onStudentClick,
        onAddStudentClick = onAddStudentClick,
        onWeeklyScheduleClick = onWeeklyScheduleClick,
        onLogout = onLogout,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onFilterChange = viewModel::onFilterChange,
        onSortChange = viewModel::onSortChange
    )
}
