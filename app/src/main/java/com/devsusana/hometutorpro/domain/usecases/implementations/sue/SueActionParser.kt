package com.devsusana.hometutorpro.domain.usecases.implementations.sue

import com.devsusana.hometutorpro.domain.entities.PaymentType
import com.devsusana.hometutorpro.domain.entities.SueOperationResult
import com.devsusana.hometutorpro.domain.entities.SuePendingAction
import com.devsusana.hometutorpro.domain.repository.DateTimeProvider
import com.devsusana.hometutorpro.domain.usecases.implementations.ScheduleTools
import com.devsusana.hometutorpro.domain.usecases.implementations.StudentTools
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parses action tags emitted by the LLM (e.g. `[ACTION: CREATE_STUDENT, ...]`)
 * and maps them into pending actions.
 */
@Singleton
class SueActionParser @Inject constructor(
    private val studentTools: StudentTools,
    private val scheduleTools: ScheduleTools,
    private val dateTimeProvider: DateTimeProvider,
    private val entityExtractor: SueEntityExtractor,
    private val conversationContext: SueConversationContext
) {

    suspend fun parseLlmActionResponse(response: String): SueOperationResult.Prepare? {
        val pattern = """\[ACTION:\s*([A-Z_]+)(?:,\s*(.*))?]""".toRegex(RegexOption.IGNORE_CASE)
        val matches = pattern.findAll(response).toList()
        if (matches.isEmpty()) return null

        val preparedActions = mutableListOf<SuePendingAction>()

        for (match in matches) {
            val actionType = match.groupValues[1].trim()
            val paramsString = match.groupValues.getOrNull(2) ?: ""

            val params = mutableMapOf<String, String>()
            if (paramsString.isNotBlank()) {
                val paramPairs = paramsString.split(",")
                for (pair in paramPairs) {
                    val kv = pair.split(":")
                    if (kv.size == 2) {
                        val key = kv[0].trim()
                        val value = kv[1].trim().removeSurrounding("\"").trim()
                        params[key] = value
                    }
                }
            }

            val student = params["student"]
            val time = entityExtractor.normalizeLlmTime(params["time"])
            val startTime = entityExtractor.normalizeLlmTime(params["startTime"])
            val endTime = entityExtractor.normalizeLlmTime(params["endTime"])
            val targetTime = entityExtractor.normalizeLlmTime(params["targetTime"])
            val fromTime = entityExtractor.normalizeLlmTime(params["fromTime"])

            // Synchronize short-term conversational memory with LLM parameters
            if (student != null) conversationContext.lastMentionedStudentName = student
            if (time != null) conversationContext.lastMentionedTime = time
            if (startTime != null) conversationContext.lastMentionedTime = startTime
            if (targetTime != null) conversationContext.lastMentionedTime = targetTime
            params["amount"]?.toDoubleOrNull()?.let { conversationContext.lastMentionedAmount = it }
            params["price"]?.toDoubleOrNull()?.let { conversationContext.lastMentionedPrice = it }
            params["duration"]?.toIntOrNull()?.let { conversationContext.lastMentionedDuration = it }
            params["subjects"]?.let { conversationContext.lastMentionedSubjects = it }
            params["course"]?.let { conversationContext.lastMentionedCourse = it }
            params["day"]?.let { entityExtractor.extractDayOfWeek(it)?.let { d -> conversationContext.lastMentionedDayOfWeek = d } }

            val prepResult = when (actionType) {
                "START_CLASS" -> {
                    if (student != null) studentTools.prepareStartClass(student, params["duration"]?.toIntOrNull() ?: 60) else null
                }
                "CREATE_STUDENT" -> {
                    if (student != null) studentTools.prepareCreateStudent(student, params["course"] ?: "", params["subjects"] ?: "", params["price"]?.toDoubleOrNull() ?: 0.0) else null
                }
                "DELETE_STUDENT" -> {
                    if (student != null) studentTools.prepareDeleteStudent(student) else null
                }
                "REGISTER_PAYMENT" -> {
                    if (student != null) {
                        val type = if (params["type"] == "EFFECTIVE") PaymentType.EFFECTIVE else PaymentType.BIZUM
                        studentTools.prepareRegisterPayment(student, params["amount"]?.toDoubleOrNull() ?: 0.0, type)
                    } else null
                }
                "ADD_BALANCE" -> {
                    if (student != null) studentTools.prepareAddBalance(student, params["amount"]?.toDoubleOrNull() ?: 0.0) else null
                }
                "CANCEL_CLASS" -> {
                    if (student != null) scheduleTools.prepareCancelAction(student, entityExtractor.extractDayOfWeek(params["day"] ?: "") ?: dateTimeProvider.getNow().dayOfWeek.value, time) else null
                }
                "DELETE_SCHEDULE" -> {
                    if (student != null) scheduleTools.prepareDeleteSchedule(student, entityExtractor.extractDayOfWeek(params["day"] ?: "") ?: dateTimeProvider.getNow().dayOfWeek.value, time) else null
                }
                "CREATE_SCHEDULE" -> {
                    if (student != null) scheduleTools.prepareCreateSchedule(student, entityExtractor.extractDayOfWeek(params["day"] ?: "") ?: dateTimeProvider.getNow().dayOfWeek.value, startTime ?: "", endTime ?: "") else null
                }
                "ADD_EXTRA_CLASS" -> {
                    if (student != null) {
                        val dayOfWeek = entityExtractor.extractDayOfWeek(params["day"] ?: "") ?: dateTimeProvider.getNow().dayOfWeek.value
                        val targetDate = dateTimeProvider.getNow().toLocalDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.of(dayOfWeek)))
                        val dateMillis = targetDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        scheduleTools.prepareAddExtraClass(student, dateMillis, targetTime ?: "", endTime ?: "")
                    } else null
                }
                "RESCHEDULE_CLASS" -> {
                    if (student != null) {
                        val fromDay = entityExtractor.extractDayOfWeek(params["fromDay"] ?: "") ?: dateTimeProvider.getNow().dayOfWeek.value
                        val toDay = entityExtractor.extractDayOfWeek(params["toDay"] ?: "") ?: dateTimeProvider.getNow().dayOfWeek.value
                        scheduleTools.prepareRescheduleAction(student, fromDay, toDay, targetTime ?: "", fromTime)
                    } else null
                }
                "UPDATE_STUDENT_NOTES" -> {
                    if (student != null) studentTools.prepareUpdateNotesAction(student, params["notes"] ?: "") else null
                }
                else -> null
            }

            if (prepResult is SueOperationResult.Prepare.Success) {
                preparedActions.add(prepResult.action)
            } else if (prepResult is SueOperationResult.Prepare.Error) {
                return prepResult
            }
        }

        return if (preparedActions.size == 1) {
            SueOperationResult.Prepare.Success(preparedActions.first())
        } else if (preparedActions.isNotEmpty()) {
            SueOperationResult.Prepare.MultipleSuccess(preparedActions)
        } else {
            null
        }
    }
}
