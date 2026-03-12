package com.devsusana.hometutorpro.presentation.dashboard

import androidx.compose.runtime.Immutable
import com.devsusana.hometutorpro.presentation.weekly_schedule.WeeklyScheduleItem

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
