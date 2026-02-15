package com.devsusana.hometutorpro.presentation.student_detail

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.ExceptionType
import com.devsusana.hometutorpro.domain.entities.Schedule
import com.devsusana.hometutorpro.domain.entities.ScheduleException
import com.devsusana.hometutorpro.domain.entities.SharedResource
import com.devsusana.hometutorpro.domain.entities.Student
import com.devsusana.hometutorpro.domain.usecases.ICheckScheduleConflictUseCase
import com.devsusana.hometutorpro.domain.usecases.IDeleteScheduleUseCase
import com.devsusana.hometutorpro.domain.usecases.IDeleteSharedResourceUseCase
import com.devsusana.hometutorpro.domain.usecases.IDeleteStudentUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetAllSchedulesUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetCurrentUserUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetSchedulesUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetSharedResourcesUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetStudentByIdUseCase
import com.devsusana.hometutorpro.domain.usecases.IRegisterPaymentUseCase
import com.devsusana.hometutorpro.domain.usecases.ISaveScheduleExceptionUseCase
import com.devsusana.hometutorpro.domain.usecases.ISaveScheduleUseCase
import com.devsusana.hometutorpro.domain.usecases.ISaveSharedResourceUseCase
import com.devsusana.hometutorpro.domain.usecases.ISaveStudentUseCase
import com.devsusana.hometutorpro.domain.usecases.IValidateStudentUseCase
import com.devsusana.hometutorpro.presentation.student_detail.components.BulkScheduleEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class StudentDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getStudentByIdUseCase: IGetStudentByIdUseCase,
    private val saveStudentUseCase: ISaveStudentUseCase,
    private val deleteStudentUseCase: IDeleteStudentUseCase,
    private val registerPaymentUseCase: IRegisterPaymentUseCase,
    private val getCurrentUserUseCase: IGetCurrentUserUseCase,
    private val saveScheduleUseCase: ISaveScheduleUseCase,
    private val deleteScheduleUseCase: IDeleteScheduleUseCase,
    private val getSharedResourcesUseCase: IGetSharedResourcesUseCase,
    private val saveSharedResourceUseCase: ISaveSharedResourceUseCase,
    private val deleteSharedResourceUseCase: IDeleteSharedResourceUseCase,
    private val getAllSchedulesUseCase: IGetAllSchedulesUseCase,
    private val getSchedulesUseCase: IGetSchedulesUseCase,
    private val saveScheduleExceptionUseCase: ISaveScheduleExceptionUseCase,
    private val validateStudentUseCase: IValidateStudentUseCase,
    private val checkScheduleConflictUseCase: ICheckScheduleConflictUseCase,
    private val application: Application
) : ViewModel() {

    private val _state = MutableStateFlow(StudentDetailState())
    val state: StateFlow<StudentDetailState> = _state.asStateFlow()

    private val studentId: String? = savedStateHandle["studentId"]

    init {
        initialize()
    }

    private fun initialize() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            getCurrentUserUseCase().collect { user ->
                if (user != null) {
                    if (studentId != null && studentId != "new") {
                        getStudentByIdUseCase(user.uid, studentId).collect { student ->
                            if (student != null) {
                                _state.value = _state.value.copy(
                                    student = student,
                                    isLoading = false,
                                    priceInput = if (student.pricePerHour > 0) student.pricePerHour.toString() else ""
                                )
                                // Load schedules and resources only once after student is loaded
                                if (_state.value.schedules.isEmpty()) {
                                    loadSchedules(user.uid, studentId)
                                }
                                if (_state.value.sharedResources.isEmpty()) {
                                    loadSharedResources(user.uid, studentId)
                                }
                            } else {
                                _state.value = _state.value.copy(isStudentDeleted = true, isLoading = false)
                            }
                        }
                    } else {
                        _state.value = _state.value.copy(student = Student(professorId = user.uid), isLoading = false)
                    }
                }
            }
        }
    }

    fun onEvent(event: StudentDetailEvent) {
        when (event) {
            is StudentDetailEvent.StudentChange -> _state.value = _state.value.copy(student = event.student)
            StudentDetailEvent.SaveStudent -> saveStudent()
            StudentDetailEvent.DeleteStudent -> deleteStudent()
            is StudentDetailEvent.PriceChange -> onPriceChange(event.input)
            is StudentDetailEvent.BalanceChange -> _state.value = _state.value.copy(balanceInput = event.input)
            StudentDetailEvent.ToggleBalanceEdit -> onBalanceEditToggle()
            is StudentDetailEvent.RegisterPayment -> registerPayment(event.amount, event.type)
            is StudentDetailEvent.StartClass -> startClass(event.durationMinutes)
            is StudentDetailEvent.SaveSchedule -> saveSchedule(event.schedule)
            is StudentDetailEvent.DeleteSchedule -> deleteSchedule(event.scheduleId)
            StudentDetailEvent.ClearFeedback -> _state.value = _state.value.copy(successMessage = null, errorMessage = null)
            is StudentDetailEvent.FileSelected -> onFileSelected(event)
            is StudentDetailEvent.ShareResource -> shareResource(event)
            is StudentDetailEvent.DeleteSharedResource -> deleteSharedResource(event.resourceId)
            StudentDetailEvent.DismissShareDialog -> onShareDialogDismiss()
            is StudentDetailEvent.ShareNotesChange -> _state.value = _state.value.copy(shareNotes = event.notes)
            is StudentDetailEvent.TabChange -> _state.value = _state.value.copy(currentTab = event.index)
            StudentDetailEvent.ContinueToNextStep -> continueToNextStep()
            StudentDetailEvent.ToggleBulkScheduleMode -> onBulkScheduleModeToggle()
            is StudentDetailEvent.BulkSchedulesChange -> _state.value = _state.value.copy(bulkSchedules = event.schedules)
            StudentDetailEvent.SaveBulkSchedules -> saveBulkSchedules()
            StudentDetailEvent.ShowExtraClassDialog -> _state.value = _state.value.copy(showExtraClassDialog = true)
            StudentDetailEvent.HideExtraClassDialog -> _state.value = _state.value.copy(showExtraClassDialog = false)
            is StudentDetailEvent.SaveExtraClass -> saveExtraClass(event.date, event.startTime, event.endTime)
        }
    }

    private fun onPriceChange(input: String) {
        val regex = Regex("^\\d*\\.?\\d*$")
        if (!regex.matches(input) && input.isNotEmpty()) return

        val student = _state.value.student ?: return
        val price = input.toDoubleOrNull() ?: 0.0

        _state.value = _state.value.copy(
            priceInput = input,
            student = student.copy(pricePerHour = price)
        )
    }

    private fun onBalanceEditToggle() {
        val isEditing = !_state.value.isBalanceEditable
        val currentBalance = _state.value.student?.pendingBalance ?: 0.0
        _state.value = _state.value.copy(
            isBalanceEditable = isEditing,
            balanceInput = if (isEditing) currentBalance.toString() else ""
        )
    }

    private fun saveStudent() {
        viewModelScope.launch {
            val state = _state.value
            var student = state.student

            if (student == null) {
                _state.value = _state.value.copy(errorMessage = application.getString(R.string.student_detail_error_unexpected))
                return@launch
            }

            if (state.isBalanceEditable) {
                val newBalance = state.balanceInput.toDoubleOrNull()
                if (newBalance == null) {
                    _state.value = _state.value.copy(errorMessage = application.getString(R.string.student_detail_error_invalid_balance))
                    return@launch
                }
                student = student.copy(pendingBalance = newBalance)
            }

            // Domain Validation
            when (val validationResult = validateStudentUseCase(student)) {
                is Result.Error -> {
                    val errorMsg = when (validationResult.error) {
                        DomainError.StudentNameRequired -> application.getString(R.string.student_detail_error_name_required)
                        DomainError.InvalidPrice -> application.getString(R.string.student_detail_error_invalid_price)
                        DomainError.InvalidBalance -> application.getString(R.string.student_detail_error_invalid_balance)
                        else -> application.getString(R.string.student_detail_error_unexpected)
                    }
                    _state.value = _state.value.copy(errorMessage = errorMsg)
                    return@launch
                }
                is Result.Success -> Unit
            }

            _state.value = _state.value.copy(isLoading = true)

            when (val result = saveStudentUseCase(student.professorId, student)) {
                is Result.Success -> {
                    val newId = result.data
                    val updatedStudent = student.copy(id = newId)
                    savePendingSchedules(updatedStudent)
                }
                is Result.Error<*> -> {
                    _state.value = _state.value.copy(
                        error = application.getString(R.string.student_detail_error_unexpected),
                        isLoading = false,
                        errorMessage = application.getString(R.string.student_detail_error_save_failed)
                    )
                }
            }
        }
    }

    private suspend fun savePendingSchedules(student: Student) {
        val pendingSchedules = _state.value.pendingSchedules
        val failedSchedules = mutableListOf<Schedule>()
        var hasConflicts = false

        if (pendingSchedules.isNotEmpty()) {
            pendingSchedules.forEach { schedule ->
                when (saveScheduleUseCase(student.professorId, student.id, schedule)) {
                    is Result.Success -> {}
                    is Result.Error -> {
                        hasConflicts = true
                        failedSchedules.add(schedule)
                    }
                }
            }
        }

        if (hasConflicts) {
            _state.value = _state.value.copy(
                student = student,
                isStudentSaved = true,
                isLoading = false,
                errorMessage = application.getString(R.string.student_detail_error_save_schedules_failed),
                pendingSchedules = failedSchedules
            )
        } else {
            _state.value = _state.value.copy(
                student = student,
                isStudentSaved = true,
                isLoading = false,
                successMessage = application.getString(R.string.student_detail_success_student_saved),
                pendingSchedules = emptyList()
            )
        }
    }

    private fun deleteStudent() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val student = _state.value.student
            if (student != null && student.id.isNotEmpty()) {
                when (deleteStudentUseCase(student.professorId, student.id)) {
                    is Result.Success<*> -> {
                        _state.value = _state.value.copy(
                            isStudentDeleted = true,
                            isLoading = false,
                            successMessage = application.getString(R.string.student_detail_success_student_deleted)
                        )
                    }
                    is Result.Error<*> -> {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            errorMessage = application.getString(R.string.student_detail_error_delete_failed)
                        )
                    }
                }
            }
        }
    }

    private fun registerPayment(amount: Double, paymentType: com.devsusana.hometutorpro.domain.entities.PaymentType) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val student = _state.value.student
            if (student != null) {
                when (registerPaymentUseCase(student.professorId, student.id, amount, paymentType)) {
                    is Result.Success<*> -> {
                        val newBalance = student.pendingBalance - amount
                        val updatedStudent = student.copy(
                            pendingBalance = newBalance,
                            lastPaymentDate = System.currentTimeMillis()
                        )
                        _state.value = _state.value.copy(
                            student = updatedStudent,
                            isPaymentRegistered = true,
                            isLoading = false,
                            successMessage = application.getString(R.string.student_detail_success_payment_registered, amount)
                        )
                    }
                    is Result.Error<*> -> {
                        _state.value = _state.value.copy(
                            error = application.getString(R.string.student_detail_error_unexpected),
                            isLoading = false,
                            errorMessage = application.getString(R.string.student_detail_error_payment_failed)
                        )
                    }
                }
            }
        }
    }

    private fun startClass(durationMinutes: Int) {
        viewModelScope.launch {
            val student = _state.value.student ?: return@launch
            _state.value = _state.value.copy(isLoading = true)

            val priceToAdd = (durationMinutes / 60.0) * student.pricePerHour
            val newBalance = student.pendingBalance + priceToAdd
            val updatedStudent = student.copy(pendingBalance = newBalance)

            when (saveStudentUseCase(student.professorId, updatedStudent)) {
                is Result.Success<*> -> {
                    _state.value = _state.value.copy(
                        student = updatedStudent,
                        isLoading = false,
                        successMessage = application.getString(R.string.student_detail_success_class_started, priceToAdd)
                    )
                }
                is Result.Error<*> -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = application.getString(R.string.student_detail_error_update_balance_failed)
                    )
                }
            }
        }
    }

    private fun saveSchedule(schedule: Schedule) {
        viewModelScope.launch {
            val student = _state.value.student ?: return@launch

            if (student.id.isEmpty() || student.id == "new") {
                handleNewStudentSchedule(schedule, student)
                return@launch
            }

            _state.value = _state.value.copy(isLoading = true)

            when (val result = saveScheduleUseCase(student.professorId, student.id, schedule)) {
                is Result.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        successMessage = application.getString(R.string.student_detail_success_schedule_saved)
                    )
                }
                is Result.Error -> {
                    handleScheduleError(result.error)
                }
            }
        }
    }

    private suspend fun handleNewStudentSchedule(schedule: Schedule, student: Student) {
        val currentPending = _state.value.pendingSchedules.toMutableList()

        if (checkScheduleConflictUseCase(schedule, currentPending)) {
            _state.value = _state.value.copy(errorMessage = application.getString(R.string.student_detail_error_schedule_conflict_time_slot))
            return
        }

        try {
            val allSchedules = getAllSchedulesUseCase(student.professorId).firstOrNull() ?: emptyList()
            if (checkScheduleConflictUseCase(schedule, allSchedules)) {
                _state.value = _state.value.copy(errorMessage = application.getString(R.string.student_detail_error_schedule_conflict))
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
        _state.value = _state.value.copy(
            pendingSchedules = currentPending,
            successMessage = application.getString(R.string.student_detail_success_schedule_saved)
        )
    }

    private fun handleScheduleError(error: DomainError) {
        val errorMsg = when (error) {
            is DomainError.ConflictingStudent ->
                application.getString(R.string.student_detail_error_schedule_conflict_student, error.studentName, error.time)
            DomainError.ScheduleConflict ->
                application.getString(R.string.student_detail_error_schedule_conflict)
            else -> application.getString(R.string.student_detail_error_schedule_failed)
        }
        _state.value = _state.value.copy(
            isLoading = false,
            errorMessage = errorMsg
        )
    }

    private fun deleteSchedule(scheduleId: String) {
        viewModelScope.launch {
            val student = _state.value.student ?: return@launch

            if (student.id.isEmpty() || student.id == "new") {
                val list = _state.value.pendingSchedules.filterNot { it.id == scheduleId }
                _state.value = _state.value.copy(
                    pendingSchedules = list,
                    successMessage = application.getString(R.string.student_detail_success_schedule_deleted)
                )
                return@launch
            }

            _state.value = _state.value.copy(isLoading = true)

            when (deleteScheduleUseCase(student.professorId, student.id, scheduleId)) {
                is Result.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        successMessage = application.getString(R.string.student_detail_success_schedule_deleted)
                    )
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = application.getString(R.string.student_detail_error_schedule_delete_failed)
                    )
                }
            }
        }
    }

    private fun loadSchedules(professorId: String, studentId: String) {
        viewModelScope.launch {
            getSchedulesUseCase(professorId, studentId).collect { schedules ->
                _state.value = _state.value.copy(schedules = schedules)
            }
        }
    }

    private fun loadSharedResources(professorId: String, studentId: String) {
        viewModelScope.launch {
            getSharedResourcesUseCase(professorId, studentId).collect { resources ->
                _state.value = _state.value.copy(sharedResources = resources, isLoading = false)
            }
        }
    }

    private fun onFileSelected(event: StudentDetailEvent.FileSelected) {
        _state.value = _state.value.copy(
            selectedFileUri = event.uri,
            selectedFileName = event.name,
            showShareDialog = true
        )
    }

    private fun onShareDialogDismiss() {
        _state.value = _state.value.copy(
            showShareDialog = false,
            selectedFileUri = null,
            selectedFileName = "",
            shareNotes = ""
        )
    }

    private fun shareResource(event: StudentDetailEvent.ShareResource) {
        viewModelScope.launch {
            val student = _state.value.student ?: return@launch
            val fileName = _state.value.selectedFileName
            val notes = _state.value.shareNotes

            if (fileName.isBlank()) {
                _state.value = _state.value.copy(errorMessage = application.getString(R.string.student_detail_error_no_file_selected))
                return@launch
            }

            _state.value = _state.value.copy(isLoading = true)

            val sharedResource = SharedResource(
                studentId = student.id,
                fileName = fileName,
                fileType = event.fileType,
                fileSizeBytes = event.size,
                sharedVia = event.method,
                notes = notes
            )

            when (saveSharedResourceUseCase(student.professorId, sharedResource)) {
                is Result.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        showShareDialog = false,
                        selectedFileUri = null,
                        selectedFileName = "",
                        shareNotes = "",
                        successMessage = application.getString(R.string.student_detail_success_resource_shared)
                    )
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = application.getString(R.string.student_detail_error_share_failed)
                    )
                }
            }
        }
    }

    private fun deleteSharedResource(resourceId: String) {
        viewModelScope.launch {
            val student = _state.value.student ?: return@launch
            _state.value = _state.value.copy(isLoading = true)

            when (deleteSharedResourceUseCase(student.professorId, resourceId)) {
                is Result.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        successMessage = application.getString(R.string.student_detail_success_resource_deleted)
                    )
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = application.getString(R.string.student_detail_error_delete_resource_failed)
                    )
                }
            }
        }
    }

    private fun onBulkScheduleModeToggle() {
        val isEntering = !_state.value.isBulkScheduleMode
        _state.value = _state.value.copy(
            isBulkScheduleMode = isEntering,
            bulkSchedules = if (isEntering) listOf(BulkScheduleEntry(id = 1)) else emptyList()
        )
    }

    private fun continueToNextStep() {
        val currentState = _state.value
        val student = currentState.student ?: return

        when (currentState.currentTab) {
            0 -> {
                if (student.name.isBlank()) {
                    _state.value = currentState.copy(errorMessage = application.getString(R.string.student_detail_error_name_required))
                    return
                }
                _state.value = currentState.copy(currentTab = 1)
            }
            1 -> {
                _state.value = currentState.copy(currentTab = 2)
            }
        }
    }

    private fun saveBulkSchedules() {
        viewModelScope.launch {
            val student = _state.value.student ?: return@launch
            val bulkSchedules = _state.value.bulkSchedules

            if (bulkSchedules.isEmpty()) {
                _state.value = _state.value.copy(errorMessage = application.getString(R.string.bulk_schedule_error_empty))
                return@launch
            }

            _state.value = _state.value.copy(bulkScheduleSaving = true)

            val schedules = mutableListOf<Schedule>()
            val updatedBulkSchedules = mutableListOf<BulkScheduleEntry>()
            var hasErrors = false

            bulkSchedules.forEachIndexed { index, entry ->
                val timeRegex = Regex("^([01]?[0-9]|2[0-3]):[0-5][0-9]$")
                if (!timeRegex.matches(entry.startTime) || !timeRegex.matches(entry.endTime)) {
                    updatedBulkSchedules.add(entry.copy(error = application.getString(R.string.bulk_schedule_error_invalid_time, index + 1)))
                    hasErrors = true
                } else {
                    val schedule = Schedule(
                        id = UUID.randomUUID().toString(),
                        studentId = student.id,
                        dayOfWeek = entry.dayOfWeek,
                        startTime = entry.startTime,
                        endTime = entry.endTime
                    )
                    schedules.add(schedule)
                    updatedBulkSchedules.add(entry.copy(error = null))
                }
            }

            if (hasErrors) {
                _state.value = _state.value.copy(bulkSchedules = updatedBulkSchedules, bulkScheduleSaving = false)
                return@launch
            }

            // Conflict Checking using Domain Use Case
            try {
                val dbSchedules = getAllSchedulesUseCase(student.professorId).firstOrNull() ?: emptyList()
                val pendingSchedules = _state.value.pendingSchedules

                var conflictFound = false
                val validatedBulkSchedules = updatedBulkSchedules.toMutableList()

                schedules.forEachIndexed { index, newSchedule ->
                    val dbConflict = checkScheduleConflictUseCase(newSchedule, dbSchedules)
                    val pendingConflict = checkScheduleConflictUseCase(newSchedule, pendingSchedules)

                    if (dbConflict || pendingConflict) {
                        conflictFound = true
                        validatedBulkSchedules[index] = validatedBulkSchedules[index].copy(error = "Conflict")
                    }
                }

                if (conflictFound) {
                    _state.value = _state.value.copy(
                        bulkSchedules = validatedBulkSchedules,
                        bulkScheduleSaving = false,
                        errorMessage = application.getString(R.string.student_detail_error_schedule_conflict)
                    )
                    return@launch
                }

            } catch (e: Exception) {
            }

            val isNewStudent = student.id.isEmpty() || student.id == "new"
            if (isNewStudent) {
                _state.value = _state.value.copy(
                    bulkScheduleSaving = false,
                    isBulkScheduleMode = false,
                    bulkSchedules = emptyList(),
                    pendingSchedules = _state.value.pendingSchedules + schedules,
                    successMessage = application.getString(R.string.student_detail_pending_schedules_added)
                )
                return@launch
            }

            // Save to DB
            var savedCount = 0
            for ((index, schedule) in schedules.withIndex()) {
                when (val result = saveScheduleUseCase(student.professorId, student.id, schedule)) {
                    is Result.Success -> savedCount++
                    is Result.Error -> {
                        val errorMsg = when (val error = result.error) {
                            is DomainError.ConflictingStudent -> application.getString(R.string.student_detail_error_schedule_conflict_student, error.studentName, error.time)
                            DomainError.ScheduleConflict -> application.getString(R.string.student_detail_error_schedule_conflict_time_slot)
                            else -> application.getString(R.string.student_detail_error_unknown)
                        }
                        val updatedEntry = bulkSchedules[index].copy(error = "Error")
                        val newBulkSchedules = bulkSchedules.toMutableList()
                        newBulkSchedules[index] = updatedEntry

                        _state.value = _state.value.copy(
                            bulkSchedules = newBulkSchedules,
                            bulkScheduleSaving = false,
                            errorMessage = errorMsg
                        )
                        return@launch
                    }
                }
            }

            _state.value = _state.value.copy(
                bulkScheduleSaving = false,
                isBulkScheduleMode = false,
                bulkSchedules = emptyList(),
                successMessage = application.getString(R.string.bulk_schedule_success, savedCount)
            )
        }
    }

    private fun saveExtraClass(date: Long, startTime: String, endTime: String) {
        viewModelScope.launch {
            val student = _state.value.student ?: return@launch
            _state.value = _state.value.copy(isLoading = true)

            val extraClass = ScheduleException(
                id = UUID.randomUUID().toString(),
                studentId = student.id,
                date = date,
                type = ExceptionType.EXTRA,
                originalScheduleId = "EXTRA",
                newStartTime = startTime,
                newEndTime = endTime,
                reason = "Extra Class"
            )

            when (saveScheduleExceptionUseCase(student.professorId, student.id, extraClass)) {
                is Result.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        showExtraClassDialog = false,
                        successMessage = application.getString(R.string.student_detail_success_extra_class_added)
                    )
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = application.getString(R.string.student_detail_error_save_failed)
                    )
                }
            }
        }
    }
}
