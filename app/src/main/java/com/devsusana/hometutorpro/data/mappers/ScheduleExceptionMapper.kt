package com.devsusana.hometutorpro.data.mappers

import com.devsusana.hometutorpro.data.models.ScheduleExceptionDataModel
import com.devsusana.hometutorpro.domain.entities.ExceptionType
import com.devsusana.hometutorpro.domain.entities.ScheduleException
import java.time.DayOfWeek

/**
 * Mapper for converting between ScheduleExceptionDataModel and ScheduleException domain entity.
 * Handles ExceptionType and DayOfWeek enum to String conversions.
 */

fun ScheduleExceptionDataModel.toDomain(): ScheduleException {
    return ScheduleException(
        id = id,
        studentId = studentId,
        date = date,
        type = try {
            ExceptionType.valueOf(type)
        } catch (e: IllegalArgumentException) {
            ExceptionType.CANCELLED
        },
        originalScheduleId = originalScheduleId,
        newStartTime = newStartTime,
        newEndTime = newEndTime,
        newDayOfWeek = newDayOfWeek?.let {
            try {
                DayOfWeek.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        },
        reason = reason
    )
}

fun ScheduleException.toData(): ScheduleExceptionDataModel {
    return ScheduleExceptionDataModel(
        id = id,
        studentId = studentId,
        date = date,
        type = type.name,
        originalScheduleId = originalScheduleId,
        newStartTime = newStartTime,
        newEndTime = newEndTime,
        newDayOfWeek = newDayOfWeek?.name,
        reason = reason
    )
}
