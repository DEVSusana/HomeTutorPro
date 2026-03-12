package com.devsusana.hometutorpro.presentation.student_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devsusana.hometutorpro.domain.usecases.IGetCurrentUserUseCase
import com.devsusana.hometutorpro.domain.entities.StudentSummary
import com.devsusana.hometutorpro.domain.usecases.IGetStudentsUseCase
import com.devsusana.hometutorpro.domain.usecases.ILogoutUseCase
import com.devsusana.hometutorpro.domain.usecases.IToggleStudentActiveUseCase
import com.devsusana.hometutorpro.presentation.student_list.StudentFilter
import com.devsusana.hometutorpro.presentation.student_list.StudentSortOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentListViewModel @Inject constructor(
    private val getStudentsUseCase: IGetStudentsUseCase,
    private val logoutUseCase: ILogoutUseCase,
    private val toggleStudentActiveUseCase: IToggleStudentActiveUseCase,
    private val getCurrentUserUseCase: IGetCurrentUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(StudentListState())
    val state: StateFlow<StudentListState> = _state

    init {
        getCurrentUserUseCase().onEach { user ->
            if (user != null) {
                getStudents(user.uid)
            }
        }.launchIn(viewModelScope)
    }

    private var getStudentsJob: kotlinx.coroutines.Job? = null

    private fun getStudents(professorId: String) {
        getStudentsJob?.cancel()
        getStudentsJob = getStudentsUseCase(professorId).onEach { students ->
            _state.value = _state.value.copy(students = students)
        }.launchIn(viewModelScope)
    }

    fun onSearchQueryChange(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
    }

    fun onFilterChange(filter: StudentFilter) {
        _state.value = _state.value.copy(selectedFilter = filter)
    }

    fun onSortChange(sortBy: StudentSortOption) {
        _state.value = _state.value.copy(sortBy = sortBy)
    }

    fun onRequestToggleActive(student: StudentSummary) {
        _state.value = _state.value.copy(confirmToggleStudent = student)
    }

    fun onDismissToggleDialog() {
        _state.value = _state.value.copy(confirmToggleStudent = null)
    }

    fun onConfirmToggleActive() {
        val student = _state.value.confirmToggleStudent ?: return
        val professorId = getCurrentUserUseCase().value?.uid ?: return

        viewModelScope.launch {
            toggleStudentActiveUseCase(professorId, student.id)
            _state.value = _state.value.copy(confirmToggleStudent = null)
        }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
        }
    }
}
