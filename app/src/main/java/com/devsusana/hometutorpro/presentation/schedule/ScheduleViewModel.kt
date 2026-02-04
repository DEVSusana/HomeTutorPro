package com.devsusana.hometutorpro.presentation.schedule

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.domain.usecases.IDeleteScheduleUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetCurrentUserUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetSchedulesUseCase
import com.devsusana.hometutorpro.domain.usecases.ISaveScheduleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getSchedulesUseCase: IGetSchedulesUseCase,
    private val saveScheduleUseCase: ISaveScheduleUseCase,
    private val deleteScheduleUseCase: IDeleteScheduleUseCase,
    private val getCurrentUserUseCase: IGetCurrentUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ScheduleState())
    val state: StateFlow<ScheduleState> = _state

    private val studentId: String = savedStateHandle.get<String>("studentId") ?: ""

    init {
        getSchedules()
    }

    private fun getSchedules() {
        getCurrentUserUseCase()
            .filterNotNull()
            .flatMapLatest { user ->
                getSchedulesUseCase(user.uid, studentId)
            }
            .onEach { schedules ->
                _state.update { it.copy(schedules = schedules) }
            }
            .launchIn(viewModelScope)
    }

    fun deleteSchedule(scheduleId: String) {
        val uid = getCurrentUserUseCase().value?.uid ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (deleteScheduleUseCase(uid, studentId, scheduleId)) {
                is com.devsusana.hometutorpro.domain.core.Result.Success -> {
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            successMessage = R.string.schedule_success_delete
                        ) 
                    }
                }
                is com.devsusana.hometutorpro.domain.core.Result.Error -> {
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = R.string.schedule_error_delete_failed
                        ) 
                    }
                }
            }
        }
    }

    fun clearFeedback() {
        _state.update { it.copy(successMessage = null, errorMessage = null) }
    }
}
