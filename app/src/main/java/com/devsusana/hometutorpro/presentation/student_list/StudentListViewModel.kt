package com.devsusana.hometutorpro.presentation.student_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.devsusana.hometutorpro.domain.usecases.IGetStudentsUseCase
import com.devsusana.hometutorpro.domain.usecases.ILogoutUseCase
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
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StudentListState())
    val state: StateFlow<StudentListState> = _state

    init {
        authRepository.currentUser.onEach { user ->
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

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
        }
    }
}
