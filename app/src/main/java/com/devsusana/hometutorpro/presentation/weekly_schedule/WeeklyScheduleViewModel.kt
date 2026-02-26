package com.devsusana.hometutorpro.presentation.weekly_schedule

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.core.utils.NotificationHelper
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.entities.ScheduleException
import com.devsusana.hometutorpro.domain.usecases.IGetAllSchedulesUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetCurrentUserUseCase
import com.devsusana.hometutorpro.domain.usecases.IDeleteScheduleExceptionUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetScheduleExceptionsUseCase
import com.devsusana.hometutorpro.domain.usecases.ISaveScheduleExceptionUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetStudentsUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetStudentByIdUseCase
import com.devsusana.hometutorpro.domain.usecases.IGenerateCalendarOccurrencesUseCase
import com.devsusana.hometutorpro.domain.entities.ScheduleType
import com.devsusana.hometutorpro.domain.usecases.implementations.CleanupDuplicatesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.devsusana.hometutorpro.domain.usecases.ISaveStudentUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class WeeklyScheduleViewModel @Inject constructor(
    private val getCurrentUserUseCase: IGetCurrentUserUseCase,
    private val getStudentsUseCase: IGetStudentsUseCase,
    private val getAllSchedulesUseCase: IGetAllSchedulesUseCase,
    private val getStudentByIdUseCase: IGetStudentByIdUseCase,
    private val getScheduleExceptionsUseCase: IGetScheduleExceptionsUseCase,
    private val saveScheduleExceptionUseCase: ISaveScheduleExceptionUseCase,
    private val deleteScheduleExceptionUseCase: IDeleteScheduleExceptionUseCase,
    private val saveStudentUseCase: ISaveStudentUseCase,
    private val generateCalendarOccurrencesUseCase: IGenerateCalendarOccurrencesUseCase,
    private val cleanupDuplicatesUseCase: CleanupDuplicatesUseCase,
    private val application: Application
) : ViewModel() {

    private val _state = MutableStateFlow(WeeklyScheduleState())
    val state: StateFlow<WeeklyScheduleState> = _state.asStateFlow()

    init {
        loadWeeklySchedule()
    }

    private fun loadWeeklySchedule() {
        viewModelScope.launch {
            // Clean up existing duplicates first
            cleanupDuplicatesUseCase()
            
            _state.update { it.copy(isLoading = true) }
            
            val minDelayJob = launch { kotlinx.coroutines.delay(300) }
            
            getCurrentUserUseCase().filterNotNull().flatMapLatest { user ->
                combine(
                    getStudentsUseCase(user.uid),
                    getAllSchedulesUseCase(user.uid)
                ) { students, schedules ->
                    Triple(user, students, schedules)
                }
            }.collect { (user, students, schedules) ->
                val today = LocalDate.now()
                val startOfWeek = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val endOfWeek = startOfWeek.plusDays(6)
                
                val allExceptions = mutableListOf<com.devsusana.hometutorpro.domain.entities.ScheduleException>()
                students.filter { it.id.isNotEmpty() }.forEach { student ->
                    allExceptions.addAll(getScheduleExceptionsUseCase(user.uid, student.id).first())
                }

                val occurrences = generateCalendarOccurrencesUseCase(
                    students = students,
                    schedules = schedules,
                    exceptions = allExceptions,
                    startDate = startOfWeek,
                    endDate = endOfWeek
                )

                val allRegularSchedules = occurrences.map { WeeklyScheduleItem.Regular(it) }
                
                val groupedByDay = mutableMapOf<DayOfWeek, List<WeeklyScheduleItem>>()

                for (day in DayOfWeek.entries) {
                    val daySchedules = allRegularSchedules
                        .filter { (it.exception?.newDayOfWeek ?: it.schedule.dayOfWeek) == day }
                        .sortedBy { it.startTime }

                    if (daySchedules.isNotEmpty()) {
                        groupedByDay[day] = daySchedules
                    }
                }
                
                minDelayJob.join()
                
                _state.update { 
                    it.copy(
                        schedulesByDay = groupedByDay,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun refreshSchedule() {
        loadWeeklySchedule()
    }

    fun onScheduleClick(scheduleItem: WeeklyScheduleItem.Regular) {
        _state.update { 
            it.copy(
                showExceptionDialog = true,
                selectedSchedule = scheduleItem
            )
        }
    }

    fun dismissDialog() {
        _state.update { it.copy(showExceptionDialog = false, selectedSchedule = null) }
    }

    fun saveException(exception: ScheduleException) {
        if (_state.value.isLoading) return // Prevent multiple clicks
        
        viewModelScope.launch {
            val uid = getCurrentUserUseCase().value?.uid ?: return@launch
            val studentId = _state.value.selectedSchedule?.student?.id ?: return@launch
            
            _state.update { it.copy(isLoading = true) }
            when (val result = saveScheduleExceptionUseCase(uid, studentId, exception)) {
                is Result.Success -> {
                    dismissDialog()
                    loadWeeklySchedule() 
                    _state.update { 
                        it.copy(
                            successMessage = application.getString(R.string.weekly_schedule_success_exception_saved)
                        ) 
                    }
                }
                is Result.Error -> {
                    val errorMsg = when (result.error) {
                        DomainError.ScheduleConflict -> application.getString(R.string.weekly_schedule_error_schedule_conflict)
                        else -> application.getString(R.string.weekly_schedule_error_exception_failed)
                    }
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = errorMsg
                        ) 
                    }
                }
            }
        }
    }

    fun deleteException(exceptionId: String, studentId: String) {
        viewModelScope.launch {
            val uid = getCurrentUserUseCase().value?.uid ?: return@launch
            
            _state.update { it.copy(isLoading = true) }
            when (deleteScheduleExceptionUseCase(uid, studentId, exceptionId)) {
                is Result.Success -> {
                    loadWeeklySchedule() 
                    _state.update { 
                        it.copy(
                            successMessage = application.getString(R.string.weekly_schedule_success_exception_removed)
                        ) 
                    }
                }
                is Result.Error -> {
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = application.getString(R.string.weekly_schedule_error_remove_exception_failed)
                        ) 
                    }
                }
            }
        }
    }

    fun startClass(studentId: String, durationMinutes: Int) {
        viewModelScope.launch {
            val uid = getCurrentUserUseCase().value?.uid ?: return@launch
            
            _state.update { it.copy(isLoading = true) }
            
            getStudentByIdUseCase(uid, studentId).first().let { student ->
                if (student == null) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = application.getString(R.string.student_detail_error_unexpected)
                        )
                    }
                    return@launch
                }
                
                val priceToAdd = (durationMinutes / 60.0) * student.pricePerHour
                val newBalance = student.pendingBalance + priceToAdd
                val updatedStudent = student.copy(pendingBalance = newBalance)
                
                when (saveStudentUseCase(uid, updatedStudent)) {
                    is Result.Success<*> -> {
                        val scheduled = NotificationHelper.scheduleClassEndNotification(
                            application,
                            student.name,
                            durationMinutes.toLong()
                        )
                        
                        dismissDialog()
                        loadWeeklySchedule() 
                        _state.update {
                            it.copy(
                                isLoading = false,
                                successMessage = application.getString(R.string.student_detail_success_class_started, priceToAdd)
                            )
                        }
                    }
                    is Result.Error<*> -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = application.getString(R.string.student_detail_error_update_balance_failed)
                            )
                        }
                    }
                }
            }
        }
    }


    fun clearFeedback() {
        _state.update { it.copy(successMessage = null, errorMessage = null) }
    }
    
    fun openAddExtraClassDialog(studentId: String) {
        _state.update { 
            it.copy(
                showExtraClassDialog = true,
                selectedStudentIdForExtraClass = studentId,
                showExceptionDialog = false // Close exception dialog
            ) 
        }
    }

    fun closeAddExtraClassDialog() {
        _state.update { 
            it.copy(
                showExtraClassDialog = false,
                selectedStudentIdForExtraClass = null
            ) 
        }
    }

    fun saveExtraClass(date: Long, startTime: String, endTime: String, dayOfWeek: DayOfWeek) {
        viewModelScope.launch {
             val uid = getCurrentUserUseCase().value?.uid ?: return@launch
             val studentId = _state.value.selectedStudentIdForExtraClass ?: return@launch
             
             _state.update { it.copy(isLoading = true) }

             // Calculate DayOfWeek from date to ensure visibility
             val localDate = java.time.Instant.ofEpochMilli(date)
                 .atZone(ZoneId.systemDefault())
                 .toLocalDate()

             val extraClass = com.devsusana.hometutorpro.domain.entities.ScheduleException(
                 id = java.util.UUID.randomUUID().toString(),
                 studentId = studentId,
                 date = date,
                 type = com.devsusana.hometutorpro.domain.entities.ExceptionType.EXTRA,
                 originalScheduleId = ScheduleType.EXTRA_ID, // Marker for extra class
                 newStartTime = startTime,
                 newEndTime = endTime,
                 newDayOfWeek = dayOfWeek, // Explicitly set day of week
                 reason = "Extra Class"
             )

             when (val result = saveScheduleExceptionUseCase(uid, studentId, extraClass)) {
                 is Result.Success -> {
                     closeAddExtraClassDialog()
                     loadWeeklySchedule()
                     _state.update {
                         it.copy(
                             successMessage = application.getString(R.string.student_detail_success_extra_class_added)
                         )
                     }
                 }
                 is Result.Error -> {
                     _state.update {
                         it.copy(
                             isLoading = false,
                             errorMessage = application.getString(R.string.student_detail_error_save_failed)
                         )
                     }
                 }
             }
        }
    }

}
