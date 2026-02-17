package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.BulkScheduleResult
import com.devsusana.hometutorpro.domain.entities.Schedule
import com.devsusana.hometutorpro.domain.repository.StudentRepository
import com.devsusana.hometutorpro.domain.usecases.ICheckScheduleConflictUseCase
import com.devsusana.hometutorpro.domain.usecases.ISaveBulkSchedulesUseCase
import com.devsusana.hometutorpro.domain.usecases.ISaveScheduleUseCase
import javax.inject.Inject

class SaveBulkSchedulesUseCase @Inject constructor(
    private val saveScheduleUseCase: ISaveScheduleUseCase,
    private val checkScheduleConflictUseCase: ICheckScheduleConflictUseCase,
    private val repository: StudentRepository
) : ISaveBulkSchedulesUseCase {

    override suspend fun invoke(
        professorId: String,
        studentId: String,
        schedules: List<Schedule>,
        pendingSchedules: List<Schedule>
    ): BulkScheduleResult {
        val errors = mutableMapOf<Int, DomainError>()
        val processedSchedules = mutableListOf<Schedule>()
        val isNewStudent = studentId.isEmpty() || studentId == "new"

        schedules.forEachIndexed { index, schedule ->
            // 1. Check for conflicts with existing schedules in DB
            val conflict = repository.getConflictingSchedule(
                dayOfWeek = schedule.dayOfWeek.value,
                startTime = schedule.startTime,
                endTime = schedule.endTime,
                scheduleId = schedule.id.takeIf { it.isNotEmpty() }
            )

            if (conflict != null) {
                errors[index] = DomainError.ScheduleConflict
                return@forEachIndexed
            }

            // 2. Check for conflicts within the bulk list or pending list
            val combinedList = processedSchedules + pendingSchedules
            if (checkScheduleConflictUseCase(schedule, combinedList)) {
                errors[index] = DomainError.ScheduleConflict
                return@forEachIndexed
            }

            if (isNewStudent) {
                // For new students, we just collect them to be saved later
                processedSchedules.add(schedule)
            } else {
                // For existing students, we save them immediately
                when (val result = saveScheduleUseCase(professorId, studentId, schedule)) {
                    is Result.Success -> processedSchedules.add(schedule)
                    is Result.Error -> errors[index] = result.error
                }
            }
        }

        return BulkScheduleResult(
            processedSchedules = processedSchedules,
            errors = errors,
            isSuccessful = errors.isEmpty()
        )
    }
}
