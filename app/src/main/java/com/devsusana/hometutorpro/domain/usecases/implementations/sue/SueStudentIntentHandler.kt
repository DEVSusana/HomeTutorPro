package com.devsusana.hometutorpro.domain.usecases.implementations.sue

import com.devsusana.hometutorpro.domain.entities.SueOperationResult
import com.devsusana.hometutorpro.domain.usecases.implementations.ScheduleTools
import com.devsusana.hometutorpro.domain.usecases.implementations.StudentTools
import java.util.Locale
import javax.inject.Inject

/**
 * Handles student-related intents: creation, deletion, field updates (address, phone, price, course, notes),
 * and student profile/count/weekly queries.
 */
class SueStudentIntentHandler @Inject constructor(
    private val studentTools: StudentTools,
    private val scheduleTools: ScheduleTools,
    private val entityExtractor: SueEntityExtractor,
    private val conversationContext: SueConversationContext
) {

    suspend fun handleCreateStudent(query: String, lower: String): SueOperationResult {
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
        return if (studentName.isNullOrBlank()) {
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

    suspend fun handleDeleteStudent(): SueOperationResult {
        val studentName = conversationContext.lastMentionedStudentName
        return if (studentName == null) {
            conversationContext.setAwaitingSlot(IntentType.DELETE_STUDENT, SlotType.STUDENT_NAME)
            SueOperationResult.Prepare.Error(
                SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
                "¿De qué alumno quieres eliminar el perfil?"
            )
        } else {
            studentTools.prepareDeleteStudent(studentName)
        }
    }

    suspend fun handleUpdateNotes(lower: String): SueOperationResult {
        val studentName = conversationContext.lastMentionedStudentName
        return if (studentName == null) {
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

    suspend fun handleUpdateAddress(query: String, lower: String): SueOperationResult {
        val studentName = conversationContext.lastMentionedStudentName
        return if (studentName == null) {
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

    suspend fun handleUpdatePrice(lower: String): SueOperationResult {
        val studentName = conversationContext.lastMentionedStudentName
        return if (studentName == null) {
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

    suspend fun handleUpdatePhone(query: String, lower: String): SueOperationResult {
        val studentName = conversationContext.lastMentionedStudentName
        return if (studentName == null) {
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

    suspend fun handleUpdateCourse(query: String, lower: String): SueOperationResult {
        val studentName = conversationContext.lastMentionedStudentName
        return if (studentName == null) {
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

    suspend fun handleQueryStudentCount(): SueOperationResult {
        return studentTools.getActiveStudentCount()
    }

    suspend fun handleQueryStudentDetails(query: String, lower: String): SueOperationResult {
        val studentName = conversationContext.lastMentionedStudentName
            ?: return SueOperationResult.StudentDetails(query, emptyList())

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
        return detailsResult
    }

    suspend fun handleQueryStudentWeeklyClasses(lower: String): SueOperationResult {
        val studentName = conversationContext.lastMentionedStudentName
            ?: return SueOperationResult.ReadSuccess("No he identificado de qué alumno quieres saber las clases. ¿Puedes decirme su nombre?")

        val isPastClassesQuery = listOf(
            "le he dado", "he dado", "dado", "dadas", "le di", "di a",
            "impartido", "impartida", "impartidas", "impartidos", "llevo",
            "given", "have i given", "did i give", "taught", "have i taught", "did i teach", "teach", "taught to", "teach to"
        ).any { it in lower }

        return if (isPastClassesQuery) {
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
