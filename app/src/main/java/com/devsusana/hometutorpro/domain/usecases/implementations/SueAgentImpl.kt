package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.entities.PaymentType
import com.devsusana.hometutorpro.domain.entities.SueOperationResult
import com.devsusana.hometutorpro.domain.entities.SuePendingAction
import com.devsusana.hometutorpro.domain.repository.DateTimeProvider
import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.devsusana.hometutorpro.domain.usecases.ISueAgent
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.LocalTime
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestration layer for the Sue AI agent.
 *
 * Responsibilities:
 * 1. Detect intent from the user's query (keyword-based routing).
 * 2. Gather relevant tool context for LLM prompts.
 * 3. Detect schedule management intents (cancel/reschedule) and build
 *    [SuePendingAction] instances for the confirmation flow.
 * 4. Provide the system prompt that defines Sue's personality.
 */
@Singleton
class SueAgentImpl @Inject constructor(
    private val studentTools: StudentTools,
    private val scheduleTools: ScheduleTools,
    private val dateTimeProvider: DateTimeProvider,
    private val authRepository: AuthRepository
) : ISueAgent {

    companion object {

        /**
         * Builds the Sue system prompt with a locale-aware language instruction.
         *
         * @param locale The locale to format instructions for.
         */
        fun buildSystemPrompt(locale: Locale): String {
            val languageTag = locale.toLanguageTag() // e.g. "es-ES", "en-GB", "en-US"
            val languageName = locale.getDisplayLanguage(Locale.ENGLISH) // e.g. "Spanish"
            val countryName = if (locale.country.isNotEmpty()) {
                locale.getDisplayCountry(Locale.ENGLISH) // e.g. "Spain", "United Kingdom"
            } else null

            val languageInstruction = if (countryName != null) {
                "Always respond in $languageName as spoken in $countryName ($languageTag). " +
                "Use vocabulary, spelling and expressions appropriate for that region."
            } else {
                "Always respond in $languageName ($languageTag)."
            }

            return """
                Eres Sue, la asistente inteligente de HomeTutorPro. Tu rol es ayudar a profesores particulares a gestionar su trabajo de forma eficiente.

                Tu personalidad:
                - Eres profesional, amable y concisa.
                - Evitas respuestas largas — sé directa y útil.
                - Si no puedes ayudar con algo, dilo claramente.
                - NO te describes a ti misma como IA. Compórtate como la asistente Sue de forma natural.
                - $languageInstruction

                Tus capacidades:
                - Consultar la lista de alumnos (nombres, asignaturas, cursos, precios, saldos).
                - Buscar alumnos específicos por nombre.
                - Consultar los horarios semanales y la próxima clase.
                - Informar sobre saldos pendientes.
                - Contar alumnos activos.
                - Cancelar o mover clases puntuales (solo la ocurrencia indicada).
                - Mostrar huecos libres en la semana actual.
                - Crear o eliminar perfiles de alumnos.
                - Configurar o eliminar horarios recurrentes permanentes de clases.
                - Programar clases extra y registrar el inicio de clases en directo.

                Limitaciones:
                - Solo conoces los datos del profesor que está usando la app.
            """.trimIndent()
        }
    }    private enum class IntentType {
        START_CLASS,
        CREATE_STUDENT,
        DELETE_STUDENT,
        ADD_EXTRA_CLASS,
        CREATE_SCHEDULE,
        DELETE_SCHEDULE,
        CANCEL_CLASS,
        RESCHEDULE_CLASS,
        REGISTER_PAYMENT,
        ADD_BALANCE
    }

    private var lastMentionedStudentName: String? = null
    private var lastMentionedDayOfWeek: Int? = null
    private var lastMentionedTime: String? = null
    private var lastMentionedAmount: Double? = null
    private var lastMentionedDuration: Int? = null
    private var lastMentionedPrice: Double? = null
    private var lastMentionedSubjects: String? = null
    private var lastMentionedCourse: String? = null
    private var lastActiveIntentType: IntentType? = null

    override fun resetConversationContext() {
        lastMentionedStudentName = null
        lastMentionedDayOfWeek = null
        lastMentionedTime = null
        lastMentionedAmount = null
        lastMentionedDuration = null
        lastMentionedPrice = null
        lastMentionedSubjects = null
        lastMentionedCourse = null
        lastActiveIntentType = null
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Schedule management intent detection
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Detects an action intent (schedule or financial) from [query] and prepares the
     * corresponding [SueOperationResult.Prepare] with all data pre-resolved.
     *
     * Returns null if no action intent is detected, meaning the
     * caller should fall through to normal LLM processing.
     *
     * Returns [SueOperationResult.Prepare] with the resolved action and confirmation data,
     * or with an error descriptor if the intent was detected but the data could not be resolved.
     */
    override suspend fun detectActionIntent(query: String): SueOperationResult.Prepare? {
        val lower = query.lowercase().trim()

        // 1. If we have a pending intent type, check if the user wants to abort or change topic
        if (lastActiveIntentType != null) {
            val abortWords = listOf("no", "nada", "olvídalo", "olvida", "déjalo", "abortar", "aborta")
            if (abortWords.any { it == lower }) {
                resetConversationContext()
                return null
            }
            if (isQuestionOrQuery(lower)) {
                lastActiveIntentType = null
            }
        }

        // 2. Extract and update context memory variables
        val matchedStudent = studentTools.extractRelevantStudent(lower)
        val matchedName = matchedStudent?.name ?: extractStudentName(lower) ?: extractStudentNameForFinance(lower) ?: extractNameAfter(lower, listOf("inicia una clase para ", "inicia clase para ", "empieza clase para ", "start class for "))
        if (matchedName != null) {
            val capitalizedName = matchedName.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            lastMentionedStudentName = capitalizedName
        }

        val relativeDay = extractRelativeDayOfWeek(lower)
        val explicitDay = extractDayOfWeek(lower)
        val matchedDay = relativeDay ?: explicitDay
        if (matchedDay != null) {
            lastMentionedDayOfWeek = matchedDay
        }

        val matchedTime = extractTime(lower)
        if (matchedTime != null) {
            lastMentionedTime = matchedTime
        }

        val matchedAmount = extractAmount(lower)
        if (matchedAmount != null) {
            lastMentionedAmount = matchedAmount
        }

        val matchedDuration = extractDurationOptional(lower)
        if (matchedDuration != null) {
            lastMentionedDuration = matchedDuration
        }

        // 3. Determine the intent to process (explicit or fallback to last active intent)
        val explicitIntent = when {
            containsStartClassKeywords(lower) -> IntentType.START_CLASS
            containsCreateStudentKeywords(lower) -> IntentType.CREATE_STUDENT
            containsDeleteStudentKeywords(lower) -> IntentType.DELETE_STUDENT
            containsAddExtraClassKeywords(lower) -> IntentType.ADD_EXTRA_CLASS
            containsCreateScheduleKeywords(lower) -> IntentType.CREATE_SCHEDULE
            containsDeleteScheduleKeywords(lower) -> IntentType.DELETE_SCHEDULE
            containsCancelKeywords(lower) -> IntentType.CANCEL_CLASS
            containsRescheduleKeywords(lower) -> IntentType.RESCHEDULE_CLASS
            containsRegisterPaymentKeywords(lower) -> IntentType.REGISTER_PAYMENT
            containsAddBalanceKeywords(lower) -> IntentType.ADD_BALANCE
            else -> null
        }

        val activeIntent = explicitIntent ?: lastActiveIntentType
        if (activeIntent == null) {
            return null
        }

        // Set the active intent so if it fails (due to missing info), we persist it for the next turn
        lastActiveIntentType = activeIntent

        val result = when (activeIntent) {
            IntentType.START_CLASS -> {
                val studentName = lastMentionedStudentName
                if (studentName == null) {
                    SueOperationResult.Prepare.Error(
                        SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
                        "¿Para qué alumno quieres iniciar la clase?"
                    )
                } else {
                    val duration = lastMentionedDuration
                    if (duration == null) {
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
                val queryWithoutKeywords = lower
                    .replace("crear un estudiante", "")
                    .replace("crear un alumno", "")
                    .replace("crea un estudiante", "")
                    .replace("crea un alumno", "")
                    .replace("crear al estudiante", "")
                    .replace("crear al alumno", "")
                    .replace("crea al estudiante", "")
                    .replace("crea al alumno", "")
                    .replace("crear el estudiante", "")
                    .replace("crear el alumno", "")
                    .replace("crea el estudiante", "")
                    .replace("crea el alumno", "")
                    .replace("añadir un estudiante", "")
                    .replace("añadir un alumno", "")
                    .replace("añade un estudiante", "")
                    .replace("añade un alumno", "")
                    .replace("añadir al estudiante", "")
                    .replace("añadir al alumno", "")
                    .replace("añade al estudiante", "")
                    .replace("añade al alumno", "")
                    .replace("añadir estudiante", "")
                    .replace("añadir alumno", "")
                    .replace("añade estudiante", "")
                    .replace("añade alumno", "")
                    .replace("create student", "")
                    .replace("add student", "")
                    .trim()

                if (lastMentionedStudentName == null) {
                    val stopWords = listOf(" de ", " a ", " para ", " in ", " at ", " for ")
                    var nameCandidate = queryWithoutKeywords
                    for (stop in stopWords) {
                        val stopIdx = nameCandidate.indexOf(stop)
                        if (stopIdx >= 0) {
                            nameCandidate = nameCandidate.substring(0, stopIdx).trim()
                        }
                    }
                    if (nameCandidate.isNotBlank() && !nameCandidate.contains(Regex("""\d"""))) {
                        lastMentionedStudentName = nameCandidate.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                    }
                }

                val price = extractAmount(lower)
                if (price != null) {
                    lastMentionedPrice = price
                }

                val subjectKeywords = listOf(" de ", " in ", " para ", " for ")
                for (kw in subjectKeywords) {
                    val idx = lower.indexOf(kw)
                    if (idx >= 0) {
                        val rest = query.substring(idx + kw.length).trim()
                        val stopWords = listOf(" a ", " por ", " at ", " for ")
                        var end = rest.length
                        for (stop in stopWords) {
                            val stopIdx = rest.lowercase().indexOf(stop)
                            if (stopIdx in 1 until end) end = stopIdx
                        }
                        val candidate = rest.substring(0, end).trim()
                        if (candidate.isNotBlank() && !candidate.contains(Regex("""\d""")) && 
                            !listOf("lunes", "martes", "miércoles", "miercoles", "jueves", "viernes", "sábado", "sabado", "domingo").any { it in candidate.lowercase() }) {
                            lastMentionedSubjects = candidate
                            break
                        }
                    }
                }

                val courseKeywords = listOf("eso", "bachillerato", "bach", "primaria", "secundaria", "universidad")
                val courseMatch = courseKeywords.find { it in lower }
                if (courseMatch != null) {
                    lastMentionedCourse = courseMatch
                }

                val studentName = lastMentionedStudentName
                if (studentName.isNullOrBlank()) {
                    SueOperationResult.Prepare.Error(
                        SueOperationResult.ErrorType.UNKNOWN,
                        "¿Cómo se llama el nuevo alumno?"
                    )
                } else {
                    val priceVal = lastMentionedPrice
                    if (priceVal == null) {
                        SueOperationResult.Prepare.Error(
                            SueOperationResult.ErrorType.UNKNOWN,
                            "¿Cuál será el precio por hora de $studentName?"
                        )
                    } else {
                        val finalCourse = lastMentionedCourse ?: "Other"
                        val finalSubjects = lastMentionedSubjects ?: "General"
                        studentTools.prepareCreateStudent(studentName, finalCourse, finalSubjects, priceVal)
                    }
                }
            }

            IntentType.DELETE_STUDENT -> {
                val studentName = lastMentionedStudentName
                if (studentName == null) {
                    SueOperationResult.Prepare.Error(
                        SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
                        "¿De qué alumno quieres eliminar el perfil?"
                    )
                } else {
                    studentTools.prepareDeleteStudent(studentName)
                }
            }

            IntentType.ADD_EXTRA_CLASS -> {
                val studentName = lastMentionedStudentName
                if (studentName == null) {
                    SueOperationResult.Prepare.Error(
                        SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
                        "¿Para qué alumno quieres programar la clase extra?"
                    )
                } else {
                    val dayOfWeek = lastMentionedDayOfWeek ?: dateTimeProvider.getNow().dayOfWeek.value
                    val targetDate = dateTimeProvider.getNow().toLocalDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.of(dayOfWeek)))
                    val dateMillis = targetDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

                    val times = extractTwoTimes(lower) ?: lastMentionedTime?.let { lastTime ->
                        val parts = lastTime.split(":")
                        val h = parts[0].toInt()
                        val m = parts[1].toInt()
                        val endH = (h + 1) % 24
                        val endTime = "%02d:%02d".format(endH, m)
                        Pair(lastTime, endTime)
                    }
                    if (times == null) {
                        SueOperationResult.Prepare.Error(
                            SueOperationResult.ErrorType.UNKNOWN,
                            "¿A qué hora quieres programar la clase extra de $studentName?"
                        )
                    } else {
                        scheduleTools.prepareAddExtraClass(studentName, dateMillis, times.first, times.second)
                    }
                }
            }

            IntentType.CREATE_SCHEDULE -> {
                val studentName = lastMentionedStudentName
                if (studentName == null) {
                    SueOperationResult.Prepare.Error(
                        SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
                        "¿Para qué alumno deseas configurar el horario?"
                    )
                } else {
                    val dayOfWeek = lastMentionedDayOfWeek
                    if (dayOfWeek == null) {
                        SueOperationResult.Prepare.Error(
                            SueOperationResult.ErrorType.UNKNOWN,
                            "¿Qué día de la semana será la clase de $studentName?"
                        )
                    } else {
                        val times = extractTwoTimes(lower) ?: lastMentionedTime?.let { lastTime ->
                            val parts = lastTime.split(":")
                            val h = parts[0].toInt()
                            val m = parts[1].toInt()
                            val endH = (h + 1) % 24
                            val endTime = "%02d:%02d".format(endH, m)
                            Pair(lastTime, endTime)
                        }
                        if (times == null) {
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
                val studentName = lastMentionedStudentName
                if (studentName == null) {
                    SueOperationResult.Prepare.Error(
                        SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
                        "¿De qué alumno quieres eliminar el horario?"
                    )
                } else {
                    val dayOfWeek = lastMentionedDayOfWeek
                    if (dayOfWeek == null) {
                        SueOperationResult.Prepare.Error(
                            SueOperationResult.ErrorType.UNKNOWN,
                            "¿Qué día de la semana es el horario que quieres eliminar?"
                        )
                    } else {
                        val time = lastMentionedTime
                        scheduleTools.prepareDeleteSchedule(studentName, dayOfWeek, time)
                    }
                }
            }

            IntentType.CANCEL_CLASS -> {
                val studentName = lastMentionedStudentName
                if (studentName == null) {
                    SueOperationResult.Prepare.Error(
                        SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
                        "¿De qué alumno quieres cancelar la clase?"
                    )
                } else {
                    var dayOfWeek = lastMentionedDayOfWeek
                    val time = lastMentionedTime

                    if (dayOfWeek == null) {
                        val schedules = scheduleTools.getSchedulesByStudentName(studentName)
                        if (schedules.isEmpty()) {
                            SueOperationResult.Prepare.Error(
                                SueOperationResult.ErrorType.CLASS_NOT_FOUND,
                                "No he encontrado ninguna clase programada para $studentName."
                            )
                        } else if (schedules.size == 1) {
                            dayOfWeek = schedules.first().dayOfWeek
                            scheduleTools.prepareCancelAction(studentName, dayOfWeek, time)
                        } else {
                            SueOperationResult.Prepare.Error(
                                SueOperationResult.ErrorType.CLASS_NOT_FOUND,
                                "¿Qué día es la clase de $studentName que quieres cancelar?"
                            )
                        }
                    } else {
                        scheduleTools.prepareCancelAction(studentName, dayOfWeek, time)
                    }
                }
            }

            IntentType.RESCHEDULE_CLASS -> {
                val studentName = lastMentionedStudentName
                if (studentName == null) {
                    SueOperationResult.Prepare.Error(
                        SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
                        "¿De qué alumno quieres mover la clase?"
                    )
                } else {
                    val days = extractTwoDaysOfWeek(lower)
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
                                if (isTargetDay(lower, singleDay)) {
                                    toDay = singleDay
                                } else {
                                    fromDay = singleDay
                                }
                            } else {
                                toDay = singleDay
                            }
                        }

                        // Resolve fromDay if null
                        if (fromDay == null) {
                            if (schedules.size == 1) {
                                fromDay = schedules.first().dayOfWeek
                            } else {
                                val fallbackDay = lastMentionedDayOfWeek ?: dateTimeProvider.getNow().dayOfWeek.value
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
                            return SueOperationResult.Prepare.Error(
                                SueOperationResult.ErrorType.CLASS_NOT_FOUND,
                                "¿Qué día es la clase de $studentName que quieres mover?"
                            )
                        }

                        if (toDay == null) {
                            toDay = fromDay
                        }

                        // Resolve times
                        val times = extractAllTimesInQuery(lower)
                        var fromTime: String? = null
                        var targetTime: String? = null

                        if (times.size >= 2) {
                            fromTime = times[0]
                            targetTime = times[1]
                        } else if (times.size == 1) {
                            val singleTime = times[0]
                            val daySchedules = schedules.filter { it.dayOfWeek == fromDay }
                            val hasClassAtTime = daySchedules.any {
                                it.startTime == singleTime || it.startTime.substringBefore(":") == singleTime.substringBefore(":")
                            }
                            if (hasClassAtTime) {
                                fromTime = singleTime
                                targetTime = singleTime // move day, keep same time
                            } else {
                                fromTime = null
                                targetTime = singleTime // move to new time
                            }
                        } else {
                            fromTime = null
                            targetTime = lastMentionedTime
                        }

                        // If targetTime is still null, default to the original class start time on fromDay!
                        if (targetTime == null) {
                            val daySchedules = schedules.filter { it.dayOfWeek == fromDay }
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

            IntentType.REGISTER_PAYMENT -> {
                val studentName = lastMentionedStudentName
                if (studentName == null) {
                    SueOperationResult.Prepare.Error(
                        SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
                        "¿De qué alumno quieres registrar el pago?"
                    )
                } else {
                    val amount = lastMentionedAmount
                    if (amount == null) {
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
                val studentName = lastMentionedStudentName
                if (studentName == null) {
                    SueOperationResult.Prepare.Error(
                        SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
                        "¿A qué alumno le quieres sumar saldo?"
                    )
                } else {
                    val amount = lastMentionedAmount
                    if (amount == null) {
                        SueOperationResult.Prepare.Error(
                            SueOperationResult.ErrorType.UNKNOWN,
                            "¿Cuánto saldo deseas sumarle a $studentName?"
                        )
                    } else {
                        studentTools.prepareAddBalance(studentName, amount)
                    }
                }
            }
        }

        if (result is SueOperationResult.Prepare.Success) {
            lastActiveIntentType = null
        }
        return result
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Normal LLM prompt building
    // ──────────────────────────────────────────────────────────────────────────

    override suspend fun buildPromptWithContext(
        userQuery: String,
        history: List<Pair<String, String>>
    ): String {
        val toolContext = gatherRelevantContext(userQuery)
        val locale = dateTimeProvider.getLocale()

        // Format date/time in the device locale so it reads naturally
        val formatter = java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy, HH:mm", locale)
        val currentDateTime = dateTimeProvider.getNow().format(formatter)

        return buildString {
            appendLine(buildSystemPrompt(locale))
            appendLine()
            appendLine("--- TEMPORAL CONTEXT ---")
            appendLine("Current date/time: $currentDateTime")
            appendLine("--- END OF TEMPORAL CONTEXT ---")
            appendLine()
            if (history.isNotEmpty()) {
                appendLine("--- RECENT CONVERSATION HISTORY ---")
                for ((usr, bot) in history.takeLast(10)) {
                    appendLine("User: $usr")
                    appendLine("Sue: $bot")
                }
                appendLine("--- END OF HISTORY ---")
                appendLine()
            }
            if (toolContext.isNotBlank()) {
                appendLine("--- AVAILABLE DATA ---")
                appendLine(toolContext)
                appendLine("--- END OF DATA ---")
            }
            appendLine()
            appendLine("User query: $userQuery")
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Context gathering — BUG FIX: removed the fallback that dumped all students
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Determines which tool context to inject based on the user's query.
     *
     * This is a `suspend` function because the tool methods perform database I/O.
     *
     * **Bug-fix:** The previous implementation had a fallback `if (isEmpty())`
     * block that called [StudentTools.getAllStudentsSummary] for ANY unrecognised
     * query (including questions about a specific student where the name wasn't
     * detected). The new version returns an empty string when no keyword matches,
     * letting the LLM respond gracefully without raw data.
     */
    private suspend fun gatherRelevantContext(query: String): String {
        val lowerQuery = query.lowercase()

        // 1. Extract and update context memory variables
        val matchedStudent = studentTools.extractRelevantStudent(lowerQuery)
        val studentName = matchedStudent?.name ?: extractStudentName(lowerQuery) ?: extractStudentNameForFinance(lowerQuery)
        if (studentName != null) {
            val capitalizedName = studentName.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            lastMentionedStudentName = capitalizedName
        }

        val relativeDay = extractRelativeDayOfWeek(lowerQuery)
        val explicitDay = extractDayOfWeek(lowerQuery)
        val day = relativeDay ?: explicitDay
        if (day != null) {
            lastMentionedDayOfWeek = day
        }

        val time = extractTime(lowerQuery)
        if (time != null) {
            lastMentionedTime = time
        }

        return buildString {
            // 1. Schedule queries
            if (containsScheduleKeywords(lowerQuery)) {
                if (containsNextClassKeywords(lowerQuery)) {
                    appendLine(formatResult(scheduleTools.getNextClass()))
                } else if (containsFreeSlotKeywords(lowerQuery)) {
                    appendLine(formatResult(scheduleTools.getFreeSlots()))
                } else {
                    val daysToInject = mutableSetOf<Int>()
                    daysToInject.add(dateTimeProvider.getNow().dayOfWeek.value)

                    if (day != null) {
                        daysToInject.add(day)
                    }
                    if (lastMentionedDayOfWeek != null) {
                        daysToInject.add(lastMentionedDayOfWeek!!)
                    }

                    val extractedDays = extractTwoDaysOfWeek(lowerQuery)
                    if (extractedDays.first != null) daysToInject.add(extractedDays.first!!)
                    if (extractedDays.second != null) daysToInject.add(extractedDays.second!!)

                    val timeFilter = extractTime(lowerQuery) ?: extractTimeOfDayFilter(lowerQuery)
                    if (day == null && timeFilter != null && timeFilter.contains(":")) {
                        val lookupName = lastMentionedStudentName
                        if (lookupName != null) {
                            val studentSchedules = scheduleTools.getSchedulesByStudentName(lookupName)
                            val matchingSchedule = studentSchedules.firstOrNull { it.startTime == timeFilter }
                            if (matchingSchedule != null) {
                                daysToInject.add(matchingSchedule.dayOfWeek)
                            }
                        }
                    }

                    for (d in daysToInject.sorted()) {
                        appendLine(formatResult(scheduleTools.getScheduleForDay(d, timeFilter)))
                    }
                }
            }

            // 2. Student queries — mutually exclusive branches to avoid data dumping
            val hasSchedule = containsScheduleKeywords(lowerQuery)

            when {
                lastMentionedStudentName != null ->
                    appendLine(formatResult(studentTools.searchStudent(lastMentionedStudentName!!)))

                containsCountKeywords(lowerQuery) ->
                    appendLine(formatResult(studentTools.getActiveStudentCount()))

                containsBalanceKeywords(lowerQuery) ->
                    appendLine(formatResult(studentTools.getStudentsWithBalance()))

                containsAllStudentsKeywords(lowerQuery) && !hasSchedule ->
                    appendLine(formatResult(studentTools.getAllStudentsSummary()))
                // No else-branch — if nothing matched, no context is injected.
            }
        }
    }

    /**
     * Converts a [SueOperationResult] to a string suitable for LLM context injection.
     * Delegates to the presentation formatter which holds all localized strings.
     */
    private fun formatResult(result: SueOperationResult): String =
        com.devsusana.hometutorpro.presentation.sue.SueResponseFormatter.format(result)

    // ──────────────────────────────────────────────────────────────────────────
    // Keyword helpers
    // ──────────────────────────────────────────────────────────────────────────

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
            "próxima clase", "siguiente clase", "próxima", "siguiente",
            "cuándo tengo", "cuándo es", "cuándo empieza", "next class"
        ).any { it in query }

    private fun containsFreeSlotKeywords(query: String) =
        listOf(
            "hueco libre", "huecos libres", "día libre", "días libres",
            "disponible", "disponibles", "libre", "libres", "espacio libre",
            "free slot", "available"
        ).any { it in query }

    private fun containsCancelKeywords(query: String) =
        listOf("cancela", "cancelar", "anula", "anular", "quitar la clase", "quita la clase",
               "cancel", "remove class", "delete class")
            .any { it in query }

    private fun containsRescheduleKeywords(query: String) =
        listOf("mueve", "mover", "cambia", "cambiar", "pasa", "pasar", "traslada", "trasladar",
               "reprograma", "reprogramar", "reprograme", "pospone", "posponer", "pospón", "adelanta", "adelantar",
               "move", "change", "reschedule", "postpone")
            .any { it in query }

    private fun containsRegisterPaymentKeywords(query: String) =
        listOf("registra un pago", "registrar un pago", "ha pagado", "abonó", "abono", "pagó", "pago de",
               "register payment", "made a payment", "paid", "payment of")
            .any { it in query }

    private fun containsAddBalanceKeywords(query: String) =
        listOf("suma saldo", "sumar saldo", "añade saldo", "añadir saldo", "suma a la deuda", "añade a la deuda", "súmale", "sumale",
               "add balance", "add to debt", "add to balance", "add to the debt")
            .any { it in query }

    private fun containsAllStudentsKeywords(query: String) =
        listOf(
            "alumno", "alumnos", "estudiante", "estudiantes",
            "student", "students", "quién", "quien", "lista", "list",
            "todos", "all"
        ).any { it in query }

    private fun containsBalanceKeywords(query: String) =
        listOf(
            "saldo", "balance", "deuda", "debt", "dinero", "money",
            "cobrar", "pagar", "pago", "payment", "pendiente", "pending", "debe", "owe"
        ).any { it in query }

    private fun containsCountKeywords(query: String) =
        listOf(
            "cuántos", "cuantos", "how many", "count",
            "total", "número", "numero", "number", "cuánta", "cuenta"
        ).any { it in query }

    private fun containsStartClassKeywords(query: String) =
        listOf("inicia una clase", "inicia clase", "empieza clase", "comienza clase", "start class").any { it in query }

    private fun containsCreateStudentKeywords(query: String) =
        listOf("crea un alumno", "crear un alumno", "crea al alumno", "crea el alumno", "añade al alumno", "añade al estudiante", "añadir estudiante", "create student", "add student").any { it in query }

    private fun containsDeleteStudentKeywords(query: String) =
        listOf("elimina al alumno", "elimina al estudiante", "borra al alumno", "borra al estudiante", "eliminar alumno", "delete student", "remove student").any { it in query }

    private fun containsAddExtraClassKeywords(query: String) =
        listOf("clase extra", "clase adicional", "tutoría adicional", "tutoria adicional", "extra class", "additional class").any { it in query }

    private fun containsCreateScheduleKeywords(query: String) =
        listOf("añade un horario", "añadir horario", "programa una clase los", "crear horario", "create schedule", "add schedule").any { it in query }

    private fun containsDeleteScheduleKeywords(query: String) =
        listOf("elimina el horario", "borra el horario", "quitar horario", "delete schedule", "remove schedule").any { it in query }

    // ──────────────────────────────────────────────────────────────────────────
    // Extraction helpers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Extracts ISO day-of-week (1=Monday … 7=Sunday) from the query, or null.
     */
    fun extractDayOfWeek(query: String): Int? {
        val dayMappings = mapOf(
            "lunes" to 1, "monday" to 1,
            "martes" to 2, "tuesday" to 2,
            "miércoles" to 3, "miercoles" to 3, "wednesday" to 3,
            "jueves" to 4, "thursday" to 4,
            "viernes" to 5, "friday" to 5,
            "sábado" to 6, "sabado" to 6, "saturday" to 6,
            "domingo" to 7, "sunday" to 7
        )
        return dayMappings.entries.firstOrNull { it.key in query }?.value
    }

    /**
     * Resolves relative time references (hoy, mañana, ayer, today, tomorrow, yesterday)
     * to an ISO day-of-week number (1=Monday … 7=Sunday).
     * Returns null if no relative reference is found.
     */
    private fun extractRelativeDayOfWeek(query: String): Int? {
        val today = dateTimeProvider.getNow().dayOfWeek.value
        return when {
            "hoy" in query || "today" in query ||
            "esta tarde" in query || "esta mañana" in query ||
            "esta noche" in query || "tonight" in query ||
            "luego" in query || "later" in query -> today

            "mañana" in query || "tomorrow" in query -> (today % 7) + 1

            "ayer" in query || "yesterday" in query -> if (today == 1) 7 else today - 1

            else -> null
        }
    }

    /**
     * Detects a time-of-day filter from the query.
     * Returns "morning", "afternoon", or null (all day).
     */
    fun extractTimeOfDayFilter(query: String): String? = when {
        "tarde" in query || "afternoon" in query || "evening" in query -> "afternoon"
        "mañana" in query && ("esta" in query || "por la" in query) -> "morning"
        "morning" in query -> "morning"
        else -> null
    }


    /**
     * Extracts two days of the week from a reschedule query (from/to).
     * Returns (fromDay, toDay) where each can be null if not found.
     */
    private fun extractTwoDaysOfWeek(query: String): Pair<Int?, Int?> {
        val today = dateTimeProvider.getNow().dayOfWeek.value
        val tomorrow = (today % 7) + 1
        val yesterday = if (today == 1) 7 else today - 1

        val rawKeywords = listOf(
            "esta mañana" to today,
            "esta tarde" to today,
            "esta noche" to today,
            "tonight" to today,
            "tomorrow" to tomorrow,
            "mañana" to tomorrow,
            "yesterday" to yesterday,
            "ayer" to yesterday,
            "later" to today,
            "luego" to today,
            "today" to today,
            "hoy" to today,
            "lunes" to 1, "monday" to 1,
            "martes" to 2, "tuesday" to 2,
            "miércoles" to 3, "miercoles" to 3, "wednesday" to 3,
            "jueves" to 4, "thursday" to 4,
            "viernes" to 5, "friday" to 5,
            "sábado" to 6, "sabado" to 6, "saturday" to 6,
            "domingo" to 7, "sunday" to 7
        )

        val allKeywords = rawKeywords.sortedByDescending { it.first.length }
        val matchedIndices = BooleanArray(query.length)
        val matches = mutableListOf<Pair<Int, Int>>() // Pair(startIndex, dayOfWeek)

        for ((keyword, day) in allKeywords) {
            var idx = query.indexOf(keyword)
            while (idx >= 0) {
                val endIdx = idx + keyword.length
                var alreadyMatched = false
                for (i in idx until endIdx) {
                    if (matchedIndices[i]) {
                        alreadyMatched = true
                        break
                    }
                }
                if (!alreadyMatched) {
                    for (i in idx until endIdx) {
                        matchedIndices[i] = true
                    }
                    matches.add(Pair(idx, day))
                }
                idx = query.indexOf(keyword, idx + 1)
            }
        }

        val sortedMatches = matches.sortedBy { it.first }
        val distinctDays = mutableListOf<Int>()
        for (m in sortedMatches) {
            if (m.second !in distinctDays) {
                distinctDays.add(m.second)
            }
        }
        return Pair(distinctDays.getOrNull(0), distinctDays.getOrNull(1))
    }

    /**
     * Attempts to extract a student name from the query by matching against
     * any known student's first name. Returns the matched full name or null.
     */
    private fun extractStudentName(query: String): String? {
        val commonConnectors = listOf(
            "cancela la clase de ", "cancelar la clase de ", "anula la clase de ",
            "anular la clase de ", "mueve la clase de ", "mover la clase de ",
            "cambia la clase de ", "cambiar la clase de ", "pasa la clase de ",
            "traslada la clase de ", "la clase de ",
            "cancela la clase con ", "cancelar la clase con ", "mueve la clase con ",
            "mover la clase con ", "cambia la clase con ", "cambiar la clase con ",
            "cancela a ", "cancelar a ", "anula a ", "anular a ",
            "mueve a ", "mover a ", "cambia a ", "cambiar a ",
            "elimina el horario de ", "borra el horario de ", "eliminar el horario de ",
            "quitar el horario de ", "quita el horario de ",
            "añade un horario para ", "añadir un horario para ", "crea un horario para ",
            "crear un horario para ", "horario para ", "horario de ",
            "elimina al alumno ", "eliminar al alumno ", "borra al alumno ", "borrar al alumno ",
            "elimina a ", "eliminar a ", "borra a ", "borrar a ",
            "cancel class for ", "cancel the class of ", "move class for ",
            "reschedule class for ", "change class for ", "the class of ",
            "cancel class with ", "cancel the class with ", "move class with ",
            "change class with "
        )
        for (connector in commonConnectors) {
            val idx = query.indexOf(connector)
            if (idx >= 0) {
                val rest = query.substring(idx + connector.length).trim()
                // Take up to the next preposition or day keyword
                val stopWords = listOf(" del ", " de ", " el ", " al ", " a ", " en ", " los ", " las ", " con ",
                                       " from ", " of ", " the ", " to ", " at ", " in ", " on ")
                var end = rest.length
                for (stop in stopWords) {
                    val stopIdx = rest.indexOf(stop)
                    if (stopIdx in 1 until end) end = stopIdx
                }
                val name = rest.substring(0, end).trim()
                if (name.isNotBlank()) {
                    val dayKeywords = setOf(
                        "lunes", "martes", "miércoles", "miercoles", "jueves", "viernes", "sábado", "sabado", "domingo",
                        "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
                        "hoy", "mañana", "tomorrow", "esta tarde", "esta mañana", "esta noche", "luego", "ayer", "yesterday",
                        "tarde", "mañana"
                    )
                    if (name.lowercase() in dayKeywords || dayKeywords.any { name.lowercase().contains(it) }) {
                        continue
                    }
                    return name
                }
            }
        }
        return null
    }

    /**
     * Resolves AM/PM ambiguity for 12-hour values (1..8) to PM.
     */
    private fun resolveHour(h: Int, query: String): Int {
        val professor = authRepository.currentUser.value
        val startStr = professor?.workingStartTime ?: "08:00"
        val endStr = professor?.workingEndTime ?: "23:00"
        return resolveHourSmart(h, query, startStr, endStr)
    }

    private fun resolveHourSmart(h: Int, query: String, workingStartTime: String, workingEndTime: String): Int {
        if (h == 0 || h > 12) {
            return h
        }
        val lower = query.lowercase()

        // 1. Explicit morning keywords (AM)
        val morningKeywords = listOf("esta mañana", "por la mañana", "de la mañana", "la mañana de", "madrugada", "am", "a.m.", "morning")
        if (morningKeywords.any { it in lower }) {
            return if (h == 12) 0 else h
        }

        // 2. Explicit afternoon/night keywords (PM)
        val afternoonKeywords = listOf("esta tarde", "esta noche", "por la tarde", "por la noche", "de la tarde", "de la noche", "tarde", "noche", "pm", "p.m.", "afternoon", "evening", "night")
        if (afternoonKeywords.any { it in lower }) {
            return if (h == 12) 12 else h + 12
        }

        // 3. Check working hours fit
        val workStart = try { LocalTime.parse(workingStartTime) } catch (e: Exception) { LocalTime.of(8, 0) }
        val workEnd = try { LocalTime.parse(workingEndTime) } catch (e: Exception) { LocalTime.of(23, 0) }

        val amTime = LocalTime.of(if (h == 12) 0 else h, 0)
        val pmTime = LocalTime.of(if (h == 12) 12 else h + 12, 0)

        val amFits = !amTime.isBefore(workStart) && !amTime.isAfter(workEnd)
        val pmFits = !pmTime.isBefore(workStart) && !pmTime.isAfter(workEnd)

        if (amFits && !pmFits) {
            return if (h == 12) 0 else h
        }
        if (pmFits && !amFits) {
            return if (h == 12) 12 else h + 12
        }

        // 4. Default fallback convention
        if (h in 1..8) {
            return h + 12 // PM (13:00 to 20:00)
        }
        return h // AM (9:00 to 12:00)
    }

    /**
     * Attempts to extract a time in "HH:mm" format from the query.
     * Recognises patterns like "a las 11:00", "a las 11", "11:00", "11h".
     */
    private fun extractTime(query: String): String? {
        val lower = query.lowercase()
        // Pattern: HH:mm
        val colonPattern = Regex("""\b(\d{1,2}):(\d{2})\b""")
        colonPattern.find(lower)?.let { match ->
            val h = match.groupValues[1].toInt()
            val m = match.groupValues[2].toInt()
            if (h in 0..23 && m in 0..59) {
                val resolvedH = resolveHour(h, lower)
                return "%02d:%02d".format(resolvedH, m)
            }
        }
        // Pattern: "a las NN", "las NN", "a la NN", "la NN" (whole hours)
        val hourPattern = Regex("""\b(?:a\s+)?las\s+(\d{1,2})\b|\b(?:a\s+)?la\s+(\d{1,2})\b""")
        hourPattern.find(lower)?.let { match ->
            val hStr = match.groupValues[1].takeIf { it.isNotEmpty() } ?: match.groupValues[2]
            val h = hStr.toIntOrNull()
            if (h != null && h in 0..23) {
                val resolvedH = resolveHour(h, lower)
                return "%02d:00".format(resolvedH)
            }
        }
        return null
    }

    /**
     * Attempts to extract a student name for financial intents.
     */
    private fun extractStudentNameForFinance(query: String): String? {
        val commonConnectors = listOf(
            "pagado por ", "deuda de ", "saldo de ", "a la cuenta de ",
            "a la deuda de ", "sumale a ", "súmale a ",
            "paid by ", "debt of ", "balance of ", "to the account of ", "to the debt of ", "add to "
        )
        // First try explicit connectors that don't usually precede numbers
        for (connector in commonConnectors) {
            val idx = query.indexOf(connector)
            if (idx >= 0) {
                val rest = query.substring(idx + connector.length).trim()
                val stopWords = listOf(" en ", " con ", " por ", " el ", " los ",
                                       " in ", " with ", " by ", " the ", " for ")
                var end = rest.length
                for (stop in stopWords) {
                    val stopIdx = rest.indexOf(stop)
                    if (stopIdx in 1 until end) end = stopIdx
                }
                val name = rest.substring(0, end).trim()
                if (name.isNotBlank()) return name
            }
        }
        
        // Fallback for "de " which might appear after the amount
        // Example: "registra un pago de 20 euros de María"
        val deIdx = query.lastIndexOf(" de ")
        val ofIdx = query.lastIndexOf(" of ")
        val splitIdx = if (deIdx >= 0) deIdx + 4 else if (ofIdx >= 0) ofIdx + 4 else -1
        
        if (splitIdx >= 0) {
            val name = query.substring(splitIdx).trim()
            if (name.isNotBlank() && !name.first().isDigit()) {
                val dayKeywords = listOf(
                    "lunes", "martes", "miércoles", "miercoles", "jueves", "viernes", "sábado", "sabado", "domingo",
                    "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
                    "hoy", "mañana", "tomorrow", "esta tarde", "esta mañana", "esta noche", "luego", "ayer", "yesterday"
                )
                if (dayKeywords.any { name.lowercase().contains(it) || it.contains(name.lowercase()) }) {
                    return null
                }
                return name
            }
        }
        
        return null
    }

    /**
     * Extracts a numeric amount (e.g. 20, 15.5) from the query.
     */
    private fun extractAmount(query: String): Double? {
        val amountPattern = Regex("""\b(\d+(?:[.,]\d{1,2})?)\s*(?:euros?|€|pavitos|pavos|dollars?|\$)\b""")
        amountPattern.find(query)?.let { match ->
            return match.groupValues[1].replace(',', '.').toDoubleOrNull()
        }
        
        // Fallback for just a number after payment verbs
        val barePattern = Regex("""(?:pago de|sumar|sumale|súmale|añadir|añade|payment of|add)\s+(\d+(?:[.,]\d{1,2})?)\b""")
        barePattern.find(query)?.let { match ->
            return match.groupValues[1].replace(',', '.').toDoubleOrNull()
        }
        return null
    }

    private fun extractNameAfter(query: String, keywords: List<String>): String? {
        for (keyword in keywords) {
            val idx = query.indexOf(keyword)
            if (idx >= 0) {
                val rest = query.substring(idx + keyword.length).trim()
                val word = rest.substringBefore(" ").trim()
                if (word.isNotBlank()) return word
            }
        }
        return null
    }

    private fun extractDurationOptional(query: String): Int? {
        val pattern = Regex("""\b(\d+)\s*(?:minutos?|min|m)\b""")
        pattern.find(query)?.let { match ->
            return match.groupValues[1].toIntOrNull()
        }
        val hourPattern = Regex("""\b(\d+)\s*(?:horas?|h)\b""")
        hourPattern.find(query)?.let { match ->
            val h = match.groupValues[1].toDoubleOrNull() ?: 1.0
            return (h * 60).toInt()
        }
        return null
    }

    private fun extractDuration(query: String): Int {
        return extractDurationOptional(query) ?: 60
    }

    private fun parseCreateStudent(query: String): SuePendingAction.CreateStudent? {
        val lower = query.lowercase()
        val nameKeywords = listOf(
            "crea al alumno ", "crea el alumno ", "crea al estudiante ", "crea el estudiante ",
            "crear al alumno ", "crear el alumno ", "añade al alumno ", "añade al estudiante ",
            "create student ", "add student "
        )
        var name = ""
        for (kw in nameKeywords) {
            val idx = lower.indexOf(kw)
            if (idx >= 0) {
                val rest = query.substring(idx + kw.length).trim()
                val stopWords = listOf(" de ", " a ", " para ", " in ", " at ", " for ")
                var end = rest.length
                for (stop in stopWords) {
                    val stopIdx = rest.lowercase().indexOf(stop)
                    if (stopIdx in 1 until end) end = stopIdx
                }
                name = rest.substring(0, end).trim()
                break
            }
        }
        if (name.isBlank()) return null
        
        val price = extractAmount(lower) ?: 15.0
        
        var subjects = "General"
        val subjectKeywords = listOf(" de ", " in ", " para ", " for ")
        for (kw in subjectKeywords) {
            val idx = lower.indexOf(kw)
            if (idx >= 0) {
                val rest = query.substring(idx + kw.length).trim()
                val stopWords = listOf(" a ", " por ", " at ", " for ")
                var end = rest.length
                for (stop in stopWords) {
                    val stopIdx = rest.lowercase().indexOf(stop)
                    if (stopIdx in 1 until end) end = stopIdx
                }
                val candidate = rest.substring(0, end).trim()
                if (candidate.isNotBlank() && !candidate.contains(Regex("""\d"""))) {
                    subjects = candidate
                    break
                }
            }
        }
        
        val courseKeywords = listOf("eso", "bachillerato", "bach", "primaria", "secundaria", "universidad")
        val course = courseKeywords.find { it in lower } ?: "Other"
        
        return SuePendingAction.CreateStudent(name, course, subjects, price)
    }

    private fun extractTwoTimes(query: String): Pair<String, String>? {
        val lower = query.lowercase()
        val timePattern = Regex("""\b(\d{1,2}):(\d{2})\b""")
        val matches = timePattern.findAll(lower).toList()
        if (matches.size >= 2) {
            val h1 = matches[0].groupValues[1].toInt()
            val m1 = matches[0].groupValues[2].toInt()
            val h2 = matches[1].groupValues[1].toInt()
            val m2 = matches[1].groupValues[2].toInt()
            val resH1 = resolveHour(h1, lower)
            val resH2 = resolveHour(h2, lower)
            return Pair("%02d:%02d".format(resH1, m1), "%02d:%02d".format(resH2, m2))
        }
        
        val hourRangePattern = Regex("""\b(?:de\s+)?(\d{1,2})\s+a\s+(\d{1,2})\b""")
        hourRangePattern.find(lower)?.let { match ->
            val h1 = match.groupValues[1].toInt()
            val h2 = match.groupValues[2].toInt()
            if (h1 in 0..23 && h2 in 0..23) {
                val resH1 = resolveHour(h1, lower)
                val resH2 = resolveHour(h2, lower)
                return Pair("%02d:00".format(resH1), "%02d:00".format(resH2))
            }
        }
        
        val singleTime = extractTime(query)
        if (singleTime != null) {
            val parts = singleTime.split(":")
            val h = parts[0].toInt()
            val m = parts[1].toInt()
            val endH = (h + 1) % 24
            val endTime = "%02d:%02d".format(endH, m)
            return Pair(singleTime, endTime)
        }
        return null
    }

    private fun extractAllTimesInQuery(query: String): List<String> {
        val lower = query.lowercase()
        val matchedIndices = BooleanArray(lower.length)
        
        data class TimeMatch(val index: Int, val timeStr: String)
        val matches = mutableListOf<TimeMatch>()

        // 1. Match HH:mm pattern (e.g. 17:30, 4:30)
        val colonPattern = Regex("""\b(\d{1,2}):(\d{2})\b""")
        for (match in colonPattern.findAll(lower)) {
            val start = match.range.first
            val end = match.range.last
            val h = match.groupValues[1].toInt()
            val m = match.groupValues[2].toInt()
            if (h in 0..23 && m in 0..59) {
                // Mark indices
                for (i in start..end) {
                    matchedIndices[i] = true
                }
                val resolvedH = resolveHour(h, lower)
                matches.add(TimeMatch(start, "%02d:%02d".format(resolvedH, m)))
            }
        }

        // 2. Match H1 a H2 range (e.g. de 5 a 6, 5 a 6)
        val hourRangePattern = Regex("""\b(?:de\s+)?(\d{1,2})\s+a\s+(\d{1,2})\b""")
        for (match in hourRangePattern.findAll(lower)) {
            val start = match.range.first
            val end = match.range.last
            // Check if any index in this match is already matched
            var overlap = false
            for (i in start..end) {
                if (i in matchedIndices.indices && matchedIndices[i]) {
                    overlap = true
                    break
                }
            }
            if (!overlap) {
                val h1 = match.groupValues[1].toInt()
                val h2 = match.groupValues[2].toInt()
                if (h1 in 0..23 && h2 in 0..23) {
                    for (i in start..end) {
                        matchedIndices[i] = true
                    }
                    val resH1 = resolveHour(h1, lower)
                    val resH2 = resolveHour(h2, lower)
                    matches.add(TimeMatch(start, "%02d:00".format(resH1)))
                    matches.add(TimeMatch(start + 1, "%02d:00".format(resH2)))
                }
            }
        }

        // 3. Match single hour patterns (e.g. a las 5, para las 5, de las 5, a la 1)
        val hourPatterns = listOf(
            Regex("""\b(?:a\s+)?las\s+(\d{1,2})\b"""),
            Regex("""\b(?:a\s+)?la\s+(\d{1,2})\b"""),
            Regex("""\bde\s+las?\s+(\d{1,2})\b"""),
            Regex("""\bpara\s+las?\s+(\d{1,2})\b""")
        )

        for (pattern in hourPatterns) {
            for (match in pattern.findAll(lower)) {
                val start = match.range.first
                val end = match.range.last
                var overlap = false
                for (i in start..end) {
                    if (i in matchedIndices.indices && matchedIndices[i]) {
                        overlap = true
                        break
                    }
                }
                if (!overlap) {
                    val hStr = match.groupValues[1]
                    val h = hStr.toIntOrNull()
                    if (h != null && h in 0..23) {
                        for (i in start..end) {
                            matchedIndices[i] = true
                        }
                        val resolvedH = resolveHour(h, lower)
                        matches.add(TimeMatch(start, "%02d:00".format(resolvedH)))
                    }
                }
            }
        }

        return matches.sortedBy { it.index }.map { it.timeStr }
    }

    private fun isTargetDay(query: String, day: Int): Boolean {
        val lower = query.lowercase()
        val today = dateTimeProvider.getNow().dayOfWeek.value
        val tomorrow = (today % 7) + 1
        val yesterday = if (today == 1) 7 else today - 1

        val dayNames = mutableListOf<String>()
        when (day) {
            1 -> dayNames.addAll(listOf("lunes", "monday"))
            2 -> dayNames.addAll(listOf("martes", "tuesday"))
            3 -> dayNames.addAll(listOf("miércoles", "miercoles", "wednesday"))
            4 -> dayNames.addAll(listOf("jueves", "thursday"))
            5 -> dayNames.addAll(listOf("viernes", "friday"))
            6 -> dayNames.addAll(listOf("sábado", "sabado", "saturday"))
            7 -> dayNames.addAll(listOf("domingo", "sunday"))
        }

        if (day == today) {
            dayNames.addAll(listOf("hoy", "today", "esta tarde", "esta mañana", "esta noche", "tonight"))
        }
        if (day == tomorrow) {
            dayNames.addAll(listOf("mañana", "tomorrow"))
        }
        if (day == yesterday) {
            dayNames.addAll(listOf("ayer", "yesterday"))
        }

        for (name in dayNames) {
            val idx = lower.indexOf(name)
            if (idx >= 0) {
                val prefix = lower.substring(0, idx).trim()
                if (prefix.endsWith(" al") || prefix.endsWith(" a") || prefix.endsWith(" para") || prefix.endsWith(" para el") || prefix.endsWith(" to")) {
                    return true
                }
            }
        }
        return false
    }

    private fun isQuestionOrQuery(query: String): Boolean {
        val lower = query.lowercase()
        if (lower.contains("?")) return true
        val questionWords = listOf(
            "qué ", "que ", "cómo ", "como ", "cuándo ", "cuando ", "dónde ", "donde ",
            "quién ", "quien ", "cuánto ", "cuanto ", "cuál ", "cual ", "horario",
            "clases", "alumnos", "información", "ver ", "muestra ", "mostrar", "dime",
            "info", "tengo", "hay", "list", "show", "get", "who", "what", "when", "where",
            "how", "how much", "how many", "which", "schedule", "classes", "students", "tell me"
        )
        return questionWords.any { lower.contains(it) }
    }
}
