package com.devsusana.hometutorpro.data.mappers

import com.devsusana.hometutorpro.data.models.ScheduleDataModel
import com.devsusana.hometutorpro.domain.entities.Schedule
import java.time.DayOfWeek

/**
 * Mapper for converting between ScheduleDataModel and Schedule domain entity.
 * Handles DayOfWeek enum to String conversion.
 */

fun ScheduleDataModel.toDomain(studentId: String): Schedule {
    return Schedule(
        id = id,
        studentId = studentId,
        dayOfWeek = try {
            DayOfWeek.valueOf(dayOfWeek)
        } catch (e: IllegalArgumentException) {
            DayOfWeek.MONDAY
        },
        startTime = startTime,
        endTime = endTime
    )
}

fun Schedule.toData(): ScheduleDataModel {
    return ScheduleDataModel(
        id = id,
        dayOfWeek = dayOfWeek.name,
        startTime = startTime,
        endTime = endTime
    )
}
