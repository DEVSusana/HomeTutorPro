package com.devsusana.hometutorpro.presentation.student_detail

import android.net.Uri
import com.devsusana.hometutorpro.domain.entities.PaymentType
import com.devsusana.hometutorpro.domain.entities.Schedule
import com.devsusana.hometutorpro.domain.entities.ShareMethod
import com.devsusana.hometutorpro.domain.entities.Student
import com.devsusana.hometutorpro.presentation.student_detail.components.BulkScheduleEntry
import java.time.DayOfWeek

sealed interface StudentDetailEvent {
    data class StudentChange(val student: Student) : StudentDetailEvent
    data object SaveStudent : StudentDetailEvent
    data object DeleteStudent : StudentDetailEvent
    
    data class PriceChange(val input: String) : StudentDetailEvent
    data class BalanceChange(val input: String) : StudentDetailEvent
    data object ToggleBalanceEdit : StudentDetailEvent
    
    data class RegisterPayment(val amount: Double, val type: PaymentType) : StudentDetailEvent
    data class StartClass(val durationMinutes: Int) : StudentDetailEvent
    
    data class SaveSchedule(val schedule: Schedule) : StudentDetailEvent
    data class DeleteSchedule(val scheduleId: String) : StudentDetailEvent
    
    data object ClearFeedback : StudentDetailEvent
    
    data class FileSelected(val uri: Uri, val name: String, val type: String, val size: Long) : StudentDetailEvent
    data class ShareResource(val method: ShareMethod, val fileType: String, val size: Long) : StudentDetailEvent
    data class DeleteSharedResource(val resourceId: String) : StudentDetailEvent
    data object DismissShareDialog : StudentDetailEvent
    data class ShareNotesChange(val notes: String) : StudentDetailEvent
    
    data class TabChange(val index: Int) : StudentDetailEvent
    data object ContinueToNextStep : StudentDetailEvent
    
    data object ToggleBulkScheduleMode : StudentDetailEvent
    data class BulkSchedulesChange(val schedules: List<BulkScheduleEntry>) : StudentDetailEvent
    data object SaveBulkSchedules : StudentDetailEvent
    
    data object ShowExtraClassDialog : StudentDetailEvent
    data object HideExtraClassDialog : StudentDetailEvent
    data class SaveExtraClass(val date: Long, val startTime: String, val endTime: String, val dayOfWeek: DayOfWeek) : StudentDetailEvent
}
