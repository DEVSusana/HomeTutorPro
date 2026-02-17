package com.devsusana.hometutorpro.presentation.student_detail.delegates

import com.devsusana.hometutorpro.domain.entities.Schedule
import com.devsusana.hometutorpro.presentation.student_detail.StudentDetailState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow

interface IStudentScheduleDelegate {
    fun loadSchedules(
        professorId: String,
        studentId: String,
        state: MutableStateFlow<StudentDetailState>,
        scope: CoroutineScope
    )

    fun saveSchedule(
        professorId: String,
        studentId: String,
        schedule: Schedule,
        state: MutableStateFlow<StudentDetailState>,
        scope: CoroutineScope
    )

    fun deleteSchedule(
        professorId: String,
        studentId: String,
        scheduleId: String,
        state: MutableStateFlow<StudentDetailState>,
        scope: CoroutineScope
    )

    fun toggleScheduleCompletion(
        professorId: String,
        scheduleId: String,
        state: MutableStateFlow<StudentDetailState>,
        scope: CoroutineScope
    )

    fun saveBulkSchedules(
        professorId: String,
        studentId: String,
        state: MutableStateFlow<StudentDetailState>,
        scope: CoroutineScope
    )

    fun saveExtraClass(
        professorId: String,
        studentId: String,
        date: Long,
        startTime: String,
        endTime: String,
        state: MutableStateFlow<StudentDetailState>,
        scope: CoroutineScope
    )
}
