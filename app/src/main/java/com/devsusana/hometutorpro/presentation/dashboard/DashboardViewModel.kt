package com.devsusana.hometutorpro.presentation.dashboard

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devsusana.hometutorpro.R
import kotlin.OptIn
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.entities.ExceptionType
import com.devsusana.hometutorpro.domain.entities.ScheduleException
import com.devsusana.hometutorpro.domain.usecases.IDeleteScheduleExceptionUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetAllSchedulesUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetCurrentUserUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetScheduleExceptionsUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetStudentByIdUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetStudentsUseCase
import com.devsusana.hometutorpro.domain.usecases.ISaveScheduleExceptionUseCase
import com.devsusana.hometutorpro.domain.usecases.ISaveStudentUseCase
import com.devsusana.hometutorpro.domain.usecases.IGenerateCalendarOccurrencesUseCase
import com.devsusana.hometutorpro.domain.entities.ScheduleType
import com.devsusana.hometutorpro.presentation.weekly_schedule.WeeklyScheduleItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

import com.devsusana.hometutorpro.core.utils.NotificationHelper
import androidx.compose.runtime.Immutable

@Immutable
data class DashboardState(
    val activeStudentsCount: Int = 0,
    val todayPendingClassesCount: Int = 0,
    val totalPendingIncome: Double = 0.0,
    val classesThisWeek: Int = 0,
    val nextClass: WeeklyScheduleItem.Regular? = null,
    val isLoading: Boolean = true,
    val userName: String = "",
    val showExceptionDialog: Boolean = false,
    val selectedSchedule: WeeklyScheduleItem.Regular? = null,
    val allSchedules: List<WeeklyScheduleItem.Regular> = emptyList(),
    val successMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getCurrentUserUseCase: IGetCurrentUserUseCase,
    private val getStudentsUseCase: IGetStudentsUseCase,
    private val getAllSchedulesUseCase: IGetAllSchedulesUseCase,
    private val getScheduleExceptionsUseCase: IGetScheduleExceptionsUseCase,
    private val saveScheduleExceptionUseCase: ISaveScheduleExceptionUseCase,
    private val deleteScheduleExceptionUseCase: IDeleteScheduleExceptionUseCase,
    private val getStudentByIdUseCase: IGetStudentByIdUseCase,
    private val saveStudentUseCase: ISaveStudentUseCase,
    private val generateCalendarOccurrencesUseCase: IGenerateCalendarOccurrencesUseCase,
    private val application: Application
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        loadDashboardData()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadDashboardData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            getCurrentUserUseCase().filterNotNull().flatMapLatest { user ->
                _state.update { it.copy(userName = user.displayName ?: application.getString(R.string.dashboard_default_user_name)) }
                combine(
                    getStudentsUseCase(user.uid),
                    getAllSchedulesUseCase(user.uid)
                ) { students, schedules ->
                    Triple(user, students, schedules)
                }
            }.collect { (user, students, schedules) ->
                val today = LocalDate.now()
                val startOfWeek = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val endOfWeek = startOfWeek.plusDays(13) 
                
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

                val allOccurrences = occurrences.map { WeeklyScheduleItem.Regular(it) }

                val activeCount = students.count { it.isActive }
                val pendingIncome = students.sumOf { it.pendingBalance }
                val classesThisWeekCount = schedules.size

                val nextClass = findNextClass(allOccurrences)

                val timeNow = java.time.LocalTime.now()
                val todayPendingClassesCount = allOccurrences.count { occurrence ->
                    val isToday = occurrence.date == today
                    
                    val start = occurrence.startTime
                    
                    val isPending = try {
                        java.time.LocalTime.parse(start).isAfter(timeNow)
                    } catch (e: Exception) { false }
                    
                    isToday && isPending && (occurrence.exception?.type != ExceptionType.CANCELLED)
                }

                _state.update {
                    it.copy(
                        activeStudentsCount = activeCount,
                        todayPendingClassesCount = todayPendingClassesCount,
                        totalPendingIncome = pendingIncome,
                        classesThisWeek = classesThisWeekCount,
                        nextClass = nextClass,
                        allSchedules = allOccurrences,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun findNextClass(occurrences: List<WeeklyScheduleItem.Regular>): WeeklyScheduleItem.Regular? {
        val now = java.time.LocalDateTime.now()
        val today = now.toLocalDate()
        val timeNow = now.toLocalTime()

        val validOccurrences = occurrences.filter { 
            it.exception?.type != ExceptionType.CANCELLED 
        }
        
        val sorted = validOccurrences.sortedWith(
            compareBy<WeeklyScheduleItem.Regular> { it.date }.thenBy { 
                 it.startTime 
            }
        )
        
        val todayOccurrences = sorted.filter { 
            it.date == today 
        }
        
        val nextToday = todayOccurrences.find {
            val start = it.startTime
            LocalTime.parse(start).isAfter(timeNow)
        }
        
        if (nextToday != null) return nextToday
        
        val future = sorted.filter {
            it.date.isAfter(today)
        }
        
        if (future.isNotEmpty()) return future.first()
        
        return sorted.firstOrNull()
    }

    fun onScheduleClick(item: WeeklyScheduleItem.Regular) {
        _state.update { 
            it.copy(
                showExceptionDialog = true,
                selectedSchedule = item
            )
        }
    }

    fun dismissDialog() {
        _state.update { it.copy(showExceptionDialog = false, selectedSchedule = null) }
    }

    fun saveException(exception: ScheduleException) {
        viewModelScope.launch {
            val uid = getCurrentUserUseCase().value?.uid ?: return@launch
            val studentId = _state.value.selectedSchedule?.student?.id ?: return@launch
            
            _state.update { it.copy(isLoading = true) }
            when (val result = saveScheduleExceptionUseCase(uid, studentId, exception)) {
                is Result.Success -> {
                    dismissDialog()
                    loadDashboardData() 
                    _state.update { it.copy(successMessage = application.getString(R.string.weekly_schedule_success_exception_saved)) }
                }
                is Result.Error -> {
                    val errorMsg = when (result.error) {
                        DomainError.ScheduleConflict -> application.getString(R.string.weekly_schedule_error_schedule_conflict)
                        else -> application.getString(R.string.weekly_schedule_error_exception_failed)
                    }
                    _state.update { it.copy(isLoading = false, errorMessage = errorMsg) }
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
                     loadDashboardData()
                     _state.update { it.copy(successMessage = application.getString(R.string.weekly_schedule_success_exception_removed)) }
                 }
                 is Result.Error -> {
                     _state.update { it.copy(isLoading = false, errorMessage = application.getString(R.string.weekly_schedule_error_remove_exception_failed)) }
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
                    _state.update { it.copy(isLoading = false, errorMessage = application.getString(R.string.student_detail_error_unexpected)) }
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
                        loadDashboardData()
                        _state.update {
                             it.copy(
                                 isLoading = false,
                                 successMessage = application.getString(R.string.student_detail_success_class_started, priceToAdd)
                             )
                        }
                    }
                    is Result.Error<*> -> {
                        _state.update { it.copy(isLoading = false, errorMessage = application.getString(R.string.student_detail_error_update_balance_failed)) }
                    }
                }
            }
        }
    }

    
    fun addExtraClass(studentId: String, dateMillis: Long, startTime: String, endTime: String, dayOfWeek: DayOfWeek) {
        viewModelScope.launch {
            val uid = getCurrentUserUseCase().value?.uid ?: return@launch
            _state.update { it.copy(isLoading = true) }
            
            getStudentByIdUseCase(uid, studentId).first().let { student ->
                if (student == null) {
                    _state.update { it.copy(isLoading = false, errorMessage = application.getString(R.string.student_detail_error_unexpected)) }
                    return@launch
                }
                
                val start = LocalTime.parse(startTime)
                val end = LocalTime.parse(endTime)
                val durationMinutes = java.time.Duration.between(start, end).toMinutes().toInt()
                
                if (durationMinutes <= 0) {
                    _state.update { it.copy(isLoading = false, errorMessage = application.getString(R.string.student_detail_error_unexpected)) }
                    return@launch
                }

                // 1. Save as a ScheduleException so it appears in the counts and schedule
                val extraClass = ScheduleException(
                    id = java.util.UUID.randomUUID().toString(),
                    studentId = student.id,
                    date = dateMillis,
                    type = ExceptionType.EXTRA,
                    originalScheduleId = ScheduleType.EXTRA_ID,
                    newStartTime = startTime,
                    newEndTime = endTime,
                    newDayOfWeek = dayOfWeek,
                    reason = "Extra Class"
                )

                val saveExceptionResult = saveScheduleExceptionUseCase(uid, studentId, extraClass)
                
                if (saveExceptionResult is Result.Error) {
                    _state.update { it.copy(isLoading = false, errorMessage = application.getString(R.string.student_detail_error_save_failed)) }
                    return@launch
                }
                
                // 2. Update student balance
                val priceToAdd = (durationMinutes / 60.0) * student.pricePerHour
                val newBalance = student.pendingBalance + priceToAdd
                val updatedStudent = student.copy(pendingBalance = newBalance)
                
                when (saveStudentUseCase(uid, updatedStudent)) {
                    is Result.Success<*> -> {
                        dismissDialog()
                        loadDashboardData()
                        _state.update {
                             it.copy(
                                 isLoading = false,
                                 successMessage = application.getString(R.string.student_detail_success_extra_class_added)
                             )
                        }
                    }
                    is Result.Error<*> -> {
                        _state.update { it.copy(isLoading = false, errorMessage = application.getString(R.string.student_detail_error_update_balance_failed)) }
                    }
                }
            }
        }
    }
    
    fun clearFeedback() {
        _state.update { it.copy(successMessage = null, errorMessage = null) }
    }
}
