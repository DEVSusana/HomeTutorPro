package com.devsusana.hometutorpro.presentation.student_detail.delegates

import android.app.Application
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.ExceptionType
import com.devsusana.hometutorpro.domain.entities.Schedule
import com.devsusana.hometutorpro.domain.entities.BulkScheduleResult
import com.devsusana.hometutorpro.domain.entities.ScheduleException
import com.devsusana.hometutorpro.domain.usecases.*
import com.devsusana.hometutorpro.presentation.student_detail.StudentDetailState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.UUID
import java.time.DayOfWeek
import javax.inject.Inject

class StudentScheduleDelegate @Inject constructor(
    private val getSchedulesUseCase: IGetSchedulesUseCase,
    private val saveScheduleUseCase: ISaveScheduleUseCase,
    private val deleteScheduleUseCase: IDeleteScheduleUseCase,
    private val toggleScheduleCompletionUseCase: IToggleScheduleCompletionUseCase,
    private val saveBulkSchedulesUseCase: ISaveBulkSchedulesUseCase,
    private val saveScheduleExceptionUseCase: ISaveScheduleExceptionUseCase,
    private val getAllSchedulesUseCase: IGetAllSchedulesUseCase,
    private val checkScheduleConflictUseCase: ICheckScheduleConflictUseCase,
    private val application: Application
) : IStudentScheduleDelegate {

    override fun loadSchedules(
        professorId: String,
        studentId: String,
        state: MutableStateFlow<StudentDetailState>,
        scope: CoroutineScope
    ) {
        scope.launch {
            getSchedulesUseCase(professorId, studentId).collect { schedules ->
                state.value = state.value.copy(schedules = schedules)
            }
        }
    }

    override fun saveSchedule(
        professorId: String,
        studentId: String,
        schedule: Schedule,
        state: MutableStateFlow<StudentDetailState>,
        scope: CoroutineScope
    ) {
        scope.launch {
            val student = state.value.student ?: return@launch

            if (student.id.isEmpty() || student.id == "new") {
                handleNewStudentSchedule(schedule, professorId, state)
                return@launch
            }

            state.value = state.value.copy(isLoading = true)

            when (val result = saveScheduleUseCase(professorId, studentId, schedule)) {
                is Result.Success -> {
                    state.value = state.value.copy(
                        isLoading = false,
                        successMessage = application.getString(R.string.student_detail_success_schedule_saved)
                    )
                }
                is Result.Error -> {
                    handleScheduleError(result.error, state)
                }
            }
        }
    }

    override fun deleteSchedule(
        professorId: String,
        studentId: String,
        scheduleId: String,
        state: MutableStateFlow<StudentDetailState>,
        scope: CoroutineScope
    ) {
        scope.launch {
            val student = state.value.student ?: return@launch

            if (student.id.isEmpty() || student.id == "new") {
                val list = state.value.pendingSchedules.filterNot { it.id == scheduleId }
                state.value = state.value.copy(
                    pendingSchedules = list,
                    successMessage = application.getString(R.string.student_detail_success_schedule_deleted)
                )
                return@launch
            }

            state.value = state.value.copy(isLoading = true)
            when (val result = deleteScheduleUseCase(professorId, studentId, scheduleId)) {
                is Result.Success -> {
                    state.value = state.value.copy(
                        isLoading = false,
                        successMessage = application.getString(R.string.student_detail_success_schedule_deleted)
                    )
                }
                is Result.Error -> {
                    state.value = state.value.copy(
                        isLoading = false,
                        errorMessage = application.getString(R.string.student_detail_error_schedule_delete_failed)
                    )
                }
            }
        }
    }

    override fun toggleScheduleCompletion(
        professorId: String,
        scheduleId: String,
        state: MutableStateFlow<StudentDetailState>,
        scope: CoroutineScope
    ) {
        scope.launch {
            toggleScheduleCompletionUseCase(professorId, scheduleId)
        }
    }

    override fun saveBulkSchedules(
        professorId: String,
        studentId: String,
        state: MutableStateFlow<StudentDetailState>,
        scope: CoroutineScope
    ) {
        scope.launch {
            val bulkSchedules = state.value.bulkSchedules

            if (bulkSchedules.isEmpty()) {
                state.value = state.value.copy(errorMessage = application.getString(R.string.bulk_schedule_error_empty))
                return@launch
            }

            state.value = state.value.copy(bulkScheduleSaving = true)

            val domainSchedules = bulkSchedules.map { entry ->
                Schedule(
                    id = UUID.randomUUID().toString(),
                    studentId = studentId,
                    dayOfWeek = entry.dayOfWeek,
                    startTime = entry.startTime,
                    endTime = entry.endTime
                )
            }

            val result = saveBulkSchedulesUseCase(
                professorId = professorId,
                studentId = studentId,
                schedules = domainSchedules,
                pendingSchedules = state.value.pendingSchedules
            )

            if (result.isSuccessful) {
                val isNewStudent = studentId.isEmpty() || studentId == "new"
                state.value = state.value.copy(
                    bulkScheduleSaving = false,
                    isBulkScheduleMode = false,
                    bulkSchedules = emptyList(),
                    pendingSchedules = if (isNewStudent) state.value.pendingSchedules + result.processedSchedules else state.value.pendingSchedules,
                    successMessage = if (isNewStudent) 
                        application.getString(R.string.student_detail_pending_schedules_added) 
                    else 
                        application.getString(R.string.bulk_schedule_success, result.processedSchedules.size)
                )
            } else {
                val updatedBulkSchedules = bulkSchedules.mapIndexed { index, entry ->
                    val error = result.errors[index]
                    if (error != null) {
                        val errorMsg = when (error) {
                            DomainError.ScheduleConflict -> application.getString(R.string.student_detail_error_schedule_conflict)
                            is DomainError.ConflictingStudent -> application.getString(R.string.student_detail_error_schedule_conflict_student, error.studentName, error.time)
                            else -> application.getString(R.string.student_detail_error_unknown)
                        }
                        entry.copy(error = errorMsg)
                    } else {
                        entry.copy(error = null)
                    }
                }
                state.value = state.value.copy(
                    bulkSchedules = updatedBulkSchedules,
                    bulkScheduleSaving = false,
                    errorMessage = application.getString(R.string.student_detail_error_schedule_conflict)
                )
            }
        }
    }

    override fun saveExtraClass(
        professorId: String,
        studentId: String,
        date: Long,
        startTime: String,
        endTime: String,
        dayOfWeek: DayOfWeek,
        state: MutableStateFlow<StudentDetailState>,
        scope: CoroutineScope
    ) {
        scope.launch {
            state.value = state.value.copy(isLoading = true)

            // Ensure date is handled correctly with timezone
            val localDate = java.time.Instant.ofEpochMilli(date)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()

            val extraClass = ScheduleException(
                id = UUID.randomUUID().toString(),
                studentId = studentId,
                date = date,
                type = ExceptionType.EXTRA,
                originalScheduleId = com.devsusana.hometutorpro.domain.entities.ScheduleType.EXTRA_ID,
                newStartTime = startTime,
                newEndTime = endTime,
                newDayOfWeek = dayOfWeek, // Explicitly set day of week
                reason = "Extra Class"
            )

            when (saveScheduleExceptionUseCase(professorId, studentId, extraClass)) {
                is Result.Success -> {
                    state.value = state.value.copy(
                        isLoading = false,
                        showExtraClassDialog = false,
                        successMessage = application.getString(R.string.student_detail_success_extra_class_added)
                    )
                }
                is Result.Error -> {
                    state.value = state.value.copy(
                        isLoading = false,
                        errorMessage = application.getString(R.string.student_detail_error_save_failed)
                    )
                }
            }
        }
    }

    private suspend fun handleNewStudentSchedule(
        schedule: Schedule, 
        professorId: String,
        state: MutableStateFlow<StudentDetailState>
    ) {
        val currentPending = state.value.pendingSchedules.toMutableList()

        if (checkScheduleConflictUseCase(schedule, currentPending)) {
            state.value = state.value.copy(errorMessage = application.getString(R.string.student_detail_error_schedule_conflict_time_slot))
            return
        }

        try {
            val allSchedules = getAllSchedulesUseCase(professorId).firstOrNull() ?: emptyList()
            if (checkScheduleConflictUseCase(schedule, allSchedules)) {
                state.value = state.value.copy(errorMessage = application.getString(R.string.student_detail_error_schedule_conflict))
                return
            }
        } catch (e: Exception) {
            // Ignore DB check if it fails
        }

        val pendingSchedule = if (schedule.id.isEmpty()) {
            schedule.copy(id = UUID.randomUUID().toString())
        } else {
            schedule
        }

        currentPending.add(pendingSchedule)
        state.value = state.value.copy(
            pendingSchedules = currentPending,
            successMessage = application.getString(R.string.student_detail_success_schedule_saved)
        )
    }

    private fun handleScheduleError(error: DomainError, state: MutableStateFlow<StudentDetailState>) {
        val message = when (error) {
            is DomainError.ConflictingStudent -> application.getString(R.string.student_detail_error_schedule_conflict_student, error.studentName, error.time)
            DomainError.ScheduleConflict -> application.getString(R.string.student_detail_error_schedule_conflict)
            else -> application.getString(R.string.student_detail_error_schedule_failed)
        }
        state.value = state.value.copy(isLoading = false, errorMessage = message)
    }
}
