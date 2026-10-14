package com.devsusana.hometutorpro.domain.usecases.implementations.sue

import com.devsusana.hometutorpro.domain.entities.SuePendingAction
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enumeration of conversational slots that can be gathered across multi-turn interactions.
 */
enum class SlotType {
    STUDENT_NAME,
    DAY_OF_WEEK,
    TIME,
    AMOUNT,
    DURATION,
    PRICE,
    SUBJECTS,
    COURSE,
    NOTES
}

/**
 * Represents an action that is awaiting one or more missing parameters from the user.
 */
data class IncompleteIntent(
    val type: IntentType,
    val missingSlot: SlotType,
    val filledSlots: Map<SlotType, Any> = emptyMap()
)

/**
 * Manages volatile conversation state across multi-turn interactions for the Sue AI agent.
 */
@Singleton
class SueConversationContext @Inject constructor() {

    var lastMentionedStudentName: String? = null
    var lastMentionedDayOfWeek: Int? = null
    var lastMentionedTime: String? = null
    var lastMentionedAmount: Double? = null
    var lastMentionedDuration: Int? = null
    var lastMentionedPrice: Double? = null
    var lastMentionedSubjects: String? = null
    var lastMentionedCourse: String? = null
    var lastActiveIntentType: IntentType? = null
    var activeIncompleteIntent: IncompleteIntent? = null

    /**
     * Records an intent awaiting a specific slot from the user.
     */
    fun setAwaitingSlot(type: IntentType, missingSlot: SlotType) {
        lastActiveIntentType = type
        val currentFilled = mutableMapOf<SlotType, Any>()
        lastMentionedStudentName?.let { currentFilled[SlotType.STUDENT_NAME] = it }
        lastMentionedDayOfWeek?.let { currentFilled[SlotType.DAY_OF_WEEK] = it }
        lastMentionedTime?.let { currentFilled[SlotType.TIME] = it }
        lastMentionedAmount?.let { currentFilled[SlotType.AMOUNT] = it }
        lastMentionedDuration?.let { currentFilled[SlotType.DURATION] = it }
        lastMentionedPrice?.let { currentFilled[SlotType.PRICE] = it }
        lastMentionedSubjects?.let { currentFilled[SlotType.SUBJECTS] = it }
        lastMentionedCourse?.let { currentFilled[SlotType.COURSE] = it }
        activeIncompleteIntent = IncompleteIntent(type, missingSlot, currentFilled)
    }

    /**
     * Resets all conversation memory to clean state.
     */
    fun reset() {
        lastMentionedStudentName = null
        lastMentionedDayOfWeek = null
        lastMentionedTime = null
        lastMentionedAmount = null
        lastMentionedDuration = null
        lastMentionedPrice = null
        lastMentionedSubjects = null
        lastMentionedCourse = null
        lastActiveIntentType = null
        activeIncompleteIntent = null
    }

    /**
     * Preserves context when an action fails due to a conflict, allowing the user to
     * retry with an alternative time without having to re-specify the student or day.
     */
    fun preserveContextForConflict(action: SuePendingAction) {
        when (action) {
            is SuePendingAction.AddExtraClass -> {
                lastMentionedStudentName = action.studentName
                lastMentionedDayOfWeek = Instant.ofEpochMilli(action.date)
                    .atZone(ZoneId.systemDefault()).toLocalDate().dayOfWeek.value
                lastActiveIntentType = IntentType.ADD_EXTRA_CLASS
                setAwaitingSlot(IntentType.ADD_EXTRA_CLASS, SlotType.TIME)
            }
            is SuePendingAction.RescheduleClass -> {
                lastMentionedStudentName = action.studentName
                lastMentionedDayOfWeek = action.newDayOfWeek?.value
                    ?: Instant.ofEpochMilli(action.newDate).atZone(ZoneId.systemDefault()).toLocalDate().dayOfWeek.value
                lastActiveIntentType = IntentType.RESCHEDULE_CLASS
                setAwaitingSlot(IntentType.RESCHEDULE_CLASS, SlotType.TIME)
            }
            is SuePendingAction.CreateSchedule -> {
                lastMentionedStudentName = action.studentName
                lastMentionedDayOfWeek = action.dayOfWeek
                lastActiveIntentType = IntentType.CREATE_SCHEDULE
                setAwaitingSlot(IntentType.CREATE_SCHEDULE, SlotType.TIME)
            }
            else -> { /* No retry context needed for other actions */ }
        }
    }
}
