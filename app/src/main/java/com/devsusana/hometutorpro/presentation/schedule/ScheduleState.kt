package com.devsusana.hometutorpro.presentation.schedule

import com.devsusana.hometutorpro.domain.entities.Schedule

data class ScheduleState(
    val schedules: List<Schedule> = emptyList(),
    val isLoading: Boolean = false,
    val error: Any? = null,
    val successMessage: Any? = null,
    val errorMessage: Any? = null
)

data class ScheduleFormState(
    val schedule: Schedule = Schedule(),
    val isLoading: Boolean = false,
    val error: Any? = null,
    val isSaved: Boolean = false
)
