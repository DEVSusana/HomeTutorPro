package com.devsusana.hometutorpro.presentation.schedule_form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.Schedule
import com.devsusana.hometutorpro.domain.usecases.IGetCurrentUserUseCase
import com.devsusana.hometutorpro.domain.usecases.ISaveScheduleUseCase
import com.devsusana.hometutorpro.presentation.schedule.ScheduleFormState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import javax.inject.Inject

@HiltViewModel
class ScheduleFormViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val saveScheduleUseCase: ISaveScheduleUseCase,
    private val getCurrentUserUseCase: IGetCurrentUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ScheduleFormState())
    val state: StateFlow<ScheduleFormState> = _state.asStateFlow()

    private val studentId: String = savedStateHandle.get<String>("studentId") ?: ""

    fun onScheduleChange(schedule: Schedule) {
        _state.update { it.copy(schedule = schedule) }
    }

    fun onDayOfWeekChange(dayOfWeek: DayOfWeek) {
        _state.update { it.copy(schedule = it.schedule.copy(dayOfWeek = dayOfWeek)) }
    }

    fun onStartTimeChange(time: String) {
        _state.update { it.copy(schedule = it.schedule.copy(startTime = time)) }
    }

    fun onEndTimeChange(time: String) {
        _state.update { it.copy(schedule = it.schedule.copy(endTime = time)) }
    }

    fun saveSchedule() {
        val uid = getCurrentUserUseCase().value?.uid ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = saveScheduleUseCase(uid, studentId, _state.value.schedule)
            when (result) {
                is Result.Success -> {
                    _state.update { it.copy(isLoading = false, isSaved = true) }
                }
                is Result.Error -> {
                    _state.update { it.copy(isLoading = false, error = R.string.schedule_form_error_save_failed) }
                }
            }
        }
    }
}
