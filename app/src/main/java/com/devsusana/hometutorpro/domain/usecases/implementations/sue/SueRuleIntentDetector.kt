package com.devsusana.hometutorpro.domain.usecases.implementations.sue

import com.devsusana.hometutorpro.domain.entities.ExceptionType
import com.devsusana.hometutorpro.domain.entities.PaymentType
import com.devsusana.hometutorpro.domain.entities.SueOperationResult
import com.devsusana.hometutorpro.domain.entities.SuePendingAction
import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.devsusana.hometutorpro.domain.repository.DateTimeProvider
import com.devsusana.hometutorpro.domain.usecases.implementations.ScheduleTools
import com.devsusana.hometutorpro.domain.usecases.implementations.StudentTools
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import javax.inject.Inject

/**
 * Handles rule-based and keyword-driven intent detection and multi-turn slot filling.
 */
class SueRuleIntentDetector @Inject constructor(
    private val studentTools: StudentTools,
    private val scheduleTools: ScheduleTools,
    private val dateTimeProvider: DateTimeProvider,
    private val authRepository: AuthRepository,
    private val entityExtractor: SueEntityExtractor,
    private val conversationContext: SueConversationContext
) {

    suspend fun detectActionIntent(query: String): SueOperationResult? {
        val lower = entityExtractor.stripAccents(query.lowercase().trim())

        // If the query is creating a student and setting their price/class attributes (e.g. "crea un alumno llamalo nuevo y ponle la clase a 9€ la hora"),
        // do not split on "y ponle ...". Treat it as a single CREATE_STUDENT action.
        val isCreatingStudentWithAttributes = containsCreateStudentKeywords(lower) &&
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
                    conversationContext.lastActiveIntentType = null
                    conversationContext.activeIncompleteIntent = null
                    conversationContext.lastMentionedTime = null
                    conversationContext.lastMentionedDuration = null
                    conversationContext.lastMentionedAmount = null
                    conversationContext.lastMentionedPrice = null
                    conversationContext.lastMentionedStudentName = null
                    conversationContext.lastMentionedSubjects = null
                    conversationContext.lastMentionedCourse = null
                    SueOperationResult.Prepare.MultipleSuccess(preparedActions)
                } else if (preparedActions.size == 1) {
                    conversationContext.lastActiveIntentType = null
                    conversationContext.activeIncompleteIntent = null
                    conversationContext.lastMentionedTime = null
                    conversationContext.lastMentionedDuration = null
                    conversationContext.lastMentionedAmount = null
                    conversationContext.lastMentionedPrice = null
                    conversationContext.lastMentionedStudentName = null
                    conversationContext.lastMentionedSubjects = null
                    conversationContext.lastMentionedCourse = null
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
        val isCreateStudent = containsCreateStudentKeywords(lower)
        val isAwaitingNonStudentSlot = conversationContext.activeIncompleteIntent != null &&
                conversationContext.activeIncompleteIntent?.missingSlot != SlotType.STUDENT_NAME
        val isFinance = containsRegisterPaymentKeywords(lower) || containsAddBalanceKeywords(lower) ||
                containsStudentBalanceKeywords(lower) || containsStudentsWithBalanceKeywords(lower)

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
            containsStartClassKeywords(lower) -> IntentType.START_CLASS
            containsCreateStudentKeywords(lower) -> IntentType.CREATE_STUDENT
            containsDeleteStudentKeywords(lower) -> IntentType.DELETE_STUDENT
            containsUpdateAddressKeywords(lower) -> IntentType.UPDATE_STUDENT_ADDRESS
            containsUpdatePriceKeywords(lower) -> IntentType.UPDATE_STUDENT_PRICE
            containsUpdatePhoneKeywords(lower) -> IntentType.UPDATE_STUDENT_PHONE
            containsUpdateCourseKeywords(lower) -> IntentType.UPDATE_STUDENT_COURSE
            containsUpdateNotesKeywords(lower) -> IntentType.UPDATE_STUDENT_NOTES
            containsAddExtraClassKeywords(lower) -> IntentType.ADD_EXTRA_CLASS
            containsCreateScheduleKeywords(lower) -> IntentType.CREATE_SCHEDULE
            containsDeleteScheduleKeywords(lower) -> IntentType.DELETE_SCHEDULE
            containsCancelledQueryKeywords(lower) -> IntentType.QUERY_CANCELLED_CLASSES
            containsCancelKeywords(lower) -> IntentType.CANCEL_CLASS
            containsRescheduleKeywords(lower) -> IntentType.RESCHEDULE_CLASS
            containsRegisterPaymentKeywords(lower) -> IntentType.REGISTER_PAYMENT
            containsAddBalanceKeywords(lower) -> IntentType.ADD_BALANCE

            // Read queries — only when there is no active multi-turn action in progress, or when user explicitly asks a question
            conversationContext.lastActiveIntentType == null || entityExtractor.isQuestionOrQuery(lower) -> when {
                containsStudentsWithBalanceKeywords(lower) -> IntentType.QUERY_STUDENTS_WITH_BALANCE
                containsStudentBalanceKeywords(lower) -> IntentType.QUERY_STUDENT_BALANCE
                containsStudentWeeklyClassesKeywords(lower) -> IntentType.QUERY_STUDENT_WEEKLY_CLASSES
                containsTodaySummaryKeywords(lower) -> IntentType.QUERY_TODAY_SUMMARY
                containsNextClassKeywords(lower) -> IntentType.QUERY_NEXT_CLASS
                containsFreeSlotWithDayKeywords(lower) || containsFreeSlotKeywords(lower) -> IntentType.QUERY_FREE_SLOTS
                containsStudentCountKeywords(lower) -> IntentType.QUERY_STUDENT_COUNT
                containsStudentDetailsKeywords(lower) -> IntentType.QUERY_STUDENT_DETAILS
                containsDayScheduleKeywords(lower) -> IntentType.QUERY_DAY_SCHEDULE
                else -> null
            }
            else -> null
        }

        val activeIntent = explicitIntent ?: conversationContext.lastActiveIntentType
        if (activeIntent == null) {
            return null
        }

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
            IntentType.START_CLASS -> {
                val studentName = conversationContext.lastMentionedStudentName
                if (studentName == null) {
                    conversationContext.setAwaitingSlot(IntentType.START_CLASS, SlotType.STUDENT_NAME)
                    SueOperationResult.Prepare.Error(
                        SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
                        "¿Para qué alumno quieres iniciar la clase?"
                    )
                } else {
                    val duration = conversationContext.lastMentionedDuration
                    if (duration == null) {
                        conversationContext.setAwaitingSlot(IntentType.START_CLASS, SlotType.DURATION)
                        SueOperationResult.Prepare.Error(
                            SueOperationResult.ErrorType.UNKNOWN,
                            "¿De cuántos minutos será la clase con $studentName?"
                        )
                    } else {
                        studentTools.prepareStartClass(studentName, duration)
                    }
                }
            }

            IntentType.CREATE_STUDENT -> {
                val prefixesToStrip = listOf(
                    "crear un nuevo estudiante", "crear un nuevo alumno", "crear una nueva estudiante", "crear una nueva alumna",
                    "crea un nuevo estudiante", "crea un nuevo alumno", "crea una nueva estudiante", "crea una nueva alumna",
                    "añadir un nuevo estudiante", "añadir un nuevo alumno", "añade un nuevo estudiante", "añade un nuevo alumno",
                    "anadir un nuevo estudiante", "anadir un nuevo alumno", "anade un nuevo estudiante", "anade un nuevo alumno",
                    "crear un estudiante", "crear un alumno", "crea un estudiante", "crea un alumno",
                    "crear al estudiante", "crear al alumno", "crea al estudiante", "crea al alumno",
                    "crear el estudiante", "crear el alumno", "crea el estudiante", "crea el alumno",
                    "crear a un estudiante", "crear a un alumno", "crea a un estudiante", "crea a un alumno",
                    "añadir un estudiante", "añadir un alumno", "añade un estudiante", "añade un alumno",
                    "anadir un estudiante", "anadir un alumno", "anade un estudiante", "anade un alumno",
                    "añadir al estudiante", "añadir al alumno", "añade al estudiante", "añade al alumno",
                    "anadir al estudiante", "anadir al alumno", "anade al estudiante", "anade al alumno",
                    "añadir a un estudiante", "añadir a un alumno", "añade a un estudiante", "añade a un alumno",
                    "anadir a un estudiante", "anadir a un alumno", "anade a un estudiante", "anade a un alumno",
                    "añadir estudiante", "añadir alumno", "anadir estudiante", "anadir alumno",
                    "añade estudiante", "añade alumno", "anade estudiante", "anade alumno",
                    "agregar un alumno", "agrega un alumno", "agregar alumno", "agrega alumno",
                    "nuevo estudiante", "nuevo alumno", "nueva estudiante", "nueva alumna",
                    "create student", "add student"
                )
                var queryWithoutKeywords = lower
                for (p in prefixesToStrip) {
                    queryWithoutKeywords = queryWithoutKeywords.replace(p, "")
                }
                queryWithoutKeywords = queryWithoutKeywords.trim()

                if (conversationContext.lastMentionedStudentName.isNullOrBlank()) {
                    val nameIntroRegex = Regex("""^(?:llamado|llamada|llámalo|llamalo|llámale|llamale|que\s+se\s+llama|que\s+se\s+llame|se\s+llama|se\s+llame|de\s+nombre|con\s+nombre|nombre)\s+""", RegexOption.IGNORE_CASE)
                    var nameCandidate = queryWithoutKeywords.replace(nameIntroRegex, "").trim()
                    val stopWords = listOf(
                        " y ponle ", " y pon ", " y la clase ", " y el precio ", " y cobrale ", " y cóbrale ",
                        " con precio ", " con tarifa ", " con clase ", " y ", " e ", " con ", " a ", " por ",
                        " para ", " de ", " en ", " que ", ",", "."
                    )
                    var firstStopIdx = nameCandidate.length
                    for (stop in stopWords) {
                        val stopIdx = nameCandidate.indexOf(stop)
                        if (stopIdx in 0 until firstStopIdx) {
                            firstStopIdx = stopIdx
                        }
                    }
                    nameCandidate = nameCandidate.substring(0, firstStopIdx).trim()
                    val cleanCandidate = entityExtractor.cleanExtractedName(nameCandidate) ?: nameCandidate.trim()
                    if (cleanCandidate.isNotBlank() && !cleanCandidate.contains(Regex("""\d"""))) {
                        conversationContext.lastMentionedStudentName = cleanCandidate.split(" ").joinToString(" ") { word ->
                            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                        }
                    }
                }

                val price = entityExtractor.extractPrice(lower) ?: entityExtractor.extractAmount(lower, null)
                if (price != null) {
                    conversationContext.lastMentionedPrice = price
                }

                val subjectKeywords = listOf(" de ", " in ", " para ", " for ")
                for (kw in subjectKeywords) {
                    val idx = lower.indexOf(kw)
                    if (idx >= 0) {
                        val rest = query.substring(idx + kw.length).trim()
                        val stopWords = listOf(" a ", " por ", " at ", " for ", " y ponle ", " y pon ", " con precio ")
                        var end = rest.length
                        for (stop in stopWords) {
                            val stopIdx = rest.lowercase().indexOf(stop)
                            if (stopIdx in 1 until end) end = stopIdx
                        }
                        val candidate = rest.substring(0, end).trim()
                        if (candidate.isNotBlank() && !candidate.contains(Regex("""\d""")) && 
                            !listOf("lunes", "martes", "miércoles", "miercoles", "jueves", "viernes", "sábado", "sabado", "domingo").any { it in candidate.lowercase() }) {
                            val normalized = entityExtractor.getPhoneticMatcher().normalizeSubjects(candidate)
                            if (normalized.isNotBlank()) {
                                conversationContext.lastMentionedSubjects = normalized
                                break
                            }
                        }
                    }
                }

                val courseKeywords = listOf("eso", "bachillerato", "bach", "primaria", "secundaria", "universidad")
                val courseMatch = courseKeywords.find { it in lower }
                if (courseMatch != null) {
                    conversationContext.lastMentionedCourse = courseMatch
                }

                val studentName = conversationContext.lastMentionedStudentName
                if (studentName.isNullOrBlank()) {
                    conversationContext.setAwaitingSlot(IntentType.CREATE_STUDENT, SlotType.STUDENT_NAME)
                    SueOperationResult.Prepare.Error(
                        SueOperationResult.ErrorType.UNKNOWN,
                        "¿Cómo se llama el nuevo alumno?"
                    )
                } else {
                    val priceVal = conversationContext.lastMentionedPrice
                    if (priceVal == null) {
                        conversationContext.setAwaitingSlot(IntentType.CREATE_STUDENT, SlotType.PRICE)
                        SueOperationResult.Prepare.Error(
                            SueOperationResult.ErrorType.UNKNOWN,
                            "¿Cuál será el precio por hora de $studentName?"
                        )
                    } else {
                        val finalCourse = conversationContext.lastMentionedCourse ?: ""
                        val finalSubjects = conversationContext.lastMentionedSubjects ?: ""
                        studentTools.prepareCreateStudent(studentName, finalCourse, finalSubjects, priceVal)
                    }
                }
            }

            IntentType.DELETE_STUDENT -> {
                val studentName = conversationContext.lastMentionedStudentName
                if (studentName == null) {
                    conversationContext.setAwaitingSlot(IntentType.DELETE_STUDENT, SlotType.STUDENT_NAME)
                    SueOperationResult.Prepare.Error(
                        SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
                        "¿De qué alumno quieres eliminar el perfil?"
                    )
                } else {
                    studentTools.prepareDeleteStudent(studentName)
                }
            }

            IntentType.ADD_EXTRA_CLASS -> {
                val studentName = conversationContext.lastMentionedStudentName
                if (studentName == null) {
                    conversationContext.setAwaitingSlot(IntentType.ADD_EXTRA_CLASS, SlotType.STUDENT_NAME)
                    SueOperationResult.Prepare.Error(
                        SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
                        "¿Para qué alumno quieres programar la clase extra?"
                    )
                } else {
                    val times = entityExtractor.extractTwoTimes(lower) ?: conversationContext.lastMentionedTime?.let { lastTime ->
                        val parts = lastTime.split(":")
                        val h = parts[0].toInt()
                        val m = parts[1].toInt()
                        val endH = (h + 1) % 24
                        val endTime = "%02d:%02d".format(endH, m)
                        Pair(lastTime, endTime)
                    }

                    val dayOfWeek = conversationContext.lastMentionedDayOfWeek ?: if (times != null) dateTimeProvider.getNow().dayOfWeek.value else null
                    if (dayOfWeek == null) {
                        conversationContext.setAwaitingSlot(IntentType.ADD_EXTRA_CLASS, SlotType.DAY_OF_WEEK)
                        SueOperationResult.Prepare.Error(
                            SueOperationResult.ErrorType.UNKNOWN,
                            "¿Qué día y a qué hora quieres la clase extra de $studentName?"
                        )
                    } else if (times == null) {
                        conversationContext.setAwaitingSlot(IntentType.ADD_EXTRA_CLASS, SlotType.TIME)
                        val dayName = when (dayOfWeek) {
                            1 -> "el lunes"; 2 -> "el martes"; 3 -> "el miércoles"; 4 -> "el jueves"
                            5 -> "el viernes"; 6 -> "el sábado"; 7 -> "el domingo"; else -> "ese día"
                        }
                        SueOperationResult.Prepare.Error(
                            SueOperationResult.ErrorType.UNKNOWN,
                            "¿A qué hora quieres programar la clase extra de $studentName $dayName?"
                        )
                    } else {
                        val targetDate = dateTimeProvider.getNow().toLocalDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.of(dayOfWeek)))
                        val dateMillis = targetDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        scheduleTools.prepareAddExtraClass(studentName, dateMillis, times.first, times.second)
                    }
                }
            }

            IntentType.CREATE_SCHEDULE -> {
                val studentName = conversationContext.lastMentionedStudentName
                if (studentName == null) {
                    conversationContext.setAwaitingSlot(IntentType.CREATE_SCHEDULE, SlotType.STUDENT_NAME)
                    SueOperationResult.Prepare.Error(
                        SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
                        "¿Para qué alumno deseas configurar el horario?"
                    )
                } else {
                    val dayOfWeek = conversationContext.lastMentionedDayOfWeek
                    if (dayOfWeek == null) {
                        conversationContext.setAwaitingSlot(IntentType.CREATE_SCHEDULE, SlotType.DAY_OF_WEEK)
                        SueOperationResult.Prepare.Error(
                            SueOperationResult.ErrorType.UNKNOWN,
                            "¿Qué día de la semana será la clase de $studentName?"
                        )
                    } else {
                        val times = entityExtractor.extractTwoTimes(lower) ?: conversationContext.lastMentionedTime?.let { lastTime ->
                            val parts = lastTime.split(":")
                            val h = parts[0].toInt()
                            val m = parts[1].toInt()
                            val endH = (h + 1) % 24
                            val endTime = "%02d:%02d".format(endH, m)
                            Pair(lastTime, endTime)
                        }
                        if (times == null) {
                            conversationContext.setAwaitingSlot(IntentType.CREATE_SCHEDULE, SlotType.TIME)
                            val dayName = when(dayOfWeek) {
                                1 -> "lunes"
                                2 -> "martes"
                                3 -> "miércoles"
                                4 -> "jueves"
                                5 -> "viernes"
                                6 -> "sábado"
                                7 -> "domingo"
                                else -> "ese día"
                            }
                            SueOperationResult.Prepare.Error(
                                SueOperationResult.ErrorType.UNKNOWN,
                                "¿A qué hora será la clase de $studentName los $dayName?"
                            )
                        } else {
                            scheduleTools.prepareCreateSchedule(studentName, dayOfWeek, times.first, times.second)
                        }
                    }
                }
            }

            IntentType.DELETE_SCHEDULE -> {
                val studentName = conversationContext.lastMentionedStudentName
                if (studentName == null) {
                    conversationContext.setAwaitingSlot(IntentType.DELETE_SCHEDULE, SlotType.STUDENT_NAME)
                    SueOperationResult.Prepare.Error(
                        SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
                        "¿De qué alumno quieres eliminar el horario?"
                    )
                } else {
                    val dayOfWeek = conversationContext.lastMentionedDayOfWeek
                    if (dayOfWeek == null) {
                        conversationContext.setAwaitingSlot(IntentType.DELETE_SCHEDULE, SlotType.DAY_OF_WEEK)
                        SueOperationResult.Prepare.Error(
                            SueOperationResult.ErrorType.UNKNOWN,
                            "¿Qué día de la semana es el horario que quieres eliminar?"
                        )
                    } else {
                        val time = conversationContext.lastMentionedTime
                        scheduleTools.prepareDeleteSchedule(studentName, dayOfWeek, time)
                    }
                }
            }

            IntentType.CANCEL_CLASS -> {
                val studentName = conversationContext.lastMentionedStudentName
                if (studentName == null) {
                    conversationContext.setAwaitingSlot(IntentType.CANCEL_CLASS, SlotType.STUDENT_NAME)
                    SueOperationResult.Prepare.Error(
                        SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
                        "¿De qué alumno quieres cancelar la clase?"
                    )
                } else {
                    var dayOfWeek = conversationContext.lastMentionedDayOfWeek
                    val time = conversationContext.lastMentionedTime
                    val preferExtra = listOf("extra", "extras", "adicional", "adicionales", "refuerzo").any { it in lower }

                    if (dayOfWeek == null) {
                        val schedules = scheduleTools.getSchedulesByStudentName(studentName)
                        if (preferExtra) {
                            scheduleTools.prepareCancelAction(
                                studentName,
                                dateTimeProvider.getNow().dayOfWeek.value,
                                time,
                                preferExtra = true
                            )
                        } else if (schedules.isEmpty()) {
                            scheduleTools.prepareCancelAction(
                                studentName,
                                dateTimeProvider.getNow().dayOfWeek.value,
                                time,
                                preferExtra = false
                            )
                        } else if (schedules.size == 1) {
                            dayOfWeek = schedules.first().dayOfWeek
                            scheduleTools.prepareCancelAction(studentName, dayOfWeek, time, preferExtra = preferExtra)
                        } else {
                            conversationContext.setAwaitingSlot(IntentType.CANCEL_CLASS, SlotType.DAY_OF_WEEK)
                            SueOperationResult.Prepare.Error(
                                SueOperationResult.ErrorType.CLASS_NOT_FOUND,
                                "¿Qué día es la clase de $studentName que quieres cancelar?"
                            )
                        }
                    } else {
                        scheduleTools.prepareCancelAction(studentName, dayOfWeek, time, preferExtra = preferExtra)
                    }
                }
            }

            IntentType.RESCHEDULE_CLASS -> {
                val studentName = conversationContext.lastMentionedStudentName
                if (studentName == null) {
                    conversationContext.setAwaitingSlot(IntentType.RESCHEDULE_CLASS, SlotType.STUDENT_NAME)
                    SueOperationResult.Prepare.Error(
                        SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
                        "¿De qué alumno quieres mover la clase?"
                    )
                } else {
                    val days = entityExtractor.extractTwoDaysOfWeek(lower)
                    var fromDay: Int? = null
                    var toDay: Int? = null

                    val schedules = scheduleTools.getSchedulesByStudentName(studentName)
                    if (schedules.isEmpty()) {
                        SueOperationResult.Prepare.Error(
                            SueOperationResult.ErrorType.CLASS_NOT_FOUND,
                            "No he encontrado ninguna clase programada para $studentName."
                        )
                    } else {
                        if (days.first != null && days.second != null) {
                            fromDay = days.first
                            toDay = days.second
                        } else if (days.first != null) {
                            val singleDay = days.first!!
                            val hasClassOnSingleDay = schedules.any { it.dayOfWeek == singleDay }
                            if (hasClassOnSingleDay) {
                                if (entityExtractor.isTargetDay(lower, singleDay)) {
                                    toDay = singleDay
                                } else {
                                    fromDay = singleDay
                                }
                            } else {
                                toDay = singleDay
                            }
                        }

                        val times = entityExtractor.extractAllTimesInQuery(lower)
                        var fromTime: String? = null
                        var targetTime: String? = null

                        if (times.size >= 2) {
                            val time0 = times[0]
                            val time1 = times[1]
                            val rawHour0 = time0.substringBefore(":")
                            val rawHour1 = time1.substringBefore(":")
                            val isTarget0 = Regex("""\b(?:a|para|hacia|to)\s+(?:las?\s+)?(?:$time0|$rawHour0)\b""").containsMatchIn(lower)
                            val isSource1 = Regex("""\b(?:de|desde|que\s+es\s+a|from|which\s+is\s+at)\s+(?:las?\s+)?(?:$time1|$rawHour1)\b""").containsMatchIn(lower)
                            if (isTarget0 && isSource1) {
                                fromTime = time1
                                targetTime = time0
                            } else {
                                fromTime = time0
                                targetTime = time1
                            }
                        } else if (times.size == 1) {
                            val singleTime = times[0]
                            val rawHour = singleTime.substringBefore(":")
                            val isSourceMarker = Regex("""\b(?:de|desde|from)\s+(?:las?\s+)?(?:$singleTime|$rawHour)\b""").containsMatchIn(lower)
                            val isTargetMarker = Regex("""\b(?:a|para|hacia|to|at)\s+(?:las?\s+)?(?:$singleTime|$rawHour)\b""").containsMatchIn(lower)
                            val hasClassAtTime = schedules.any {
                                it.startTime == singleTime || it.startTime.substringBefore(":") == rawHour
                            }
                            if (isSourceMarker) {
                                fromTime = singleTime
                                targetTime = null
                            } else if (isTargetMarker && !hasClassAtTime) {
                                fromTime = null
                                targetTime = singleTime
                            } else if (hasClassAtTime && !isTargetMarker) {
                                fromTime = singleTime
                                targetTime = singleTime
                            } else {
                                fromTime = null
                                targetTime = singleTime
                            }
                        } else {
                            fromTime = null
                            targetTime = conversationContext.lastMentionedTime
                        }

                        if (fromDay == null && fromTime != null) {
                            val matching = schedules.firstOrNull {
                                it.startTime == fromTime || it.startTime.substringBefore(":") == fromTime.substringBefore(":")
                            }
                            if (matching != null) {
                                fromDay = matching.dayOfWeek
                            }
                        }

                        if (fromDay == null) {
                            if (schedules.size == 1) {
                                fromDay = schedules.first().dayOfWeek
                            } else {
                                val fallbackDay = conversationContext.lastMentionedDayOfWeek ?: dateTimeProvider.getNow().dayOfWeek.value
                                val hasClassOnFallback = schedules.any { it.dayOfWeek == fallbackDay }
                                if (hasClassOnFallback) {
                                    fromDay = fallbackDay
                                } else {
                                    val todayDay = dateTimeProvider.getNow().dayOfWeek.value
                                    val todayMatch = schedules.firstOrNull { it.dayOfWeek == todayDay }
                                    if (todayMatch != null) {
                                        fromDay = todayDay
                                    } else {
                                        fromDay = schedules.firstOrNull()?.dayOfWeek
                                    }
                                }
                            }
                        }

                        if (fromDay == null) {
                            conversationContext.setAwaitingSlot(IntentType.RESCHEDULE_CLASS, SlotType.DAY_OF_WEEK)
                            SueOperationResult.Prepare.Error(
                                SueOperationResult.ErrorType.CLASS_NOT_FOUND,
                                "¿Qué día es la clase de $studentName que quieres mover?"
                            )
                        } else {
                            if (toDay == null) {
                                toDay = fromDay
                            }

                            val daySchedules = schedules.filter { it.dayOfWeek == fromDay }
                            if (fromTime == null && daySchedules.size == 1) {
                                fromTime = daySchedules.first().startTime
                            }

                            if (targetTime == null) {
                                if (fromTime == null && daySchedules.size > 1) {
                                    return SueOperationResult.Prepare.Error(
                                        SueOperationResult.ErrorType.UNKNOWN,
                                        "¿Qué clase de $studentName quieres mover? Tiene varias ese día."
                                    )
                                }
                                val match = if (fromTime != null) {
                                    daySchedules.firstOrNull { it.startTime == fromTime }
                                        ?: daySchedules.firstOrNull { it.startTime.substringBefore(":") == fromTime.substringBefore(":") }
                                        ?: daySchedules.firstOrNull()
                                } else {
                                    daySchedules.firstOrNull()
                                }
                                if (match != null) {
                                    targetTime = match.startTime
                                }
                            }

                            if (targetTime == null) {
                                conversationContext.setAwaitingSlot(IntentType.RESCHEDULE_CLASS, SlotType.TIME)
                                SueOperationResult.Prepare.Error(
                                    SueOperationResult.ErrorType.UNKNOWN,
                                    "¿A qué hora quieres programar la clase de $studentName?"
                                )
                            } else {
                                scheduleTools.prepareRescheduleAction(studentName, fromDay, toDay, targetTime, fromTime)
                            }
                        }
                    }
                }
            }

            IntentType.REGISTER_PAYMENT -> {
                val studentName = conversationContext.lastMentionedStudentName
                if (studentName == null) {
                    conversationContext.setAwaitingSlot(IntentType.REGISTER_PAYMENT, SlotType.STUDENT_NAME)
                    SueOperationResult.Prepare.Error(
                        SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
                        "¿De qué alumno quieres registrar el pago?"
                    )
                } else {
                    val amount = conversationContext.lastMentionedAmount
                    if (amount == null) {
                        conversationContext.setAwaitingSlot(IntentType.REGISTER_PAYMENT, SlotType.AMOUNT)
                        SueOperationResult.Prepare.Error(
                            SueOperationResult.ErrorType.UNKNOWN,
                            "¿De cuánto es el pago de $studentName?"
                        )
                    } else {
                        val paymentType = if (lower.contains("bizum")) PaymentType.BIZUM else PaymentType.EFFECTIVE
                        studentTools.prepareRegisterPayment(studentName, amount, paymentType)
                    }
                }
            }

            IntentType.ADD_BALANCE -> {
                val studentName = conversationContext.lastMentionedStudentName
                if (studentName == null) {
                    conversationContext.setAwaitingSlot(IntentType.ADD_BALANCE, SlotType.STUDENT_NAME)
                    SueOperationResult.Prepare.Error(
                        SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
                        "¿A qué alumno le quieres sumar saldo?"
                    )
                } else {
                    val amount = conversationContext.lastMentionedAmount
                    if (amount == null) {
                        conversationContext.setAwaitingSlot(IntentType.ADD_BALANCE, SlotType.AMOUNT)
                        SueOperationResult.Prepare.Error(
                            SueOperationResult.ErrorType.UNKNOWN,
                            "¿Cuánto saldo deseas sumarle a $studentName?"
                        )
                    } else {
                        studentTools.prepareAddBalance(studentName, amount)
                    }
                }
            }

            IntentType.UPDATE_STUDENT_NOTES -> {
                val studentName = conversationContext.lastMentionedStudentName
                if (studentName == null) {
                    conversationContext.setAwaitingSlot(IntentType.UPDATE_STUDENT_NOTES, SlotType.STUDENT_NAME)
                    SueOperationResult.Prepare.Error(
                        SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
                        "¿De qué alumno quieres actualizar las notas?"
                    )
                } else {
                    val noteText = entityExtractor.extractNotesText(lower)
                    if (noteText == null) {
                        conversationContext.setAwaitingSlot(IntentType.UPDATE_STUDENT_NOTES, SlotType.NOTES)
                        SueOperationResult.Prepare.Error(
                            SueOperationResult.ErrorType.UNKNOWN,
                            "¿Qué notas o apuntes quieres añadir para $studentName?"
                        )
                    } else {
                        studentTools.prepareUpdateNotesAction(studentName, noteText)
                    }
                }
            }

            IntentType.UPDATE_STUDENT_ADDRESS -> {
                val studentName = conversationContext.lastMentionedStudentName
                if (studentName == null) {
                    conversationContext.setAwaitingSlot(IntentType.UPDATE_STUDENT_ADDRESS, SlotType.STUDENT_NAME)
                    SueOperationResult.Prepare.Error(
                        SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
                        "¿De qué alumno quieres actualizar la dirección?"
                    )
                } else {
                    val addressText = if (conversationContext.activeIncompleteIntent?.missingSlot == SlotType.ADDRESS && !entityExtractor.isQuestionOrQuery(lower)) {
                        query.trim()
                    } else {
                        conversationContext.lastMentionedAddress ?: entityExtractor.extractAddressText(query, studentName)
                    }
                    if (addressText.isNullOrBlank()) {
                        conversationContext.setAwaitingSlot(IntentType.UPDATE_STUDENT_ADDRESS, SlotType.ADDRESS)
                        SueOperationResult.Prepare.Error(
                            SueOperationResult.ErrorType.UNKNOWN,
                            "¿Cuál es la nueva dirección para $studentName?"
                        )
                    } else {
                        conversationContext.lastMentionedAddress = null
                        studentTools.prepareUpdateAddressAction(studentName, addressText)
                    }
                }
            }

            IntentType.UPDATE_STUDENT_PRICE -> {
                val studentName = conversationContext.lastMentionedStudentName
                if (studentName == null) {
                    conversationContext.setAwaitingSlot(IntentType.UPDATE_STUDENT_PRICE, SlotType.STUDENT_NAME)
                    SueOperationResult.Prepare.Error(
                        SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
                        "¿De qué alumno quieres actualizar el precio por hora?"
                    )
                } else {
                    val priceVal = conversationContext.lastMentionedPrice
                        ?: (entityExtractor.extractPrice(lower) ?: entityExtractor.extractAmount(lower))
                    if (priceVal == null) {
                        conversationContext.setAwaitingSlot(IntentType.UPDATE_STUDENT_PRICE, SlotType.PRICE)
                        SueOperationResult.Prepare.Error(
                            SueOperationResult.ErrorType.UNKNOWN,
                            "¿Cuál será el nuevo precio por hora para $studentName?"
                        )
                    } else {
                        conversationContext.lastMentionedPrice = null
                        studentTools.prepareUpdatePriceAction(studentName, priceVal)
                    }
                }
            }

            IntentType.UPDATE_STUDENT_PHONE -> {
                val studentName = conversationContext.lastMentionedStudentName
                if (studentName == null) {
                    conversationContext.setAwaitingSlot(IntentType.UPDATE_STUDENT_PHONE, SlotType.STUDENT_NAME)
                    SueOperationResult.Prepare.Error(
                        SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
                        "¿De qué alumno quieres actualizar el teléfono?"
                    )
                } else {
                    val phoneText = if (conversationContext.activeIncompleteIntent?.missingSlot == SlotType.PHONE && !entityExtractor.isQuestionOrQuery(lower)) {
                        query.trim()
                    } else {
                        conversationContext.lastMentionedPhone ?: entityExtractor.extractPhoneNumber(query)
                    }
                    if (phoneText.isNullOrBlank()) {
                        conversationContext.setAwaitingSlot(IntentType.UPDATE_STUDENT_PHONE, SlotType.PHONE)
                        SueOperationResult.Prepare.Error(
                            SueOperationResult.ErrorType.UNKNOWN,
                            "¿Cuál es el nuevo teléfono para $studentName?"
                        )
                    } else {
                        conversationContext.lastMentionedPhone = null
                        studentTools.prepareUpdatePhoneAction(studentName, phoneText)
                    }
                }
            }

            IntentType.UPDATE_STUDENT_COURSE -> {
                val studentName = conversationContext.lastMentionedStudentName
                if (studentName == null) {
                    conversationContext.setAwaitingSlot(IntentType.UPDATE_STUDENT_COURSE, SlotType.STUDENT_NAME)
                    SueOperationResult.Prepare.Error(
                        SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
                        "¿De qué alumno quieres actualizar el curso?"
                    )
                } else {
                    val courseText = if (conversationContext.activeIncompleteIntent?.missingSlot == SlotType.COURSE && !entityExtractor.isQuestionOrQuery(lower)) {
                        query.trim()
                    } else {
                        conversationContext.lastMentionedCourse ?: entityExtractor.extractCourseText(query)
                    }
                    if (courseText.isNullOrBlank()) {
                        conversationContext.setAwaitingSlot(IntentType.UPDATE_STUDENT_COURSE, SlotType.COURSE)
                        SueOperationResult.Prepare.Error(
                            SueOperationResult.ErrorType.UNKNOWN,
                            "¿Cuál es el nuevo curso para $studentName?"
                        )
                    } else {
                        conversationContext.lastMentionedCourse = null
                        studentTools.prepareUpdateCourseAction(studentName, courseText)
                    }
                }
            }

            // READ QUERIES IMPLEMENTATION
            IntentType.QUERY_NEXT_CLASS -> {
                val nextClassResult = scheduleTools.getNextClass(conversationContext.lastMentionedStudentName)
                if (nextClassResult is SueOperationResult.NextClass && nextClassResult.schedule != null) {
                    val s = nextClassResult.schedule
                    conversationContext.lastMentionedStudentName = s.studentName
                    conversationContext.lastMentionedDayOfWeek = s.dayOfWeek
                    conversationContext.lastMentionedTime = s.startTime
                }
                nextClassResult
            }

            IntentType.QUERY_FREE_SLOTS -> {
                val day = conversationContext.lastMentionedDayOfWeek
                val professor = authRepository.currentUser.value
                val workingStart = professor?.workingStartTime?.takeIf { it.isNotBlank() } ?: "08:00"
                val workingEnd   = professor?.workingEndTime?.takeIf { it.isNotBlank() } ?: "23:00"
                scheduleTools.getFreeSlots(workingStart, workingEnd, day)
            }

            IntentType.QUERY_STUDENT_COUNT -> {
                studentTools.getActiveStudentCount()
            }

            IntentType.QUERY_STUDENT_DETAILS -> {
                val studentName = conversationContext.lastMentionedStudentName
                if (studentName == null) {
                    SueOperationResult.StudentDetails(query, emptyList())
                } else {
                    val detailsResult = studentTools.searchStudent(studentName)
                    if (detailsResult is SueOperationResult.StudentDetails && detailsResult.students.isNotEmpty()) {
                        val s = detailsResult.students.first()
                        conversationContext.lastMentionedStudentName = s.name
                        conversationContext.lastMentionedAmount = s.pendingBalance

                        val isSpecificAddressQuery = listOf(
                            "direccion", "dirección", "donde vive", "dónde vive", "portero", "portal", "timbre"
                        ).any { it in lower }
                        if (isSpecificAddressQuery) {
                            return if (s.address.isNotBlank()) {
                                SueOperationResult.ReadSuccess("La dirección de ${s.name} es: ${s.address}.")
                            } else {
                                SueOperationResult.ReadSuccess("${s.name} no tiene ninguna dirección registrada.")
                            }
                        }

                        val isSpecificPhoneQuery = listOf(
                            "telefono", "teléfono", "movil", "móvil", "celular"
                        ).any { it in lower }
                        if (isSpecificPhoneQuery) {
                            val phones = listOfNotNull(
                                s.studentPhone.takeIf { it.isNotBlank() }?.let { "Alumno: $it" },
                                s.parentPhones.takeIf { it.isNotBlank() }?.let { "Padres: $it" }
                            )
                            return if (phones.isNotEmpty()) {
                                SueOperationResult.ReadSuccess("Teléfonos de ${s.name}: ${phones.joinToString(", ")}.")
                            } else {
                                SueOperationResult.ReadSuccess("${s.name} no tiene ningún teléfono registrado.")
                            }
                        }
                    }
                    detailsResult
                }
            }

            IntentType.QUERY_DAY_SCHEDULE -> {
                val day = conversationContext.lastMentionedDayOfWeek ?: dateTimeProvider.getNow().dayOfWeek.value
                val timeFilter = entityExtractor.extractTime(lower) ?: entityExtractor.extractTimeOfDayFilter(lower)
                scheduleTools.getScheduleForDay(day, timeFilter)
            }

            IntentType.QUERY_STUDENT_WEEKLY_CLASSES -> {
                val studentName = conversationContext.lastMentionedStudentName
                if (studentName == null) {
                    SueOperationResult.ReadSuccess("No he identificado de qué alumno quieres saber las clases. ¿Puedes decirme su nombre?")
                } else {
                    val isPastClassesQuery = listOf(
                        "le he dado", "he dado", "dado", "dadas", "le di", "di a",
                        "impartido", "impartida", "impartidas", "impartidos", "llevo",
                        "given", "have i given", "did i give", "taught", "have i taught", "did i teach", "teach", "taught to", "teach to"
                    ).any { it in lower }
                    if (isPastClassesQuery) {
                        val logs = studentTools.getClassLogs(studentName)
                        val count = logs.size
                        val extraCount = logs.count { it.isExtra }
                        val regularCount = count - extraCount
                        val weeklyResult = scheduleTools.getWeeklyClassesForStudent(studentName)
                        val weeklyCount = if (weeklyResult is SueOperationResult.WeeklyClassesForStudent) weeklyResult.schedules.size else 0

                        val msg = buildString {
                            if (count == 0) {
                                append("No tienes registrada ninguna clase impartida aún a $studentName.")
                            } else {
                                val classWord = if (count == 1) "clase" else "clases"
                                append("Le has dado $count $classWord a $studentName")
                                if (extraCount > 0) {
                                    append(" ($regularCount ordinarias y $extraCount extra)")
                                }
                                append(".")
                            }
                            if (weeklyCount > 0) {
                                val weeklyWord = if (weeklyCount == 1) "clase semanal programada" else "clases semanales programadas"
                                append(" Actualmente tiene $weeklyCount $weeklyWord.")
                            }
                        }
                        SueOperationResult.ReadSuccess(msg)
                    } else {
                        scheduleTools.getWeeklyClassesForStudent(studentName)
                    }
                }
            }

            IntentType.QUERY_STUDENTS_WITH_BALANCE -> {
                studentTools.getStudentsWithBalance()
            }

            IntentType.QUERY_STUDENT_BALANCE -> {
                val studentName = conversationContext.lastMentionedStudentName
                if (studentName == null) {
                    SueOperationResult.ReadSuccess("¿De qué alumno quieres saber el saldo pendiente?")
                } else {
                    val details = studentTools.searchStudent(studentName)
                    if (details is SueOperationResult.StudentDetails && details.students.isNotEmpty()) {
                        val student = details.students.first()
                        conversationContext.lastMentionedStudentName = student.name
                        conversationContext.lastMentionedAmount = student.pendingBalance
                        if (student.pendingBalance <= 0.0) {
                            SueOperationResult.ReadSuccess("${student.name} está al corriente de pago. No tiene ningún saldo pendiente.")
                        } else {
                            SueOperationResult.ReadSuccess("${student.name} tiene un saldo pendiente de ${"%.2f".format(student.pendingBalance)} €.")
                        }
                    } else {
                        SueOperationResult.ReadSuccess("No he encontrado ningún alumno llamado \"$studentName\".")
                    }
                }
            }

            IntentType.QUERY_TODAY_SUMMARY -> {
                val today = dateTimeProvider.getNow().dayOfWeek.value
                val todayResult = scheduleTools.getScheduleForDay(today, null)
                val scheduleText = SueResponseFormatter.format(todayResult)

                val debtors = studentTools.getStudentsWithBalance()
                val debtorResult = debtors as? SueOperationResult.StudentsWithBalance

                val debtAlert = if (debtorResult != null && debtorResult.students.isNotEmpty()) {
                    val todayNames = if (todayResult is SueOperationResult.DaySchedule) {
                        todayResult.schedules.map { it.studentName.lowercase() }.toSet()
                    } else emptySet()
                    val debtorsToday = debtorResult.students.filter { it.name.lowercase() in todayNames }
                    if (debtorsToday.isNotEmpty()) {
                        "\n\n⚠️ Tienen saldo pendiente y clase hoy: " +
                        debtorsToday.joinToString(", ") { "${it.name} (${"%.2f".format(it.pendingBalance)} €)" }
                    } else ""
                } else ""

                SueOperationResult.ReadSuccess(scheduleText + debtAlert)
            }

            IntentType.QUERY_CANCELLED_CLASSES -> {
                val queryStudent = matchedStudent?.name
                val queryDay = relativeDay ?: explicitDay
                val typeFilter = when {
                    listOf("extra", "extras", "adicional", "adicionales").any { it in lower } -> ExceptionType.EXTRA
                    listOf("reprogramada", "reprogramadas", "reprogramado", "reprogramados", "movida", "movidas").any { it in lower } -> ExceptionType.RESCHEDULED
                    listOf("cancelada", "canceladas", "cancelado", "cancelados", "anulada", "anuladas", "anulado", "anulados").any { it in lower } -> ExceptionType.CANCELLED
                    else -> null
                }
                val cancelledDesc = scheduleTools.getCancelledClassesDescription(queryDay, queryStudent, typeFilter)
                if (cancelledDesc.isNotBlank()) {
                    SueOperationResult.ReadSuccess(cancelledDesc)
                } else {
                    val typeLabel = when (typeFilter) {
                        ExceptionType.EXTRA -> "clases extra registradas"
                        ExceptionType.RESCHEDULED -> "clases reprogramadas registradas"
                        ExceptionType.CANCELLED -> "clases canceladas registradas"
                        null -> "clases canceladas, reprogramadas ni extra registradas"
                    }
                    val dayLabel = if (queryDay != null) {
                        val dayNames = listOf("el lunes", "el martes", "el miércoles", "el jueves", "el viernes", "el sábado", "el domingo")
                        dayNames.getOrElse(queryDay - 1) { "el día seleccionado" }
                    } else if (queryStudent != null) {
                        "con $queryStudent"
                    } else "esta semana"
                    SueOperationResult.ReadSuccess("No hay $typeLabel $dayLabel.")
                }
            }
        }

        if (result is SueOperationResult.Prepare.Success) {
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
        return result
    }

    private fun containsScheduleKeywords(query: String) =
        listOf(
            "horario", "schedule", "clase", "clases", "class",
            "lunes", "martes", "miércoles", "miercoles", "jueves", "viernes",
            "sábado", "sabado", "domingo",
            "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
            "semana", "week", "hoy", "today", "mañana", "tomorrow", "tarde", "esta tarde",
            "ayer", "esta mañana", "luego", "que viene", "pasado", "el próximo", "la próxima",
            "próxima clase", "siguiente clase", "hueco", "libre"
        ).any { it in query }

    private fun containsNextClassKeywords(query: String) =
        listOf(
            "proxima clase", "próxima clase", "siguiente clase",
            "proxima", "próxima", "siguiente",
            "cuando tengo", "cuándo tengo",
            "cuando es", "cuándo es",
            "cuando empieza", "cuándo empieza",
            "next class"
        ).any { it in query }

    private fun containsFreeSlotKeywords(query: String): Boolean {
        val days = listOf("lunes", "martes", "miércoles", "miercoles", "jueves", "viernes", "sábado", "sabado", "domingo", "hoy", "mañana")
        if (days.any { it in query }) {
            return false
        }
        return listOf(
            "hueco libre", "huecos libres", "día libre", "días libres",
            "tengo libre", "estoy libre", "tengo disponible", "estoy disponible",
            "espacio libre", "rato libre", "momento libre",
            "free slot", "available slot", "am i free", "am i available"
        ).any { it in query }
    }

    private fun containsFreeSlotWithDayKeywords(query: String): Boolean {
        val hasFreeIntent = listOf(
            "hueco", "huecos", "libre", "libres", "disponible", "disponibles",
            "tiempo libre", "espacio", "rato", "momento", "free", "available"
        ).any { it in query }
        val hasDay = listOf(
            "lunes", "martes", "miércoles", "miercoles", "jueves", "viernes",
            "sábado", "sabado", "domingo", "hoy", "mañana", "esta tarde", "esta mañana", "tarde"
        ).any { it in query }
        return hasFreeIntent && hasDay
    }

    private fun containsCancelKeywords(query: String): Boolean {
        if (containsCancelledQueryKeywords(query)) {
            return false
        }
        if (listOf("cancelada", "canceladas", "cancelado", "cancelados", "cancelacion", "cancelación", "cancelaciones").any { it in query }) {
            return false
        }
        val directPhrases = listOf(
            "cancela ", "cancela la", "cancela el", "cancela a", "cancela con",
            "cancelar ", "cancelar la", "cancelar el", "cancelar a", "cancelar con",
            "cancelale", "cancélale", "cancelala", "cancélala",
            "anula ", "anula la", "anula el", "anula a", "anula con",
            "anular ", "anular la", "anular el", "anular a", "anular con",
            "anulale", "anúlale", "anulala", "anúlala",
            "quitar la clase", "quita la clase", "quitar clase", "quita clase",
            "quitarle la clase", "quítale la clase", "quitale la clase",
            "quítala", "quitala", "quítale", "quitale", "quitarle",
            "quita a ", "quitar a ", "quitale a ", "quítale a ",
            "quita con ", "quitar con ", "quitale con ", "quítale con ",
            "quita la de ", "quítale la de ", "quitale la de ",
            "borra la clase", "borrar la clase", "bórrale la clase", "borrale la clase",
            "elimina la clase", "eliminar la clase", "elimínale la clase", "eliminale la clase",
            "no voy a poder dar clase", "no puedo dar clase", "no podre dar clase", "no podré dar clase",
            "cancel ", "cancel the", "cancel a",
            "remove class", "delete class"
        )
        if (directPhrases.any { it in query }) return true

        val hasCancelVerb = listOf(
            "quita", "quitar", "quitale", "quítale",
            "cancela", "cancelar", "cancelale", "cancélale",
            "anula", "anular", "anulale", "anúlale"
        ).any { it in query }
        val hasClassWord = listOf("clase", "clases", "horario", "la de ").any { it in query }
        return hasCancelVerb && hasClassWord
    }

    private fun containsCancelledQueryKeywords(query: String): Boolean {
        val hasExceptionQuery = listOf(
            "cancelada", "canceladas", "cancelado", "cancelados",
            "reprogramada", "reprogramadas", "reprogramado", "reprogramados",
            "clase cancelada", "clases canceladas",
            "hay cancelada", "hay canceladas", "tengo cancelada", "tengo canceladas",
            "algo cancelado", "algo cancelada",
            "cancele", "cancelé", "he cancelado", "he cancelada",
            "anulada", "anuladas", "anulado", "anulados", "anule", "anulé",
            "cancelacion", "cancelación", "cancelaciones",
            "que cancelaciones", "qué cancelaciones", "cuales canceladas", "cuáles canceladas",
            "clase extra", "clases extra", "clases extras", "clase adicional", "clases adicionales",
            "reprogramaciones", "excepciones", "excepcion", "excepción"
        ).any { it in query }

        val isActionCommand = listOf(
            "cancela ", "cancela la", "cancela el", "cancela a",
            "cancelar ", "cancelar la", "cancelar el", "cancelar a",
            "anula ", "anula la", "anula el", "anula a",
            "añade una clase extra", "pon una clase extra", "agrega una clase extra",
            "añadir clase extra", "poner clase extra"
        ).any { it in query }

        return hasExceptionQuery && !isActionCommand
    }

    private fun containsRescheduleKeywords(query: String): Boolean {
        val specificKeywords = listOf(
            "mueve", "mover", "muevele", "muévele", "muevela", "muévela", "muevesela", "muévesela", "mueveme", "muéveme", "mueveles",
            "traslada", "trasladar", "trasladale", "trasládale",
            "reprograma", "reprogramar", "reprograme", "reprogramale", "reprográmale", "reprogramala", "reprográmala",
            "pospone", "posponer", "pospón", "pospon", "posponle", "pospónle",
            "adelanta", "adelantar", "adelantale", "adelántale",
            "pasa", "pasar", "pásame", "pasame", "pasale", "pásale", "pasala", "pásala",
            "reschedule", "postpone"
        )
        if (specificKeywords.any { it in query }) return true

        val hasChangeVerb = listOf(
            "cambia", "cambiar", "cambiale", "cámbiale", "cambiala", "cámbiala", "cambiame", "cámbiame", "change", "move"
        ).any { it in query }
        val hasScheduleContext = listOf(
            "la clase", "el horario", "la hora", "las horas", "la de ", "el de ",
            "al lunes", "al martes", "al miércoles", "al miercoles", "al jueves", "al viernes", "al sábado", "al sabado", "al domingo",
            "para el lunes", "para el martes", "para el miércoles", "para el miercoles", "para el jueves", "para el viernes", "para el sábado", "para el sabado", "para el domingo",
            "para hoy", "para mañana", "para manana",
            "a las ", "de las ",
            "class", "schedule", "the class",
            "to monday", "to tuesday", "to wednesday", "to thursday", "to friday", "to saturday", "to sunday",
            "to today", "to tomorrow",
            "to ", "at ", "from "
        ).any { it in query }
        return hasChangeVerb && hasScheduleContext
    }

    private fun containsRegisterPaymentKeywords(query: String): Boolean {
        val safeKeywords = listOf(
            "registra un pago", "registrar un pago",
            "ha pagado", "abonó", "abono", "pago de",
            "register payment", "made a payment", "payment of"
        )
        if (safeKeywords.any { it in query }) return true

        val hasPago = "pagó" in query || "paid" in query
        val hasTimeOrAmount = listOf(
            "hoy", "ayer", "esta semana", "acaba de", "ahora",
            "€", "euro", "euros", "dollar", "dollars"
        ).any { it in query }
        return hasPago && hasTimeOrAmount
    }

    private fun containsAddBalanceKeywords(query: String): Boolean {
        val hasVerb = listOf(
            "suma", "sumar", "anade", "añade", "anadir", "añadir",
            "agrega", "agregar", "sumale", "súmale", "add"
        ).any { it in query }
        val hasBalanceTarget = listOf("saldo", "deuda", "balance", "debt").any { it in query }
        if (hasVerb && hasBalanceTarget) return true

        return listOf(
            "suma saldo", "sumar saldo", "añade saldo", "anade saldo", "añadir saldo", "anadir saldo",
            "suma a la deuda", "añade a la deuda", "anade a la deuda", "súmale", "sumale",
            "add balance", "add to debt", "add to balance", "add to the debt"
        ).any { it in query }
    }

    private fun containsUpdateNotesKeywords(query: String): Boolean {
        val safeKeywords = listOf(
            "apunte", "apuntes", "observacion", "observaciones",
            "observation", "observations"
        )
        if (safeKeywords.any { it in query }) return true

        val hasNoteWord = listOf("nota", "notas", "comentario", "comentarios", "note", "notes", "comment", "comments").any { it in query }
        val hasWriteVerb = listOf(
            "añade", "añadir", "agrega", "agregar", "pon", "poner",
            "escribe", "escribir", "actualiza", "actualizar",
            "modifica", "modificar", "cambia", "cambiar",
            "add", "write", "update", "change"
        ).any { it in query }
        return hasNoteWord && hasWriteVerb
    }

    private fun containsUpdateAddressKeywords(query: String): Boolean {
        if (query.startsWith("cual es la direccion") || query.startsWith("cuál es la dirección") ||
            query.startsWith("dónde vive") || query.startsWith("donde vive")) {
            return false
        }

        val hasDirectAction = listOf(
            "cambia la direccion", "cambia la dirección",
            "cambiar la direccion", "cambiar la dirección",
            "cámbiale la direccion", "cámbiale la dirección", "cambiale la direccion", "cambiale la dirección",
            "actualiza la direccion", "actualiza la dirección",
            "actualizar la direccion", "actualizar la dirección",
            "actualízale la direccion", "actualízale la dirección", "actualizale la direccion", "actualizale la dirección",
            "modifica la direccion", "modifica la dirección",
            "modificar la direccion", "modificar la dirección",
            "modifícale la direccion", "modifícale la dirección", "modificale la direccion", "modificale la dirección",
            "ponle la direccion", "ponle la dirección", "pónle la direccion", "pónle la dirección",
            "pon la direccion", "pon la dirección",
            "pon de direccion", "pon de dirección",
            "nueva direccion", "nueva dirección",
            "edita la direccion", "edita la dirección",
            "editar la direccion", "editar la dirección",
            "edítale la direccion", "edítale la dirección", "editale la direccion", "editale la dirección",
            "guarda la direccion", "guarda la dirección",
            "guardar la direccion", "guardar la dirección",
            "guárdale la direccion", "guárdale la dirección", "guardale la direccion", "guardale la dirección",
            "anade la direccion", "añade la direccion", "anade la dirección", "añade la dirección",
            "anadir direccion", "añadir direccion", "anadir dirección", "añadir dirección",
            "añádele la direccion", "añádele la dirección", "añadele la direccion", "añadele la dirección",
            "update address", "change address", "set address", "edit address"
        ).any { it in query }

        val hasPorteroAction = listOf(
            "el portero de ", "portero de ", "pon el portero", "ponle el portero", "pónle el portero",
            "cambia el portero", "actualiza el portero", "edita el portero", "edítale el portero", "editale el portero",
            "añade el portero", "añádele el portero", "añadele el portero", "anade el portero",
            "el telefonillo de ", "pon el telefonillo", "ponle el telefonillo", "pónle el telefonillo",
            "cambia el telefonillo", "actualiza el telefonillo", "edita el telefonillo", "edítale el telefonillo",
            "añade el telefonillo", "añádele el telefonillo"
        ).any { it in query }

        val hasAddressStatement = (query.startsWith("la direccion de ") || query.startsWith("la dirección de ") ||
                query.startsWith("el portero de ") || query.startsWith("el telefonillo de ")) &&
                listOf(" es ", " seria ", " será ").any { it in query }

        val hasWriteVerb = listOf(
            "pon", "pone", "poner", "ponle", "pónle", "ponme", "pónme", "ponsela", "pónsela",
            "anade", "añade", "anadir", "añadir", "añádele", "añadele", "anadele", "anádele", "añadela", "añádela", "añademe", "añádeme",
            "agrega", "agregar", "agrégale", "agregale",
            "actualiza", "actualizar", "actualízale", "actualizale", "actualízala", "actualizala",
            "cambia", "cambiar", "cámbiale", "cambiale", "cámbiala", "cambiala",
            "modifica", "modificar", "modifícale", "modificale", "modifícala", "modificala",
            "guarda", "guardar", "guárdale", "guardale", "guárdala", "guardala",
            "edita", "editar", "edítale", "editale", "edítala", "editala", "edítame", "editame",
            "mete", "meter", "métele", "metele", "métela", "metela"
        ).any { it in query }

        val hasAddressTarget = listOf(
            "en su direccion", "en su dirección", "a su direccion", "a su dirección",
            "de direccion", "de dirección", "su direccion", "su dirección",
            "la direccion", "la dirección",
            "el portero", "de portero", "el telefonillo", "de telefonillo", "el portal", "el timbre", "el intercom",
            "codigo de portero", "código de portero", "codigo del portero", "código del portero",
            "codigo de porte", "código de porte", "codigo de acceso", "código de acceso",
            "codigo de portal", "código de portal", "codigo portal", "código portal"
        ).any { it in query }

        val isAddressUpdatePhrase = hasWriteVerb && hasAddressTarget

        val hasRelocationWord = listOf(
            "se ha mudado", "se muda", "se mudó", "se mudo",
            "ha cambiado de casa", "cambia de casa", "cambió de casa", "cambio de casa",
            "la nueva casa de", "nueva casa de", "su nueva casa es",
            "ahora vive en", "ha pasado a vivir en", "vive ahora en"
        ).any { it in query }

        return hasDirectAction || hasPorteroAction || hasAddressStatement || isAddressUpdatePhrase || hasRelocationWord
    }

    private fun containsUpdatePriceKeywords(query: String): Boolean {
        if (containsCreateStudentKeywords(query) || containsRegisterPaymentKeywords(query) || containsAddBalanceKeywords(query)) {
            return false
        }
        val hasPriceWord = listOf(
            "precio", "tarifa", "por hora", "/hora", "/h", "euros la hora", "euros hora", "la hora"
        ).any { it in query }

        val hasChangeVerb = listOf(
            "cambia", "cambiar", "cámbiale", "cambiale",
            "actualiza", "actualizar", "actualízale", "actualizale",
            "modifica", "modificar", "modifícale", "modificale",
            "pon", "pone", "poner", "ponle", "pónle",
            "edita", "editar", "edítale", "editale",
            "sube", "subir", "súbele", "subele",
            "baja", "bajar", "bájale", "bajale",
            "ajusta", "ajustar", "ajústale", "ajustale"
        ).any { it in query }

        val isPriceStatement = (query.startsWith("el precio de ") || query.startsWith("la tarifa de ")) &&
                listOf(" es ", " seria ", " será ", " pasa a ser ").any { it in query }

        return (hasPriceWord && hasChangeVerb) || isPriceStatement
    }

    private fun containsUpdatePhoneKeywords(query: String): Boolean {
        val hasPhoneWord = listOf(
            "telefono", "teléfono", "movil", "móvil", "tlf", "celular", "numero", "número"
        ).any { it in query }

        val hasChangeVerb = listOf(
            "cambia", "cambiar", "cámbiale", "cambiale",
            "actualiza", "actualizar", "actualízale", "actualizale",
            "modifica", "modificar", "modifícale", "modificale",
            "pon", "pone", "poner", "ponle", "pónle",
            "guarda", "guardar", "guárdale", "guardale",
            "edita", "editar", "edítale", "editale"
        ).any { it in query }

        val isPhoneStatement = listOf(
            "el telefono de ", "el teléfono de ", "el movil de ", "el móvil de ",
            "el nuevo telefono de ", "el nuevo teléfono de ", "el nuevo movil de ", "el nuevo móvil de ",
            "el numero de ", "el número de ", "el nuevo numero de ", "el nuevo número de ",
            "el celular de ", "el nuevo celular de "
        ).any { query.startsWith(it) } && listOf(" es ", " seria ", " será ", " es: ", ": ").any { it in query }

        return (hasPhoneWord && hasChangeVerb) || isPhoneStatement
    }

    private fun containsUpdateCourseKeywords(query: String): Boolean {
        if (containsCreateStudentKeywords(query)) return false
        val hasCourseWord = listOf(
            "curso", "nivel", "grado"
        ).any { it in query }

        val hasChangeVerb = listOf(
            "cambia", "cambiar", "cámbiale", "cambiale",
            "actualiza", "actualizar", "actualízale", "actualizale",
            "modifica", "modificar", "modifícale", "modificale",
            "pon", "pone", "poner", "ponle", "pónle",
            "edita", "editar", "edítale", "editale"
        ).any { it in query }

        val isCourseStatement = listOf(
            "el curso de ", "el nivel de ", "el grado de ",
            "el nuevo curso de ", "el nuevo nivel de ", "el nuevo grado de "
        ).any { query.startsWith(it) } && listOf(" es ", " seria ", " será ", " es: ", ": ").any { it in query }

        return (hasCourseWord && hasChangeVerb) || isCourseStatement
    }

    private fun containsStartClassKeywords(query: String) =
        listOf("inicia una clase", "inicia clase", "empieza clase", "comienza clase", "start class").any { it in query }

    private fun containsCreateStudentKeywords(query: String) =
        listOf(
            "crear un nuevo estudiante", "crear un nuevo alumno", "crear una nueva estudiante", "crear una nueva alumna",
            "crea un nuevo estudiante", "crea un nuevo alumno", "crea una nueva estudiante", "crea una nueva alumna",
            "crear un estudiante", "crear un alumno", "crea al alumno", "crea el alumno", "crea un alumno",
            "crea al estudiante", "crea el estudiante", "crea un estudiante", "crear al estudiante", "crear al alumno",
            "crear a un estudiante", "crear a un alumno", "crea a un estudiante", "crea a un alumno",
            "crea alumno", "crear alumno", "crea estudiante", "crear estudiante",
            "anade un alumno", "añade un alumno", "anade al alumno", "añade al alumno",
            "anade un estudiante", "añade un estudiante", "anade al estudiante", "añade al estudiante",
            "anadir estudiante", "añadir estudiante", "anadir alumno", "añadir alumno",
            "anade estudiante", "añade estudiante", "anade alumno", "añade alumno",
            "agrega un alumno", "agregar un alumno", "agrega al alumno", "agregar al alumno", "agrega alumno", "agregar alumno",
            "registra un alumno", "registrar un alumno", "registra al alumno", "registrar al alumno", "registra alumno", "registrar alumno",
            "un alumno nuevo", "un nuevo alumno", "una nueva alumna", "una alumna nueva",
            "nuevo alumno", "nuevo estudiante", "nueva estudiante", "nueva alumna",
            "create student", "add student"
        ).any { it in query }

    private fun containsDeleteStudentKeywords(query: String) =
        listOf("elimina al alumno", "elimina al estudiante", "borra al alumno", "borra al estudiante", "eliminar alumno", "delete student", "remove student").any { it in query }

    private fun containsAddExtraClassKeywords(query: String): Boolean {
        val hasWriteVerb = listOf(
            "anade", "añade", "anadir", "añadir", "agrega", "agregar",
            "pon", "pone", "poner", "ponle", "pónle", "ponme", "pónme", "ponles", "ponsela", "pónsela",
            "programa", "programar", "prográmale", "programale",
            "crea", "crear", "créale", "creale",
            "mete", "meter", "métele", "metele",
            "incluye", "incluir",
            "add", "create", "schedule", "insert"
        ).any { it in query }

        val hasExtra = listOf(
            "clase extra", "clases extra", "clases extras",
            "clase de refuerzo", "clases de refuerzo", "refuerzo",
            "clase adicional", "clases adicionales",
            "tutoria adicional", "tutoría adicional", "tutorias adicionales", "tutorías adicionales",
            "extra class", "extra classes", "additional class", "additional classes"
        ).any { it in query }

        val hasClassTarget = listOf(
            "una clase a ", "una clase para ", "una clase con ", "una clase de ",
            "clase a ", "clase para ", "clase con ", "clase de ",
            "una tutoria a ", "una tutoría a ", "tutoria a ", "tutoría a ",
            "tutoria con ", "tutoría con ", "tutoria para ", "tutoría para ",
            "a class for ", "a class to ", "a class with "
        ).any { it in query }

        val hasInvertedTarget = Regex("""\b(?:pon|ponle|pónle|ponme|añade|anade|agrega|programa)\s+a\s+\w+\s+(?:una\s+)?(?:clase|tutoria|tutoría)\b""").containsMatchIn(query)

        val hasDirectPonStudent = Regex("""\b(?:pon|ponle|pónle|ponme)\s+a\s+\w+\b""").containsMatchIn(query) &&
                (entityExtractor.extractDayOfWeek(query) != null || entityExtractor.extractRelativeDayOfWeek(query) != null || entityExtractor.extractTime(query) != null)

        val hasGenericScheduleIntent = hasWriteVerb && listOf("una clase", "clase").any { it in query } &&
                (entityExtractor.extractDayOfWeek(query) != null || entityExtractor.extractRelativeDayOfWeek(query) != null || entityExtractor.extractTime(query) != null)

        return hasWriteVerb && (hasExtra || hasClassTarget || hasInvertedTarget || hasDirectPonStudent || hasGenericScheduleIntent)
    }

    private fun containsCreateScheduleKeywords(query: String) =
        listOf(
            "anade un horario", "añade un horario", "anadir horario", "añadir horario",
            "programa una clase los", "crear horario", "create schedule", "add schedule"
        ).any { it in query }

    private fun containsDeleteScheduleKeywords(query: String) =
        listOf(
            "elimina el horario", "borra el horario",
            "quita el horario", "quitar el horario",
            "quitar horario", "delete schedule", "remove schedule"
        ).any { it in query }

    private fun containsStudentCountKeywords(query: String) =
        listOf("cuántos alumnos", "cuantos alumnos", "número de alumnos", "numero de alumnos", "cantidad de alumnos").any { it in query }

    private fun containsStudentDetailsKeywords(query: String) =
        listOf(
            "información de", "informacion de",
            "ficha de",
            "datos de",
            "detalles de",
            "info de",
            "dirección de", "direccion de",
            "dónde vive", "donde vive",
            "portero de", "portal de",
            "teléfono de", "telefono de",
            "contacto de"
        ).any { it in query }

    private fun containsStudentWeeklyClassesKeywords(query: String): Boolean {
        val hasQuantityWord = listOf("cuántas", "cuantas", "cuántos", "cuantos", "how many").any { it in query }
        val hasClassWord = listOf("clase", "clases", "sesión", "sesiones", "veces", "class", "classes", "sessions", "times").any { it in query }
        val hasStudentReference = listOf(
            "de ", "con ", "tiene ", "tengo de ", "tengo con ",
            " a ", "al ", "le he dado", "he dado", "dado a", "dadas a", "le di", "di a",
            "impartido", "impartidas", "llamado", "llamada", "alumno", "estudiante",
            "for ", "with ", "does ", "have for ", "given to ", "taught to ", "teach to ", "teach ", "to ", "student named ", "called "
        ).any { it in query }
        return hasQuantityWord && hasClassWord && hasStudentReference
    }

    private fun containsDayScheduleKeywords(query: String): Boolean {
        val excludes = listOf(
            "cancelada", "canceladas", "cancelado", "cancelados",
            "hueco", "huecos", "libre", "libres",
            "cuántas", "cuantas", "cuántos", "cuantos",
            "how many"
        )
        if (excludes.any { it in query }) {
            return false
        }
        return listOf(
            "clase", "clases", "horario", "lunes", "martes", "miércoles", "miercoles", "jueves", "viernes", "sábado", "sabado", "domingo", "hoy", "mañana",
            "class", "classes", "schedule", "today", "tomorrow"
        ).any { it in query }
    }

    private fun containsStudentsWithBalanceKeywords(query: String) =
        listOf(
            "quién me debe", "quien me debe",
            "quiénes me deben", "quienes me deben",
            "quién debe", "quien debe",
            "tienen deuda", "deben dinero",
            "deuda pendiente", "saldo pendiente",
            "pendiente de pago", "sin pagar",
            "no han pagado", "no ha pagado",
            "who owes me", "who hasn't paid"
        ).any { it in query }

    private fun containsStudentBalanceKeywords(query: String) =
        listOf(
            "cuánto me debe", "cuanto me debe",
            "cuánto debe", "cuanto debe",
            "qué me debe", "que me debe",
            "saldo de", "deuda de", "balance de",
            "cuánto tiene pendiente", "cuanto tiene pendiente",
            "how much does", "how much owes"
        ).any { it in query }

    private fun containsTodaySummaryKeywords(query: String) =
        listOf(
            "resumen de hoy", "resumen hoy",
            "cómo está mi día", "como esta mi dia",
            "cómo va mi día", "como va mi dia",
            "qué tengo hoy", "que tengo hoy",
            "summary today", "today's summary"
        ).any { it in query }
}
