package com.devsusana.hometutorpro.domain.usecases.implementations.sue

import com.devsusana.hometutorpro.domain.entities.SueOperationResult
import com.devsusana.hometutorpro.domain.entities.SuePendingAction
import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.devsusana.hometutorpro.domain.repository.DateTimeProvider
import com.devsusana.hometutorpro.domain.usecases.implementations.ScheduleTools
import com.devsusana.hometutorpro.domain.usecases.implementations.StudentTools
import java.util.Locale
import javax.inject.Inject

/**
 * Handles rule-based and keyword-driven intent detection, routing intents to domain-specific handlers
 * and managing multi-turn conversation context.
 */
class SueRuleIntentDetector @Inject constructor(
    private val studentTools: StudentTools,
    private val scheduleTools: ScheduleTools,
    private val dateTimeProvider: DateTimeProvider,
    private val authRepository: AuthRepository,
    private val entityExtractor: SueEntityExtractor,
    private val conversationContext: SueConversationContext,
    private val keywords: SueIntentKeywords = SueIntentKeywords(entityExtractor),
    private val studentHandler: SueStudentIntentHandler = SueStudentIntentHandler(studentTools, scheduleTools, entityExtractor, conversationContext),
    private val scheduleHandler: SueScheduleIntentHandler = SueScheduleIntentHandler(scheduleTools, studentTools, authRepository, dateTimeProvider, entityExtractor, conversationContext),
    private val financeHandler: SueFinanceIntentHandler = SueFinanceIntentHandler(studentTools, conversationContext)
) {

    suspend fun detectActionIntent(query: String): SueOperationResult? {
        val lower = entityExtractor.stripAccents(query.lowercase().trim())

        // If the query is creating a student and setting their price/class attributes, do not split on "y ponle ...".
        val isCreatingStudentWithAttributes = keywords.containsCreateStudentKeywords(lower) &&
            Regex("""\s+y\s+(?:ponle|pon|pónle|cobrale|cóbrale)\s+(?:la\s+clase\s+a|el\s+precio|la\s+tarifa|precio|tarifa|a\s+\d|\d+\s*(?:€|euros?|la\s+hora))\b""", RegexOption.IGNORE_CASE)
                .containsMatchIn(lower)

        val compoundSplitRegex = Regex(
            """\s+(?:y\s+que|y|e|además|ademas|y\s+luego|and\s+then|and\s+also|and)\s+(?=(?:adelanta|adelantar|adelante|adelantale|adelántale|mueve|mover|mueva|muevele|muévele|pasa|pasar|pase|pasale|pásale|reprograma|reprogramar|reprograme|reprogramale|reprográmale|cambia|cambiar|cambie|cambiale|cámbiale|cancela|cancelar|cancele|cancelale|cancélale|anula|anular|anule|anulale|anúlale|quita|quitar|quite|quitale|quítale|pon|poner|ponga|ponle|pónle|añade|añadir|anade|anadir|agrega|agregar|inicia|iniciar|move|reschedule|postpone|advance|cancel|remove|delete|add|create|schedule|start)\b)""",
            RegexOption.IGNORE_CASE
        )
        if (!isCreatingStudentWithAttributes && compoundSplitRegex.containsMatchIn(query)) {
            val clauses = query.split(compoundSplitRegex).map { it.trim() }.filter { it.isNotBlank() }
            if (clauses.size > 1) {
                val preparedActions = mutableListOf<SuePendingAction>()
                for (clause in clauses) {
                    val clauseResult = detectSingleActionIntent(clause)
                    when (clauseResult) {
                        is SueOperationResult.Prepare.Success -> preparedActions.add(clauseResult.action)
                        is SueOperationResult.Prepare.MultipleSuccess -> preparedActions.addAll(clauseResult.actions)
                        is SueOperationResult.Prepare.Error -> return clauseResult
                        else -> return clauseResult
                    }
                }
                return if (preparedActions.size > 1) {
                    clearConversationContext()
                    SueOperationResult.Prepare.MultipleSuccess(preparedActions)
                } else if (preparedActions.size == 1) {
                    clearConversationContext()
                    SueOperationResult.Prepare.Success(preparedActions.first())
                } else null
            }
        }
        return detectSingleActionIntent(query)
    }

    private suspend fun detectSingleActionIntent(query: String): SueOperationResult? {
        val lower = entityExtractor.stripAccents(query.lowercase().trim())

        // 1. If we have a pending intent type, check if the user wants to abort or change topic
        if (conversationContext.lastActiveIntentType != null) {
            val abortWords = listOf("no", "nada", "olvídalo", "olvida", "déjalo", "abortar", "aborta")
            if (abortWords.any { it == lower }) {
                conversationContext.reset()
                return null
            }
            if (entityExtractor.isQuestionOrQuery(lower)) {
                conversationContext.lastActiveIntentType = null
            }
        }

        // 2. Extract and update context memory variables
        val isCreateStudent = keywords.containsCreateStudentKeywords(lower)
        val isAwaitingNonStudentSlot = conversationContext.activeIncompleteIntent != null &&
                conversationContext.activeIncompleteIntent?.missingSlot != SlotType.STUDENT_NAME
        val isFinance = keywords.containsRegisterPaymentKeywords(lower) || keywords.containsAddBalanceKeywords(lower) ||
                keywords.containsStudentBalanceKeywords(lower) || keywords.containsStudentsWithBalanceKeywords(lower)

        val matchedStudent = if (isCreateStudent || isAwaitingNonStudentSlot) null else studentTools.extractRelevantStudent(lower)
        val matchedName = matchedStudent?.name?.takeIf { it.isNotBlank() }
            ?: if (isCreateStudent || isAwaitingNonStudentSlot) null else (
                entityExtractor.extractStudentName(lower)?.takeIf { it.isNotBlank() }
                    ?: (if (isFinance) entityExtractor.extractStudentNameForFinance(lower)?.takeIf { it.isNotBlank() } else null)
                    ?: entityExtractor.extractNameAfter(lower, listOf(
                        "inicia una clase para ", "inicia clase para ", "empieza clase para ",
                        "inicia una clase con ", "inicia clase con ", "empieza clase con ",
                        "start class for ", "start class with "
                    ))?.takeIf { it.isNotBlank() }
            )
        if (!matchedName.isNullOrBlank()) {
            val cleanName = entityExtractor.cleanExtractedName(matchedName) ?: matchedName
            if (cleanName.isNotBlank()) {
                val capitalizedName = cleanName.trim().split(" ").joinToString(" ") { word ->
                    word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                }
                conversationContext.lastMentionedStudentName = capitalizedName
            }
        }

        val relativeDay = entityExtractor.extractRelativeDayOfWeek(lower)
        val explicitDay = entityExtractor.extractDayOfWeek(lower)
        val matchedDay = relativeDay ?: explicitDay
        if (matchedDay != null) {
            conversationContext.lastMentionedDayOfWeek = matchedDay
        }

        val matchedTime = entityExtractor.extractTime(lower)
        if (matchedTime != null) {
            conversationContext.lastMentionedTime = matchedTime
        }

        val matchedAmount = entityExtractor.extractAmount(lower, conversationContext.lastMentionedAmount)
        if (matchedAmount != null) {
            conversationContext.lastMentionedAmount = matchedAmount
        }

        val matchedDuration = entityExtractor.extractDurationOptional(lower)
        if (matchedDuration != null) {
            conversationContext.lastMentionedDuration = matchedDuration
        }

        // 3. Determine the intent to process (explicit or fallback to last active intent)
        val explicitIntent = when {
            keywords.containsStartClassKeywords(lower) -> IntentType.START_CLASS
            keywords.containsCreateStudentKeywords(lower) -> IntentType.CREATE_STUDENT
            keywords.containsDeleteStudentKeywords(lower) -> IntentType.DELETE_STUDENT
            keywords.containsUpdateAddressKeywords(lower) -> IntentType.UPDATE_STUDENT_ADDRESS
            keywords.containsUpdatePriceKeywords(lower) -> IntentType.UPDATE_STUDENT_PRICE
            keywords.containsUpdatePhoneKeywords(lower) -> IntentType.UPDATE_STUDENT_PHONE
            keywords.containsUpdateCourseKeywords(lower) -> IntentType.UPDATE_STUDENT_COURSE
            keywords.containsUpdateNotesKeywords(lower) -> IntentType.UPDATE_STUDENT_NOTES
            keywords.containsAddExtraClassKeywords(lower) -> IntentType.ADD_EXTRA_CLASS
            keywords.containsCreateScheduleKeywords(lower) -> IntentType.CREATE_SCHEDULE
            keywords.containsDeleteScheduleKeywords(lower) -> IntentType.DELETE_SCHEDULE
            keywords.containsCancelledQueryKeywords(lower) -> IntentType.QUERY_CANCELLED_CLASSES
            keywords.containsCancelKeywords(lower) -> IntentType.CANCEL_CLASS
            keywords.containsRescheduleKeywords(lower) -> IntentType.RESCHEDULE_CLASS
            keywords.containsRegisterPaymentKeywords(lower) -> IntentType.REGISTER_PAYMENT
            keywords.containsAddBalanceKeywords(lower) -> IntentType.ADD_BALANCE

            // Read queries — only when there is no active multi-turn action in progress, or when user explicitly asks a question
            conversationContext.lastActiveIntentType == null || entityExtractor.isQuestionOrQuery(lower) -> when {
                keywords.containsStudentsWithBalanceKeywords(lower) -> IntentType.QUERY_STUDENTS_WITH_BALANCE
                keywords.containsStudentBalanceKeywords(lower) -> IntentType.QUERY_STUDENT_BALANCE
                keywords.containsStudentWeeklyClassesKeywords(lower) -> IntentType.QUERY_STUDENT_WEEKLY_CLASSES
                keywords.containsTodaySummaryKeywords(lower) -> IntentType.QUERY_TODAY_SUMMARY
                keywords.containsNextClassKeywords(lower) -> IntentType.QUERY_NEXT_CLASS
                keywords.containsFreeSlotWithDayKeywords(lower) || keywords.containsFreeSlotKeywords(lower) -> IntentType.QUERY_FREE_SLOTS
                keywords.containsStudentCountKeywords(lower) -> IntentType.QUERY_STUDENT_COUNT
                keywords.containsStudentDetailsKeywords(lower) -> IntentType.QUERY_STUDENT_DETAILS
                keywords.containsDayScheduleKeywords(lower) -> IntentType.QUERY_DAY_SCHEDULE
                else -> null
            }
            else -> null
        }

        val activeIntent = explicitIntent ?: conversationContext.lastActiveIntentType ?: return null

        val isReadQuery = activeIntent == IntentType.QUERY_NEXT_CLASS ||
                activeIntent == IntentType.QUERY_FREE_SLOTS ||
                activeIntent == IntentType.QUERY_STUDENT_COUNT ||
                activeIntent == IntentType.QUERY_STUDENT_DETAILS ||
                activeIntent == IntentType.QUERY_STUDENT_WEEKLY_CLASSES ||
                activeIntent == IntentType.QUERY_TODAY_SUMMARY ||
                activeIntent == IntentType.QUERY_STUDENTS_WITH_BALANCE ||
                activeIntent == IntentType.QUERY_STUDENT_BALANCE ||
                activeIntent == IntentType.QUERY_DAY_SCHEDULE ||
                activeIntent == IntentType.QUERY_CANCELLED_CLASSES

        if (!isReadQuery) {
            conversationContext.lastActiveIntentType = activeIntent
        }

        val result = when (activeIntent) {
            IntentType.START_CLASS -> scheduleHandler.handleStartClass()
            IntentType.CREATE_STUDENT -> studentHandler.handleCreateStudent(query, lower)
            IntentType.DELETE_STUDENT -> studentHandler.handleDeleteStudent()
            IntentType.ADD_EXTRA_CLASS -> scheduleHandler.handleAddExtraClass(lower)
            IntentType.CREATE_SCHEDULE -> scheduleHandler.handleCreateSchedule(lower)
            IntentType.DELETE_SCHEDULE -> scheduleHandler.handleDeleteSchedule()
            IntentType.CANCEL_CLASS -> scheduleHandler.handleCancelClass(lower)
            IntentType.RESCHEDULE_CLASS -> scheduleHandler.handleRescheduleClass(lower)
            IntentType.REGISTER_PAYMENT -> financeHandler.handleRegisterPayment(lower)
            IntentType.ADD_BALANCE -> financeHandler.handleAddBalance()
            IntentType.UPDATE_STUDENT_NOTES -> studentHandler.handleUpdateNotes(lower)
            IntentType.UPDATE_STUDENT_ADDRESS -> studentHandler.handleUpdateAddress(query, lower)
            IntentType.UPDATE_STUDENT_PRICE -> studentHandler.handleUpdatePrice(lower)
            IntentType.UPDATE_STUDENT_PHONE -> studentHandler.handleUpdatePhone(query, lower)
            IntentType.UPDATE_STUDENT_COURSE -> studentHandler.handleUpdateCourse(query, lower)
            IntentType.QUERY_NEXT_CLASS -> scheduleHandler.handleQueryNextClass()
            IntentType.QUERY_FREE_SLOTS -> scheduleHandler.handleQueryFreeSlots()
            IntentType.QUERY_STUDENT_COUNT -> studentHandler.handleQueryStudentCount()
            IntentType.QUERY_STUDENT_DETAILS -> studentHandler.handleQueryStudentDetails(query, lower)
            IntentType.QUERY_DAY_SCHEDULE -> scheduleHandler.handleQueryDaySchedule(lower)
            IntentType.QUERY_STUDENT_WEEKLY_CLASSES -> studentHandler.handleQueryStudentWeeklyClasses(lower)
            IntentType.QUERY_STUDENTS_WITH_BALANCE -> financeHandler.handleQueryStudentsWithBalance()
            IntentType.QUERY_STUDENT_BALANCE -> financeHandler.handleQueryStudentBalance()
            IntentType.QUERY_TODAY_SUMMARY -> scheduleHandler.handleQueryTodaySummary()
            IntentType.QUERY_CANCELLED_CLASSES -> scheduleHandler.handleQueryCancelledClasses(
                lower = lower,
                matchedStudentName = matchedStudent?.name,
                relativeDay = relativeDay,
                explicitDay = explicitDay
            )
        }

        if (result is SueOperationResult.Prepare.Success) {
            clearConversationContext()
        }
        return result
    }

    private fun clearConversationContext() {
        conversationContext.lastActiveIntentType = null
        conversationContext.activeIncompleteIntent = null
        conversationContext.lastMentionedTime = null
        conversationContext.lastMentionedDuration = null
        conversationContext.lastMentionedAmount = null
        conversationContext.lastMentionedPrice = null
        conversationContext.lastMentionedStudentName = null
        conversationContext.lastMentionedSubjects = null
        conversationContext.lastMentionedCourse = null
    }
}
