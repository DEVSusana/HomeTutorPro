package com.devsusana.hometutorpro.presentation.dashboard

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devsusana.hometutorpro.R
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
import com.devsusana.hometutorpro.presentation.weekly_schedule.WeeklyScheduleItem
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val errorMessage: String? = null,
    val permissionNeeded: Boolean = false
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
    private val application: Application
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            getCurrentUserUseCase().filterNotNull().flatMapLatest { user ->
                _state.update { it.copy(userName = user.displayName ?: "Professor") }
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
                
                val exceptionsMap = mutableMapOf<String, MutableList<ScheduleException>>()
                students.filter { it.id.isNotEmpty() }.forEach { student ->
                    getScheduleExceptionsUseCase(user.uid, student.id).first().forEach { exception ->
                        val key = "${student.id}_${exception.originalScheduleId}_${exception.date}"
                        exceptionsMap.getOrPut(key) { mutableListOf() }.add(exception)
                    }
                }

                val allOccurrences = mutableListOf<WeeklyScheduleItem.Regular>()
                
                schedules.forEach { schedule ->
                    val student = students.find { it.id == schedule.studentId }
                    if (student != null && student.isActive) {
                        var currentDate = startOfWeek
                        while (!currentDate.isAfter(endOfWeek)) {
                            if (currentDate.dayOfWeek == schedule.dayOfWeek) {
                                val dateTimestamp = currentDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                                val key = "${student.id}_${schedule.id}_$dateTimestamp"
                                val exception = exceptionsMap[key]?.firstOrNull()
                                
                                val occurrence = WeeklyScheduleItem.Regular(schedule, student, exception, currentDate)
                                allOccurrences.add(occurrence)
                            }
                            currentDate = currentDate.plusDays(1)
                        }
                    }
                }

                // Add standalone extra classes
                exceptionsMap.values.flatten().filter { it.type == ExceptionType.EXTRA && it.originalScheduleId == "EXTRA" }.forEach { extraException ->
                    val student = students.find { it.id == extraException.studentId }
                    if (student != null && student.isActive) {
                        val extraDate = java.time.Instant.ofEpochMilli(extraException.date).atZone(ZoneId.systemDefault()).toLocalDate()
                        if (!extraDate.isBefore(startOfWeek) && !extraDate.isAfter(endOfWeek)) {
                            val dummySchedule = com.devsusana.hometutorpro.domain.entities.Schedule(
                                id = "EXTRA_${extraException.id}",
                                studentId = student.id,
                                dayOfWeek = extraDate.dayOfWeek,
                                startTime = extraException.newStartTime,
                                endTime = extraException.newEndTime
                            )
                            allOccurrences.add(WeeklyScheduleItem.Regular(dummySchedule, student, extraException, extraDate))
                        }
                    }
                }

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
            it.date?.isAfter(today) == true
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
                                 successMessage = application.getString(R.string.student_detail_success_class_started, priceToAdd),
                                 permissionNeeded = !scheduled
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

    fun clearPermissionNeeded() {
        _state.update { it.copy(permissionNeeded = false) }
    }
    
    fun addExtraClass(studentId: String, dateMillis: Long, startTime: String, endTime: String) {
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
                    originalScheduleId = "EXTRA",
                    newStartTime = startTime,
                    newEndTime = endTime,
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
