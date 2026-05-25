package com.devsusana.hometutorpro.domain.entities

import java.time.DayOfWeek

/**
 * Represents a schedule management action that is pending user confirmation.
 *
 * Sue detects a cancel/reschedule intent from the user, looks up the required
 * data (studentId, scheduleId, target date), and stores the resolved action here.
 * The confirmation question is displayed to the user; the action is only executed
 * once the user confirms.
 */
sealed class SuePendingAction {

    /**
     * The user wants to cancel a specific class occurrence.
     *
     * @param studentName    Human-readable name.
     * @param studentId      Student ID.
     * @param scheduleId     Regular schedule ID.
     * @param date           Epoch-millis of the specific occurrence to cancel.
     * @param startTime      Class start time in "HH:mm" format.
     * @param endTime        Class end time in "HH:mm" format.
     */
    data class CancelClass(
        val studentName: String,
        val studentId: String,
        val scheduleId: String,
        val date: Long,
        val startTime: String,
        val endTime: String
    ) : SuePendingAction()

    /**
     * The user wants to move a specific class occurrence to a new day/time.
     *
     * @param studentName       Human-readable name.
     * @param studentId         Student ID.
     * @param scheduleId        Regular schedule ID.
     * @param originalDate      Epoch-millis of the occurrence being moved.
     * @param originalStartTime Original class start time.
     * @param newDayOfWeek      Target [DayOfWeek], or null if same day.
     * @param newDate           Epoch-millis of the target date.
     * @param newStartTime      New start time in "HH:mm" format.
     * @param newEndTime        New end time in "HH:mm" format.
     */
    data class RescheduleClass(
        val studentName: String,
        val studentId: String,
        val scheduleId: String,
        val originalDate: Long,
        val originalStartTime: String,
        val newDayOfWeek: DayOfWeek?,
        val newDate: Long,
        val newStartTime: String,
        val newEndTime: String
    ) : SuePendingAction()

    /**
     * The user wants to register a payment for a student.
     *
     * @param studentName  Human-readable name for confirmation.
     * @param studentId    Student ID.
     * @param amount       Payment amount.
     * @param paymentType  EFFECTIVE or BIZUM.
     */
    data class RegisterPayment(
        val studentName: String,
        val studentId: String,
        val amount: Double,
        val paymentType: PaymentType
    ) : SuePendingAction()

    /**
     * The user wants to manually add to a student's pending balance.
     *
     * @param studentName  Human-readable name for confirmation.
     * @param studentId    Student ID.
     * @param amount       Amount to add to balance.
     */
    data class AddBalance(
        val studentName: String,
        val studentId: String,
        val amount: Double
    ) : SuePendingAction()
}
