package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.entities.SueOperationResult
import com.devsusana.hometutorpro.domain.entities.SuePendingAction
import com.devsusana.hometutorpro.domain.usecases.IExecuteSuePendingActionUseCase
import javax.inject.Inject

/**
 * Concrete implementation of [IExecuteSuePendingActionUseCase], delegating to
 * domain tools [ScheduleTools] and [StudentTools].
 */
class ExecuteSuePendingActionUseCase @Inject constructor(
    private val scheduleTools: ScheduleTools,
    private val studentTools: StudentTools
) : IExecuteSuePendingActionUseCase {

    override suspend operator fun invoke(action: SuePendingAction): SueOperationResult.Execute {
        return when (action) {
            is SuePendingAction.CancelClass -> scheduleTools.executeCancelAction(action)
            is SuePendingAction.RescheduleClass -> scheduleTools.executeRescheduleAction(action)
            is SuePendingAction.RegisterPayment -> studentTools.executeRegisterPayment(action)
            is SuePendingAction.AddBalance -> studentTools.executeAddBalance(action)
            is SuePendingAction.StartClass -> studentTools.executeStartClass(action)
            is SuePendingAction.CreateStudent -> studentTools.executeCreateStudent(action)
            is SuePendingAction.DeleteStudent -> studentTools.executeDeleteStudent(action)
            is SuePendingAction.CreateSchedule -> scheduleTools.executeCreateSchedule(action)
            is SuePendingAction.DeleteSchedule -> scheduleTools.executeDeleteSchedule(action)
            is SuePendingAction.AddExtraClass -> scheduleTools.executeAddExtraClass(action)
            is SuePendingAction.UpdateStudentNotes -> studentTools.executeUpdateNotesAction(action)
            is SuePendingAction.UpdateStudentAddress -> studentTools.executeUpdateAddressAction(action)
            is SuePendingAction.UpdateStudentPrice -> studentTools.executeUpdatePriceAction(action)
            is SuePendingAction.UpdateStudentPhone -> studentTools.executeUpdatePhoneAction(action)
            is SuePendingAction.UpdateStudentCourse -> studentTools.executeUpdateCourseAction(action)
        }
    }

    override suspend fun getConflictFreeSlots(dayOfWeek: Int?): SueOperationResult {
        return scheduleTools.getFreeSlots(dayOfWeek = dayOfWeek)
    }
}
