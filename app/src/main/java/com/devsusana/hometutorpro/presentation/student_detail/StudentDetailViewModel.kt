package com.devsusana.hometutorpro.presentation.student_detail

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.Schedule
import com.devsusana.hometutorpro.domain.entities.Student
import com.devsusana.hometutorpro.domain.usecases.*
import com.devsusana.hometutorpro.presentation.student_detail.components.BulkScheduleEntry
import com.devsusana.hometutorpro.presentation.student_detail.delegates.IStudentFinanceDelegate
import com.devsusana.hometutorpro.presentation.student_detail.delegates.IStudentResourceDelegate
import com.devsusana.hometutorpro.presentation.student_detail.delegates.IStudentScheduleDelegate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getStudentByIdUseCase: IGetStudentByIdUseCase,
    private val saveStudentUseCase: ISaveStudentUseCase,
    private val deleteStudentUseCase: IDeleteStudentUseCase,
    private val getCurrentUserUseCase: IGetCurrentUserUseCase,
    private val validateStudentUseCase: IValidateStudentUseCase,
    private val saveScheduleUseCase: ISaveScheduleUseCase,
    private val financeDelegate: IStudentFinanceDelegate,
    private val scheduleDelegate: IStudentScheduleDelegate,
    private val resourceDelegate: IStudentResourceDelegate,
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
                                    scheduleDelegate.loadSchedules(user.uid, studentId, _state, viewModelScope)
                                }
                                if (_state.value.sharedResources.isEmpty()) {
                                    resourceDelegate.loadSharedResources(user.uid, studentId, _state, viewModelScope)
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
        val student = _state.value.student ?: return
        when (event) {
            is StudentDetailEvent.StudentChange -> _state.value = _state.value.copy(student = event.student)
            StudentDetailEvent.SaveStudent -> saveStudent()
            StudentDetailEvent.DeleteStudent -> deleteStudent()
            is StudentDetailEvent.PriceChange -> onPriceChange(event.input)
            is StudentDetailEvent.BalanceChange -> _state.value = _state.value.copy(balanceInput = event.input)
            StudentDetailEvent.ToggleBalanceEdit -> onBalanceEditToggle()
            is StudentDetailEvent.RegisterPayment -> financeDelegate.registerPayment(student.professorId, student.id, event.amount, event.type, _state, viewModelScope)
            is StudentDetailEvent.StartClass -> financeDelegate.startClass(student.professorId, student.id, event.durationMinutes, student.pricePerHour, _state, viewModelScope)
            is StudentDetailEvent.SaveSchedule -> scheduleDelegate.saveSchedule(student.professorId, student.id, event.schedule, _state, viewModelScope)
            is StudentDetailEvent.DeleteSchedule -> scheduleDelegate.deleteSchedule(student.professorId, student.id, event.scheduleId, _state, viewModelScope)
            StudentDetailEvent.ClearFeedback -> _state.value = _state.value.copy(successMessage = null, errorMessage = null)
            is StudentDetailEvent.FileSelected -> onFileSelected(event)
            is StudentDetailEvent.ShareResource -> resourceDelegate.shareResource(student.professorId, student.id, event.fileType, event.size, event.method, _state, viewModelScope)
            is StudentDetailEvent.DeleteSharedResource -> resourceDelegate.deleteSharedResource(student.professorId, event.resourceId, _state, viewModelScope)
            StudentDetailEvent.DismissShareDialog -> onShareDialogDismiss()
            is StudentDetailEvent.ShareNotesChange -> _state.value = _state.value.copy(shareNotes = event.notes)
            is StudentDetailEvent.TabChange -> _state.value = _state.value.copy(currentTab = event.index)
            StudentDetailEvent.ContinueToNextStep -> continueToNextStep()
            StudentDetailEvent.ToggleBulkScheduleMode -> onBulkScheduleModeToggle()
            is StudentDetailEvent.BulkSchedulesChange -> _state.value = _state.value.copy(bulkSchedules = event.schedules)
            StudentDetailEvent.SaveBulkSchedules -> scheduleDelegate.saveBulkSchedules(student.professorId, student.id, _state, viewModelScope)
            StudentDetailEvent.ShowExtraClassDialog -> _state.value = _state.value.copy(showExtraClassDialog = true)
            StudentDetailEvent.HideExtraClassDialog -> _state.value = _state.value.copy(showExtraClassDialog = false)
            is StudentDetailEvent.SaveExtraClass -> scheduleDelegate.saveExtraClass(student.professorId, student.id, event.date, event.startTime, event.endTime, _state, viewModelScope)
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
            var student = state.student ?: return@launch

            if (state.isBalanceEditable) {
                val newBalance = state.balanceInput.toDoubleOrNull()
                if (newBalance == null) {
                    _state.value = _state.value.copy(errorMessage = application.getString(R.string.student_detail_error_invalid_balance))
                    return@launch
                }
                student = student.copy(pendingBalance = newBalance)
            }

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

        _state.value = _state.value.copy(
            student = student,
            isStudentSaved = true,
            isLoading = false,
            successMessage = if (hasConflicts) null else application.getString(R.string.student_detail_success_student_saved),
            errorMessage = if (hasConflicts) application.getString(R.string.student_detail_error_save_schedules_failed) else null,
            pendingSchedules = failedSchedules
        )
    }

    private fun deleteStudent() {
        viewModelScope.launch {
            val student = _state.value.student ?: return@launch
            if (student.id.isNotEmpty()) {
                _state.value = _state.value.copy(isLoading = true)
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
}
