package com.devsusana.hometutorpro.presentation.student_detail

import android.net.Uri
import com.devsusana.hometutorpro.domain.entities.SharedResource
import com.devsusana.hometutorpro.domain.entities.Student

data class StudentDetailState(
    val student: Student? = null,
    val isLoading: Boolean = false,
    val error: Any? = null,
    val isStudentSaved: Boolean = false,
    val isPaymentRegistered: Boolean = false,
    val isStudentDeleted: Boolean = false,
    val successMessage: Any? = null,
    val errorMessage: Any? = null,
    val isBalanceEditable: Boolean = false,
    val balanceInput: String = "",
    val priceInput: String = "",

    // Shared resources fields
    val sharedResources: List<SharedResource> = emptyList(),
    val showShareDialog: Boolean = false,
    val selectedFileUri: Uri? = null,
    val selectedFileName: String = "",
    val shareNotes: String = "",
    
    // Tab navigation for new students
    val currentTab: Int = 0,
    
    // Bulk schedule creation
    val bulkSchedules: List<com.devsusana.hometutorpro.presentation.student_detail.components.BulkScheduleEntry> = emptyList(),
    val isBulkScheduleMode: Boolean = false,
    val bulkScheduleSaving: Boolean = false,
    
    // Pending schedules for new students
    val pendingSchedules: List<com.devsusana.hometutorpro.domain.entities.Schedule> = emptyList(),
    
    // Saved schedules for existing students
    val schedules: List<com.devsusana.hometutorpro.domain.entities.Schedule> = emptyList(),

    // Extra Class Dialog
    val showExtraClassDialog: Boolean = false
)
