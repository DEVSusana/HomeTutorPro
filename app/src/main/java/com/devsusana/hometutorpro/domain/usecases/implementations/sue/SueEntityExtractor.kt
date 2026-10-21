package com.devsusana.hometutorpro.domain.usecases.implementations.sue

import com.devsusana.hometutorpro.domain.entities.SuePendingAction
import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.devsusana.hometutorpro.domain.repository.DateTimeProvider
import com.devsusana.hometutorpro.domain.utils.DoorCodeExtractor
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dedicated parser and extractor for entities in natural language queries (dates, times, names, amounts).
 */
@Singleton
class SueEntityExtractor @Inject constructor(
    private val dateTimeProvider: DateTimeProvider,
    private val authRepository: AuthRepository,
    private val phoneticMatcher: SpanishPhoneticMatcher
) {

    constructor(
        dateTimeProvider: DateTimeProvider,
        authRepository: AuthRepository
    ) : this(dateTimeProvider, authRepository, SpanishPhoneticMatcher())

    fun getPhoneticMatcher(): SpanishPhoneticMatcher = phoneticMatcher

    fun stripAccents(str: String): String {
        val normalized = java.text.Normalizer.normalize(str, java.text.Normalizer.Form.NFD)
        return normalized.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    }

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
     */
    fun extractRelativeDayOfWeek(query: String): Int? {
        val today = dateTimeProvider.getNow().dayOfWeek.value
        return when {
            "hoy" in query || "today" in query ||
            "esta tarde" in query || "esta mañana" in query || "esta manana" in query ||
            "esta noche" in query || "tonight" in query ||
            "luego" in query || "later" in query -> today

            "mañana" in query || "manana" in query || "tomorrow" in query -> (today % 7) + 1

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
        ("mañana" in query || "manana" in query) && ("esta" in query || "por la" in query) -> "morning"
        "morning" in query -> "morning"
        else -> null
    }

    /**
     * Extracts two days of the week from a reschedule query (from/to).
     * Returns (fromDay, toDay) where each can be null if not found.
     */
    fun extractTwoDaysOfWeek(query: String): Pair<Int?, Int?> {
        val today = dateTimeProvider.getNow().dayOfWeek.value
        val tomorrow = (today % 7) + 1
        val yesterday = if (today == 1) 7 else today - 1

        val rawKeywords = listOf(
            "esta mañana" to today,
            "esta manana" to today,
            "esta tarde" to today,
            "esta noche" to today,
            "tonight" to today,
            "tomorrow" to tomorrow,
            "mañana" to tomorrow,
            "manana" to tomorrow,
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
        val matches = mutableListOf<Pair<Int, Int>>()

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
     * Cleans an extracted student name by removing conversational prefixes ("llamado", "alumno llamado", "a un alumno llamado")
     * and filtering out date/calendar keywords.
     */
    fun cleanExtractedName(candidate: String): String? {
        var clean = candidate.trim()
        clean = clean.replace(
            Regex("""^(?:a\s+un\s+alumno\s+llamado|a\s+una\s+alumna\s+llamada|un\s+alumno\s+llamado|un\s+estudiante\s+llamado|una\s+alumna\s+llamada|una\s+estudiante\s+llamada|al\s+alumno\s+llamado|al\s+estudiante\s+llamado|al\s+alumno\s+llamalo|al\s+alumno\s+llámalo|alumno\s+llamado|estudiante\s+llamado|al\s+alumno|al\s+estudiante|a\s+la\s+alumna|a\s+la\s+estudiante|llamado|llamada|llámalo|llamalo|llámale|llamale|que\s+se\s+llama|que\s+se\s+llame|se\s+llama|se\s+llame|de\s+nombre|con\s+nombre|nombre|a\s+student\s+named|student\s+named|named|called)\s+""", RegexOption.IGNORE_CASE),
            ""
        ).trim()
        if (clean.isBlank()) return null

        val trailingWordsToStrip = Regex("""\s+(?:hoy|esta\s+tarde|esta\s+mañana|esta\s+manana|esta\s+noche|mañana|manana|ayer|lunes|martes|miércoles|miercoles|jueves|viernes|sábado|sabado|domingo|que\s+es|que\s+tiene|que\s+esta|que\s+está|y\s+ponle|y\s+pon|y\s+la\s+clase|y\s+el\s+precio|y\s+cobrale|y\s+cóbrale|con\s+precio|con\s+tarifa|a\s+\d).*$""", RegexOption.IGNORE_CASE)
        clean = clean.replace(trailingWordsToStrip, "").trim()
        clean = clean.replace(Regex("""\s+(?:en|su|de|con|por|para)$""", RegexOption.IGNORE_CASE), "").trim()
        clean = clean.trim('¿', '¡', '?', '!', '.', ',', ';', ':', '"', '\'').trim()
        if (clean.isBlank()) return null

        val dayKeywords = setOf(
            "lunes", "martes", "miércoles", "miercoles", "jueves", "viernes", "sábado", "sabado", "domingo",
            "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
            "hoy", "mañana", "tomorrow", "esta tarde", "esta mañana", "esta noche", "luego", "ayer", "yesterday",
            "tarde", "semana", "esta semana", "este mes", "este año", "este curso", "fin de semana"
        )
        if (clean.lowercase() in dayKeywords) {
            return null
        }
        return clean
    }

    /**
     * Attempts to extract a student name from the query by matching common connectors.
     */
    fun extractStudentName(query: String): String? {
        // 1. Inverted patterns where student is between verb and target:
        // e.g. "quita a Marcos la clase", "quítale a Marcos la clase", "pon a Marcos una clase", "edita a Marcos la dirección"
        val invertedRegex = Regex(
            """\b(?:pon|ponle|pónle|ponme|pónme|edita|editale|edítale|añade|anade|añadele|añádele|anadele|agrega|agregale|quita|quitale|quítale|quítala|quitala|mueve|muevele|muévele|adelanta|adelantale|adelántale|cancela|cancelale|cancélale|anula|anulale|anúlale|cambia|cambiale|cámbiale|pasa|pasale|pásale|actualiza|actualizale|actualízale)\s+a\s+([a-zA-ZáéíóúÁÉÍÓÚñÑ]+(?:\s+[a-zA-ZáéíóúÁÉÍÓÚñÑ]+)?)\s+(?:la\s+clase|una\s+clase|clase|el\s+horario|su\s+clase|en\s+su\s+direcci[oó]n|en\s+la\s+direcci[oó]n|la\s+direcci[oó]n|su\s+direcci[oó]n|de\s+direcci[oó]n|el\s+portero|de\s+portero|el\s+telefonillo|de\s+telefonillo|el\s+timbre|el\s+portal|el\s+intercom|el\s+c[oó]digo|el\s+precio|la\s+tarifa|de\s+precio|de\s+tarifa|el\s+tel[eé]fono|el\s+m[oó]vil|de\s+tel[eé]fono|de\s+m[oó]vil|el\s+curso|de\s+curso)\b""",
            RegexOption.IGNORE_CASE
        )
        invertedRegex.find(query)?.let { match ->
            val name = cleanExtractedName(match.groupValues[1])
            if (name != null) return name
        }

        // 1b. Preceding student patterns for relocation/movement:
        // e.g. "Pepe se ha mudado a Calle Mayor 12", "Carlos ahora vive en Calle Luna 5"
        val relocationRegex = Regex(
            """^\s*([a-zA-ZáéíóúÁÉÍÓÚñÑ]+(?:\s+[a-zA-ZáéíóúÁÉÍÓÚñÑ]+)?)\s+(?:se\s+ha\s+mudado|se\s+muda|se\s+mud[oó]|ha\s+cambiado\s+de\s+casa|ahora\s+vive|vive\s+ahora)\b""",
            RegexOption.IGNORE_CASE
        )
        relocationRegex.find(query)?.let { match ->
            val name = cleanExtractedName(match.groupValues[1])
            if (name != null) return name
        }

        // 2. Elided patterns with "la de [Student]":
        // e.g. "quita la de Marcos", "mueve la de Marcos al viernes", "quítale la de Marcos"
        val elidedRegex = Regex(
            """\b(?:pon|ponle|pónle|quita|quitale|quítale|mueve|muevele|muévele|adelanta|adelantale|adelántale|cancela|cancelale|cancélale|anula|anulale|anúlale|cambia|cambiale|cámbiale|pasa|pasale|pásale)\s+(?:la\s+de|el\s+de)\s+([a-zA-ZáéíóúÁÉÍÓÚñÑ]+)\b""",
            RegexOption.IGNORE_CASE
        )
        elidedRegex.find(query)?.let { match ->
            val name = cleanExtractedName(match.groupValues[1])
            if (name != null) return name
        }

        // 3. Connectors for direct phrasing
        val commonConnectors = listOf(
            // Cancel and Quita connectors
            "cancela la clase extra de ", "cancelar la clase extra de ", "cancela la clase extra con ",
            "cancela la clase de ", "cancelar la clase de ", "cancélale la clase a ", "cancelale la clase a ",
            "anula la clase de ", "anular la clase de ", "anúlale la clase a ", "anulale la clase a ",
            "quitar la clase de ", "quita la clase de ", "quítale la clase de ", "quitale la clase de ",
            "quitar la clase con ", "quita la clase con ", "quítale la clase con ", "quitale la clase con ",
            "quitar la clase a ", "quita la clase a ", "quítale la clase a ", "quitale la clase a ",
            "quitarle la clase a ", "quitarle la clase de ",
            "quita la de ", "quítale la de ", "quitale la de ",
            "quita a ", "quitar a ", "quítale a ", "quitale a ",
            "borra la clase de ", "borrar la clase de ", "elimina la clase de ", "eliminar la clase de ",
            "cancela la clase con ", "cancelar la clase con ", "cancela a ", "cancelar a ", "anula a ", "anular a ",
            
            // Move, Adelanta and Reschedule connectors
            "adelanta la clase de ", "adelantar la clase de ", "adelántale la clase a ", "adelantale la clase a ",
            "adelanta la de ", "adelántale la de ", "adelantale la de ",
            "adelanta a ", "adelantar a ", "adelántale a ", "adelantale a ",
            "mueve la clase de ", "mover la clase de ", "muévele la clase de ", "muevele la clase de ",
            "mueve la clase con ", "mover la clase con ", "mueve la clase a ",
            "muévele la clase a ", "muevele la clase a ", "mueve la de ", "muévele la de ", "muevele la de ",
            "mueve a ", "mover a ", "muévele a ", "muevele a ",
            "pasa la clase de ", "pasar la clase de ", "pasa la de ", "pásale la clase a ", "pasale la clase a ",
            "pasa a ", "pasar a ", "pásale a ", "pasale a ",
            "cambia la clase de ", "cambiar la clase de ", "cambia la clase con ", "cambiar la clase con ",
            "cámbiale la clase a ", "cambiale la clase a ", "cambia la de ",
            "cambia a ", "cambiar a ", "cámbiale a ", "cambiale a ",
            "traslada la clase de ", "la clase de ",
            
            // Querying student classes connectors
            "le he dado a un alumno llamado ", "le he dado a un estudiante llamado ",
            "le he dado a ", "he dado a ", "dado a ", "le di a ", "di a ",
            "clases dadas a ", "clases de ", "clases con ", "clases para ",
            "un alumno llamado ", "un estudiante llamado ", "alumno llamado ", "estudiante llamado ",

            // Add and Pon connectors
            "pon una clase a ", "pon una clase para ", "pon una clase con ", "pon una clase de ",
            "ponle una clase a ", "ponle una clase para ", "ponle una clase con ", "ponle una clase de ",
            "pon clase a ", "pon clase para ", "pon clase con ", "pon clase de ",
            "ponle clase a ", "ponle clase para ", "pon a ", "ponle a ", "ponme a ",
            "añade una clase para ", "añade una clase a ", "añade una clase con ", "añade una clase de ",
            "añade clase para ", "añade clase a ", "añade clase con ", "añade clase de ",
            "añádele una clase a ", "añadele una clase a ",
            "añade a ", "añádele a ", "añadele a ",
            "anade una clase para ", "anade una clase a ",
            "anade a ", "anadele a ",
            "agrega una clase para ", "agrega una clase a ",
            "agrega a ", "agregar a ",
            "añade un horario para ", "añadir un horario para ", "crea un horario para ", "crear un horario para ",
            "horario para ", "horario de ",
            
            // Deletion & Creation connectors
            "elimina el horario de ", "borra el horario de ", "eliminar el horario de ",
            "quitar el horario de ", "quita el horario de ",
            "elimina al alumno ", "eliminar al alumno ", "borra al alumno ", "borrar al alumno ",
            "elimina a ", "eliminar a ", "borra a ", "borrar a ",

            // Address & Student details connectors
            "cambiale la direccion a ", "cambiale la dirección a ", "cámbiale la direccion a ", "cámbiale la dirección a ",
            "actualizale la direccion a ", "actualizale la dirección a ", "actualízale la direccion a ", "actualízale la dirección a ",
            "ponle la direccion a ", "ponle la dirección a ", "pónle la direccion a ", "pónle la dirección a ",
            "editale la direccion a ", "editale la dirección a ", "edítale la direccion a ", "edítale la dirección a ",
            "añadele la direccion a ", "añadele la dirección a ", "añádele la direccion a ", "añádele la dirección a ",
            "cambia la direccion de ", "cambia la dirección de ", "cambiar la direccion de ", "cambiar la dirección de ",
            "actualiza la direccion de ", "actualiza la dirección de ", "actualizar la direccion de ", "actualizar la dirección de ",
            "modifica la direccion de ", "modifica la dirección de ", "modificar la direccion de ", "modificar la dirección de ",
            "edita la direccion de ", "edita la dirección de ", "editar la direccion de ", "editar la dirección de ",
            "añade la direccion de ", "añade la dirección de ", "anade la direccion de ", "anade la dirección de ",
            "pon la direccion de ", "pon la dirección de ", "guarda la direccion de ", "guarda la dirección de ",
            "la direccion de ", "la dirección de ", "direccion de ", "dirección de ",
            "cambia el portero de ", "actualiza el portero de ", "pon el portero de ", "edita el portero de ", "añade el portero de ", "anade el portero de ",
            "cambiale el portero a ", "cámbiale el portero a ", "ponle el portero a ", "edítale el portero a ", "editale el portero a ", "añádele el portero a ", "añadele el portero a ", "anadele el portero a ",
            "cambiale de portero a ", "cámbiale de portero a ", "ponle de portero a ", "edítale de portero a ", "editale de portero a ", "añádele de portero a ", "añadele de portero a ",
            "cambiale de direccion a ", "cámbiale de dirección a ", "ponle de direccion a ", "ponle de dirección a ", "edítale de direccion a ", "edítale de dirección a ",
            "el portero de ", "portero de ",
            "cambia el telefonillo de ", "actualiza el telefonillo de ", "pon el telefonillo de ", "edita el telefonillo de ", "añade el telefonillo de ", "anade el telefonillo de ",
            "cambiale el telefonillo a ", "cámbiale el telefonillo a ", "ponle el telefonillo a ", "edítale el telefonillo a ", "editale el telefonillo a ", "añádele el telefonillo a ", "añadele el telefonillo a ", "anadele el telefonillo a ",
            "el telefonillo de ", "telefonillo de ",
            "datos de ", "informacion de ", "información de ", "ficha de ", "detalles de ", "dónde vive ", "donde vive ",
            "la nueva casa de ", "nueva casa de ", "la nueva direccion de ", "la nueva dirección de ",

            // Price / Rate connectors
            "el precio por hora de ", "el precio por hora a ", "la tarifa por hora de ", "la tarifa por hora a ",
            "cambia el precio de ", "cambia la tarifa de ", "cámbiale el precio a ", "cambiale el precio a ", "cámbiale la tarifa a ", "cambiale la tarifa a ",
            "actualiza el precio de ", "actualiza la tarifa de ", "actualízale el precio a ", "actualizale el precio a ", "actualízale la tarifa a ",
            "pon el precio de ", "pon la tarifa de ", "ponle el precio a ", "ponle la tarifa a ",
            "edita el precio de ", "edita la tarifa de ", "edítale el precio a ", "editale el precio a ",
            "el precio de ", "el precio a ", "la tarifa de ", "la tarifa a ",

            // Phone connectors
            "cambia el telefono de ", "cambia el teléfono de ", "cambia el movil de ", "cambia el móvil de ",
            "cámbiale el telefono a ", "cambiale el telefono a ", "cámbiale el teléfono a ", "cambiale el teléfono a ",
            "actualiza el telefono de ", "actualiza el teléfono de ", "actualízale el telefono a ", "actualizale el telefono a ", "actualízale el teléfono a ",
            "pon el telefono de ", "pon el teléfono de ", "ponle el telefono a ", "ponle el teléfono a ",
            "edita el telefono de ", "edita el teléfono de ", "edítale el telefono a ", "editale el telefono a ", "edítale el teléfono a ",
            "el nuevo telefono de ", "el nuevo teléfono de ", "el nuevo movil de ", "el nuevo móvil de ",
            "el telefono de ", "el teléfono de ", "el movil de ", "el móvil de ",

            // Course / Level connectors
            "cambia el curso de ", "cámbiale el curso a ", "cambiale el curso a ",
            "actualiza el curso de ", "actualízale el curso a ", "actualizale el curso a ",
            "pon el curso de ", "ponle el curso a ",
            "edita el curso de ", "edítale el curso a ", "editale el curso a ",
            "el nuevo curso de ", "el curso de ", "el nivel de ",
            
            // English equivalents
            "cancel the extra class of ", "cancel the extra class for ", "cancel extra class for ", "cancel extra class with ",
            "cancel class for ", "cancel the class of ", "move class for ",
            "reschedule class for ", "change class for ", "the class of ",
            "cancel class with ", "cancel the class with ", "move class with ",
            "change class with ", "how many classes for ", "how many classes with ",
            "classes given to ", "classes taught to ", "a student named ", "student named "
        )
        for (connector in commonConnectors) {
            val idx = query.indexOf(connector)
            if (idx >= 0) {
                val rest = query.substring(idx + connector.length).trim()
                val stopWords = listOf(
                    " una clase", " la clase", " clase ", " su clase", " el horario",
                    " es ", " es: ", ": ", " sea ", " sería ", " seria ", " que ", " que es",
                    " hoy", " esta tarde", " esta mañana", " esta manana", " esta noche", " mañana", " manana", " ayer",
                    " del ", " de ", " el ", " al ", " a ", " en ", " los ", " las ", " con ", " para ",
                    " from ", " of ", " the ", " to ", " at ", " in ", " on "
                )
                var end = rest.length
                for (stop in stopWords) {
                    val stopIdx = rest.indexOf(stop)
                    if (stopIdx in 1 until end) end = stopIdx
                }
                val rawName = rest.substring(0, end).trim()
                val name = cleanExtractedName(rawName)
                if (name != null) {
                    return name
                }
            }
        }
        return null
    }

    /**
     * Resolves AM/PM ambiguity for 12-hour values (1..8) to PM.
     */
    fun resolveHour(h: Int, query: String): Int {
        val professor = authRepository.currentUser.value
        val startStr = professor?.workingStartTime ?: "08:00"
        val endStr = professor?.workingEndTime ?: "23:00"
        return resolveHourSmart(h, query, startStr, endStr)
    }

    fun resolveHourSmart(h: Int, query: String, workingStartTime: String, workingEndTime: String): Int {
        if (h == 0 || h > 12) {
            return h
        }
        val lower = query.lowercase()

        val morningKeywords = listOf("esta mañana", "por la mañana", "de la mañana", "la mañana de", "madrugada", "am", "a.m.", "morning")
        if (morningKeywords.any { it in lower }) {
            return if (h == 12) 0 else h
        }

        val afternoonKeywords = listOf("esta tarde", "esta noche", "por la tarde", "por la noche", "de la tarde", "de la noche", "tarde", "noche", "pm", "p.m.", "afternoon", "evening", "night")
        if (afternoonKeywords.any { it in lower }) {
            return if (h == 12) 12 else h + 12
        }

        if (h in 1..7) {
            return h + 12
        }

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

        return h
    }

    private val spanishHourWords = mapOf(
        "cero" to 0, "medianoche" to 0,
        "una" to 1, "un" to 1, "uno" to 1,
        "dos" to 2, "tres" to 3, "cuatro" to 4, "cinco" to 5,
        "seis" to 6, "siete" to 7, "ocho" to 8, "nueve" to 9,
        "diez" to 10, "once" to 11, "doce" to 12, "trece" to 13,
        "catorce" to 14, "quince" to 15, "dieciseis" to 16,
        "dieciséis" to 16, "diecisiete" to 17, "dieciocho" to 18,
        "diecinueve" to 19, "veinte" to 20, "veintiuno" to 21,
        "veintiun" to 21, "veintiún" to 21, "veintidos" to 22,
        "veintidós" to 22, "veintitres" to 23, "veintitrés" to 23,
        "veinticuatro" to 0
    )

    private val hourWordsRegexStr = """una|un|uno|dos|tres|cuatro|cinco|seis|siete|ocho|nueve|diez|once|doce|trece|catorce|quince|dieciseis|dieciséis|diecisiete|dieciocho|diecinueve|veinte|veintiuno|veintiun|veintiún|veintidos|veintidós|veintitres|veintitrés|veinticuatro|cero|medianoche"""

    fun parseHourString(str: String): Int? {
        val trimmed = stripAccents(str.trim().lowercase())
        return trimmed.toIntOrNull() ?: spanishHourWords[trimmed]
    }

    /**
     * Attempts to extract a time in "HH:mm" format from the query.
     */
    fun extractTime(query: String): String? {
        val lower = stripAccents(query.lowercase().trim())
        val originalLower = query.lowercase().trim()

        val colonPattern = Regex("""\b(\d{1,2}):(\d{2})\s*(?:h|hrs|horas)?\b""")
        colonPattern.find(lower)?.let { match ->
            val h = match.groupValues[1].toInt()
            val m = match.groupValues[2].toInt()
            if (h in 0..23 && m in 0..59) {
                val resolvedH = resolveHour(h, originalLower)
                return "%02d:%02d".format(resolvedH, m)
            }
        }

        val militaryPattern = Regex("""\b(?:(?:a\s+)?las\s+)?(\d{2})(\d{2})\s*(?:h|hrs|horas)?\b""")
        militaryPattern.find(lower)?.let { match ->
            val h = match.groupValues[1].toIntOrNull()
            val m = match.groupValues[2].toIntOrNull()
            if (h != null && h in 0..23 && m != null && m in 0..59) {
                val resolvedH = resolveHour(h, originalLower)
                return "%02d:%02d".format(resolvedH, m)
            }
        }

        val fractionPattern = Regex(
            """\b(?:(?:a\s+eso\s+de\s+|sobre\s+|para\s+|a\s+|de\s+)?(?:las?\s+)?)?(\d{1,2}|$hourWordsRegexStr)\s+(y\s+media|y\s+cuarto|menos\s+cuarto|menos\s+veinte|menos\s+diez|menos\s+cinco|y\s+\d{1,2}|y\s+diez|y\s+veinte|y\s+veinticinco|y\s+treinta|y\s+cuarenta|y\s+cuarenta\s+y\s+cinco|y\s+cincuenta)\b"""
        )
        fractionPattern.find(lower)?.let { match ->
            val hVal = parseHourString(match.groupValues[1])
            if (hVal != null && hVal in 0..23) {
                val frac = match.groupValues[2]
                var h = hVal
                val m = when {
                    frac.startsWith("y media") || frac == "y 30" || frac == "y treinta" -> 30
                    frac.startsWith("y cuarto") || frac == "y 15" || frac == "y quince" -> 15
                    frac.startsWith("menos cuarto") -> {
                        h = if (h > 0) h - 1 else 23
                        45
                    }
                    frac.startsWith("menos veinte") -> {
                        h = if (h > 0) h - 1 else 23
                        40
                    }
                    frac.startsWith("menos diez") -> {
                        h = if (h > 0) h - 1 else 23
                        50
                    }
                    frac.startsWith("menos cinco") -> {
                        h = if (h > 0) h - 1 else 23
                        55
                    }
                    frac == "y 10" || frac == "y diez" -> 10
                    frac == "y 20" || frac == "y veinte" -> 20
                    frac == "y 25" || frac == "y veinticinco" -> 25
                    frac == "y 35" -> 35
                    frac == "y 40" || frac == "y cuarenta" -> 40
                    frac == "y 45" || frac == "y cuarenta y cinco" -> 45
                    frac == "y 50" || frac == "y cincuenta" -> 50
                    frac.startsWith("y ") -> frac.removePrefix("y ").trim().toIntOrNull() ?: 0
                    else -> 0
                }
                val resolvedH = resolveHour(h, originalLower)
                return "%02d:%02d".format(resolvedH, m)
            }
        }

        val prefixHourPattern = Regex(
            """\b(?:(?:a\s+eso\s+de\s+|sobre\s+|para\s+|a\s+|de\s+)?(?:las?\s+)|(?:to|at|from)\s+)(\d{1,2}|$hourWordsRegexStr)\s*(?:h|hrs|horas|hours)?\b"""
        )
        prefixHourPattern.find(lower)?.let { match ->
            val h = parseHourString(match.groupValues[1])
            if (h != null && h in 0..23) {
                val resolvedH = resolveHour(h, originalLower)
                return "%02d:00".format(resolvedH)
            }
        }

        val modifierHourPattern = Regex(
            """\b(\d{1,2}|$hourWordsRegexStr)\s*(?:h|hrs|horas|pm|p\.m\.|am|a\.m\.|de\s+la\s+tarde|de\s+la\s+manana|de\s+la\s+noche|por\s+la\s+tarde|por\s+la\s+manana|por\s+la\s+noche)\b"""
        )
        modifierHourPattern.find(lower)?.let { match ->
            val h = parseHourString(match.groupValues[1])
            if (h != null && h in 0..23) {
                val resolvedH = resolveHour(h, originalLower)
                return "%02d:00".format(resolvedH)
            }
        }

        val standalonePattern = Regex(
            """^(?:a\s+)?(\d{1,2}|$hourWordsRegexStr)$"""
        )
        standalonePattern.find(lower)?.let { match ->
            val h = parseHourString(match.groupValues[1])
            if (h != null && h in 0..23) {
                val resolvedH = resolveHour(h, originalLower)
                return "%02d:00".format(resolvedH)
            }
        }

        // 7. Colloquial conversational time phrases (e.g. "después de comer", "a primera hora de la tarde")
        phoneticMatcher.resolveColloquialTimePhrase(query)?.let { return it }

        return null
    }

    /**
     * Attempts to extract a student name for financial intents.
     */
    fun extractStudentNameForFinance(query: String): String? {
        val commonConnectors = listOf(
            "pagado por ", "deuda de ", "saldo de ", "a la cuenta de ",
            "a la deuda de ", "sumale a ", "súmale a ",
            "paid by ", "debt of ", "balance of ", "to the account of ", "to the debt of ", "add to "
        )
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

        val deIdx = query.lastIndexOf(" de ")
        val ofIdx = query.lastIndexOf(" of ")
        val splitIdx = if (deIdx >= 0) deIdx + 4 else if (ofIdx >= 0) ofIdx + 4 else -1

        if (splitIdx >= 0) {
            val name = query.substring(splitIdx).trim()
            if (name.isNotBlank() && !name.first().isDigit()) {
                val dayKeywords = listOf(
                    "lunes", "martes", "miércoles", "miercoles", "jueves", "viernes", "sábado", "sabado", "domingo",
                    "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
                    "hoy", "mañana", "tomorrow", "esta tarde", "esta mañana", "esta noche", "luego", "ayer", "yesterday",
                    "semana", "esta semana", "este mes", "este año", "este curso", "fin de semana"
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
     * Extracts a numeric amount from the query (supports euros, pavos, $, and hourly rates like 'a 15 la hora').
     */
    fun extractAmount(query: String, fallbackAmount: Double? = null): Double? {
        val amountPattern = Regex("""\b(\d+(?:[.,]\d{1,2})?)\s*(?:euros?\b|pavitos\b|pavos\b|dollars?\b|€|\$)""")
        amountPattern.find(query)?.let { match ->
            return match.groupValues[1].replace(',', '.').toDoubleOrNull()
        }

        val ratePattern = Regex("""\b(?:a|por\s+)?(\d+(?:[.,]\d{1,2})?)\s*(?:euros?|€)?\s*(?:la\s+hora|por\s+hora|la\s+clase|el\s+mes)\b""")
        ratePattern.find(query)?.let { match ->
            return match.groupValues[1].replace(',', '.').toDoubleOrNull()
        }

        val barePattern = Regex("""(?:pago de|sumar|sumale|súmale|añadir|añade|payment of|add)\s+(\d+(?:[.,]\d{1,2})?)\b""")
        barePattern.find(query)?.let { match ->
            return match.groupValues[1].replace(',', '.').toDoubleOrNull()
        }

        val hasQuantityPronoun = listOf("esa cantidad", "esa suma", "eso", "todo", "su deuda", "el total", "el saldo").any { it in query }
        if (hasQuantityPronoun) {
            if (fallbackAmount != null && fallbackAmount > 0.0) {
                return fallbackAmount
            }
        }
        return null
    }

    /**
     * Extracts an hourly rate or price for student creation and schedule workflows.
     * Supports formats like 'a 9 la hora', 'ponle la clase a 9€', 'a 9€', 'tarifa de 9',
     * or bare numbers ('9', '9.50', '9€') when answering a price prompt.
     */
    fun extractPrice(query: String): Double? {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return null

        // 1. Explicit hourly rate / price setting expressions
        val ratePattern = Regex(
            """(?:(?:precio|tarifa|clase|ponle|pon|pónle|cobrale|cóbrale|a|por)\s+)*(?:la\s+clase\s+a\s+|el\s+precio\s+a\s+|a\s+|por\s+)?(\d+(?:[.,]\d{1,2})?)\s*(?:euros?|€|pavitos|pavos)?(?:\s*(?:/h|/hora|la\s+hora|por\s+hora|cada\s+hora|la\s+clase|por\s+clase))?""",
            RegexOption.IGNORE_CASE
        )
        ratePattern.findAll(trimmed).forEach { match ->
            val numStr = match.groupValues[1]
            val candidate = numStr.replace(',', '.').toDoubleOrNull()
            if (candidate != null && candidate > 0.0 && candidate <= 500.0) {
                val matchedText = match.value.lowercase()
                if (matchedText.contains("hora") || matchedText.contains("clase") ||
                    matchedText.contains("€") || matchedText.contains("euro") ||
                    matchedText.contains("precio") || matchedText.contains("tarifa") ||
                    matchedText.contains("ponle") || matchedText.contains("a ") || matchedText.startsWith("a")) {
                    return candidate
                }
            }
        }

        // 2. Standard extractAmount (e.g. "9€", "15 euros")
        extractAmount(trimmed, null)?.let { return it }

        // 3. Bare number input (e.g. user answering "¿Cuál será el precio?" with "9", "9.50", "a 9")
        val barePattern = Regex("""^\s*(?:a\s+|ponle\s+|pon\s+|pónle\s+)?(\d+(?:[.,]\d{1,2})?)\s*(?:€|euros?|pavitos|pavos)?\s*$""", RegexOption.IGNORE_CASE)
        barePattern.find(trimmed)?.let { match ->
            return match.groupValues[1].replace(',', '.').toDoubleOrNull()
        }

        return null
    }

    fun extractNameAfter(query: String, keywords: List<String>): String? {
        for (keyword in keywords) {
            val idx = query.indexOf(keyword)
            if (idx >= 0) {
                val rest = query.substring(idx + keyword.length).trim()
                val stopWords = listOf(" una clase", " la clase", " clase ", " de ", " del ", " a ", " al ", " para ", " en ", " con ")
                var end = rest.length
                for (stop in stopWords) {
                    val stopIdx = rest.indexOf(stop)
                    if (stopIdx in 1 until end) end = stopIdx
                }
                val word = rest.substring(0, end).substringBefore(" ").trim()
                val clean = cleanExtractedName(word)
                if (clean != null && clean.isNotBlank()) return clean
            }
        }
        return null
    }

    fun extractDurationOptional(query: String): Int? {
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

    fun extractDuration(query: String): Int {
        return extractDurationOptional(query) ?: 60
    }

    fun parseCreateStudent(query: String): SuePendingAction.CreateStudent? {
        val lower = query.lowercase()
        val nameKeywords = listOf(
            "crea un alumno ", "crear un alumno ", "crea al alumno ", "crea el alumno ",
            "crea un estudiante ", "crear un estudiante ", "crea al estudiante ", "crea el estudiante ",
            "añade un alumno ", "añadir un alumno ", "añade al alumno ",
            "añade un estudiante ", "añadir un estudiante ", "añade al estudiante ",
            "anade un alumno ", "anadir un alumno ", "anade al alumno ",
            "anade un estudiante ", "anadir un estudiante ", "anade al estudiante ",
            "nuevo alumno ", "nuevo estudiante ",
            "create student ", "add student "
        )
        var name = ""
        for (kw in nameKeywords) {
            val idx = lower.indexOf(kw)
            if (idx >= 0) {
                val rest = query.substring(idx + kw.length).trim()
                val stopWords = listOf(" de ", " a ", " para ", " in ", " at ", " for ", " los ", " el ", " las ", " la ")
                var end = rest.length
                for (stop in stopWords) {
                    val stopIdx = rest.lowercase().indexOf(stop)
                    if (stopIdx in 1 until end) end = stopIdx
                }
                val candidate = rest.substring(0, end).trim()
                name = cleanExtractedName(candidate) ?: candidate
                break
            }
        }
        if (name.isBlank()) return null

        val price = extractPrice(lower) ?: extractAmount(lower) ?: 15.0

        var subjects = ""
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
        val course = courseKeywords.find { it in lower } ?: ""

        val normalizedSubjects = if (subjects.isNotBlank()) phoneticMatcher.normalizeSubjects(subjects) else ""
        return SuePendingAction.CreateStudent(name, course, normalizedSubjects, price)
    }

    fun extractTwoTimes(query: String): Pair<String, String>? {
        val lower = stripAccents(query.lowercase().trim())
        val originalLower = query.lowercase().trim()

        val timePattern = Regex("""\b(\d{1,2}):(\d{2})\b""")
        val matches = timePattern.findAll(lower).toList()
        if (matches.size >= 2) {
            val h1 = matches[0].groupValues[1].toInt()
            val m1 = matches[0].groupValues[2].toInt()
            val h2 = matches[1].groupValues[1].toInt()
            val m2 = matches[1].groupValues[2].toInt()
            val resH1 = resolveHour(h1, originalLower)
            val resH2 = resolveHour(h2, originalLower)
            return Pair("%02d:%02d".format(resH1, m1), "%02d:%02d".format(resH2, m2))
        }

        val hourRangePattern = Regex("""\b(?:de\s+)?(\d{1,2}|$hourWordsRegexStr)\s+a\s+(\d{1,2}|$hourWordsRegexStr)\b""")
        hourRangePattern.find(lower)?.let { match ->
            val h1 = parseHourString(match.groupValues[1])
            val h2 = parseHourString(match.groupValues[2])
            if (h1 != null && h1 in 0..23 && h2 != null && h2 in 0..23) {
                val resH1 = resolveHour(h1, originalLower)
                val resH2 = resolveHour(h2, originalLower)
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

    fun isTargetDay(query: String, day: Int): Boolean {
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

    fun isQuestionOrQuery(query: String): Boolean {
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

    fun extractNotesText(query: String): String? {
        val markers = listOf("que diga que ", "que diga ", "observacion: ", "observaciones: ", "nota: ", "notas: ", "apunte: ", "apuntes: ", "comentario: ", "comentarios: ", "que ", "para: ", "nota ", "notas ")
        for (marker in markers) {
            val idx = query.indexOf(marker)
            if (idx >= 0) {
                val text = query.substring(idx + marker.length).trim()
                if (text.isNotBlank()) return text
            }
        }
        return null
    }

    /**
     * Extracts a postal address or intercom / portero specification from a query.
     */
    fun extractAddressText(query: String, studentName: String? = null): String? {
        val lower = stripAccents(query.lowercase().trim())
        val original = query.trim()

        val streetPrefixes = listOf("calle ", "c/ ", "c/", "avenida ", "avda ", "av. ", "av ", "plaza ", "pza ", "paseo ", "pº ", "carretera ", "ctra ")
        val hasStreet = streetPrefixes.any { lower.contains(it) }

        // 1. Direct door / intercom code if no street is present (e.g. "el codigo de porte 387", "el portero de Hugo es 14B")
        if (!hasStreet) {
            val doorCode = DoorCodeExtractor.extractDoorCode(original)
            if (doorCode != null) {
                return doorCode
            }
        }

        // 1b. Relocation markers (e.g. "Pepe se ha mudado a Calle Mayor 12", "ahora vive en Calle Sol 4")
        val relocationMarkers = listOf(
            "se ha mudado a ", "se muda a ", "se mudó a ", "se mudo a ",
            "ha cambiado de casa a ", "cambia de casa a ",
            "ahora vive en ", "ha pasado a vivir en ", "vive ahora en ",
            "la nueva casa de ", "nueva casa de ", "su nueva casa es "
        )
        for (m in relocationMarkers) {
            val idx = lower.indexOf(m)
            if (idx >= 0) {
                var candidate = original.substring(idx + m.length).trim()
                if (studentName != null && candidate.startsWith("$studentName es ", ignoreCase = true)) {
                    candidate = candidate.substring("$studentName es ".length).trim()
                } else if (studentName != null && candidate.startsWith("$studentName ", ignoreCase = true)) {
                    candidate = candidate.substring("$studentName ".length).trim()
                }
                if (candidate.isNotBlank()) {
                    return candidate
                }
            }
        }

        // 2. Markers after "direccion"
        val dirIdx = listOf(lower.indexOf("direccion"), lower.indexOf("dirección")).filter { it >= 0 }.minOrNull()
        if (dirIdx != null) {
            val afterDir = lower.substring(dirIdx + 9)
            val afterDirOriginal = original.substring(Math.min(original.length, dirIdx + 9))

            // Check transitions like " a ", " por ", " es ", ": "
            val transitionMarkers = listOf(" a ", " por ", " es ", ": ")
            for (m in transitionMarkers) {
                val tIdx = afterDir.indexOf(m)
                if (tIdx >= 0) {
                    val candidate = afterDirOriginal.substring(tIdx + m.length).trim()
                    if (candidate.isNotBlank()) {
                        val doorCode = DoorCodeExtractor.extractDoorCode(candidate)
                        return if (doorCode != null && !streetPrefixes.any { candidate.lowercase().contains(it) }) doorCode else candidate
                    }
                }
            }

            // Trailing target check: "ponle la direccion Calle Mayor 12 a Marcos"
            if (studentName != null) {
                val studentLower = stripAccents(studentName.lowercase().trim())
                val toStudent = " a $studentLower"
                if (afterDir.endsWith(toStudent)) {
                    val candidate = afterDirOriginal.substring(0, afterDir.length - toStudent.length).trim()
                    if (candidate.isNotBlank()) {
                        val doorCode = DoorCodeExtractor.extractDoorCode(candidate)
                        return if (doorCode != null && !streetPrefixes.any { candidate.lowercase().contains(it) }) doorCode else candidate
                    }
                }
            }

            // Direct candidate after "direccion"
            val candidate = afterDirOriginal.trim().removePrefix(":").trim()
            if (studentName != null && candidate.equals("de $studentName", ignoreCase = true)) {
                return null
            }
            if (candidate.isNotBlank() && !candidate.startsWith("de ")) {
                val doorCode = DoorCodeExtractor.extractDoorCode(candidate)
                return if (doorCode != null && !streetPrefixes.any { candidate.lowercase().contains(it) }) doorCode else candidate
            }
        }

        // 3. Street prefixes in direct input
        if (streetPrefixes.any { lower.startsWith(it) }) {
            return original
        }

        return null
    }

    /**
     * Extracts a phone number from the query.
     */
    fun extractPhoneNumber(query: String): String? {
        val phoneRegex = Regex("""(?:\+?\d{1,3}[\s\-]?)?(?:[6789]\d{2}[\s\-]?\d{3}[\s\-]?\d{3}|[6789]\d{8}|\b\d{9}\b)""")
        val match = phoneRegex.find(query) ?: return null
        return match.value.replace(Regex("""[\s\-]"""), "").trim()
    }

    /**
     * Extracts a course or education level from the query.
     */
    fun extractCourseText(query: String): String? {
        val lower = query.lowercase().trim()
        val original = query.trim()

        val courseIdx = listOf(lower.indexOf("curso"), lower.indexOf("nivel"), lower.indexOf("grado")).filter { it >= 0 }.minOrNull()
        if (courseIdx != null) {
            val afterCourse = lower.substring(courseIdx + 5)
            val afterCourseOriginal = original.substring(Math.min(original.length, courseIdx + 5))
            val transitionMarkers = listOf(" a ", " por ", " es ", ": ", " sea ")
            for (m in transitionMarkers) {
                val tIdx = afterCourse.indexOf(m)
                if (tIdx >= 0) {
                    val candidate = afterCourseOriginal.substring(tIdx + m.length).trim()
                    if (candidate.isNotBlank()) return candidate
                }
            }
            val candidate = afterCourseOriginal.trim().removePrefix(":").trim()
            if (candidate.isNotBlank() && !candidate.startsWith("de ")) {
                return candidate
            }
        }

        val directKeywords = listOf("bachillerato", "bach", "eso", "primaria", "secundaria", "infantil", "universidad")
        for (kw in directKeywords) {
            val idx = lower.indexOf(kw)
            if (idx >= 0) {
                val prefix = original.substring(0, idx).trimEnd()
                val gradePrefixMatch = Regex("""\b(\d+[ºªo°]?\s*(?:de\s+)?(?:la\s+)?)$""", RegexOption.IGNORE_CASE).find(prefix)
                return if (gradePrefixMatch != null) {
                    original.substring(gradePrefixMatch.range.first).trim()
                } else {
                    original.substring(idx).trim()
                }
            }
        }

        return null
    }

    fun extractAllTimesInQuery(query: String): List<String> {
        val lower = stripAccents(query.lowercase().trim())
        val originalLower = query.lowercase().trim()
        val matchedIndices = BooleanArray(lower.length)

        data class TimeMatch(val index: Int, val timeStr: String)
        val matches = mutableListOf<TimeMatch>()

        val colonPattern = Regex("""\b(\d{1,2}):(\d{2})\b""")
        for (match in colonPattern.findAll(lower)) {
            val start = match.range.first
            val end = match.range.last
            val h = match.groupValues[1].toInt()
            val m = match.groupValues[2].toInt()
            if (h in 0..23 && m in 0..59) {
                for (i in start..end) {
                    matchedIndices[i] = true
                }
                val resolvedH = resolveHour(h, originalLower)
                matches.add(TimeMatch(start, "%02d:%02d".format(resolvedH, m)))
            }
        }

        val hourRangePattern = Regex("""\b(?:de\s+)?(\d{1,2}|$hourWordsRegexStr)\s+a\s+(\d{1,2}|$hourWordsRegexStr)\b""")
        for (match in hourRangePattern.findAll(lower)) {
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
                val h1 = parseHourString(match.groupValues[1])
                val h2 = parseHourString(match.groupValues[2])
                if (h1 != null && h1 in 0..23 && h2 != null && h2 in 0..23) {
                    for (i in start..end) {
                        matchedIndices[i] = true
                    }
                    val resH1 = resolveHour(h1, originalLower)
                    val resH2 = resolveHour(h2, originalLower)
                    matches.add(TimeMatch(start, "%02d:00".format(resH1)))
                    matches.add(TimeMatch(start + 1, "%02d:00".format(resH2)))
                }
            }
        }

        val hourPatterns = listOf(
            Regex("""\b(?:a\s+eso\s+de\s+|sobre\s+|para\s+|a\s+|de\s+)?(?:las?\s+)(\d{1,2}|$hourWordsRegexStr)\b"""),
            Regex("""\b(?:to|at|from)\s+(\d{1,2}|$hourWordsRegexStr)\b"""),
            Regex("""\b(\d{1,2}|$hourWordsRegexStr)\s*(?:h|hrs|horas|hours|pm|am|de\s+la\s+tarde|de\s+la\s+manana|de\s+la\s+noche)\b""")
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
                    val h = parseHourString(match.groupValues[1])
                    if (h != null && h in 0..23) {
                        for (i in start..end) {
                            matchedIndices[i] = true
                        }
                        val resolvedH = resolveHour(h, originalLower)
                        matches.add(TimeMatch(start, "%02d:00".format(resolvedH)))
                    }
                }
            }
        }

        return matches.sortedBy { it.index }.map { it.timeStr }
    }

    fun normalizeLlmTime(timeStr: String?): String? {
        if (timeStr == null) return null
        val cleaned = timeStr.trim().lowercase()
        if (cleaned.isEmpty()) return timeStr

        val parts = cleaned.split(":")
        val h = parts[0].toIntOrNull() ?: return timeStr
        val m = if (parts.size > 1) parts[1].toIntOrNull() ?: 0 else 0

        val resolvedH = if (h in 1..7) h + 12 else h
        return String.format(Locale.US, "%02d:%02d", resolvedH, m)
    }
}
