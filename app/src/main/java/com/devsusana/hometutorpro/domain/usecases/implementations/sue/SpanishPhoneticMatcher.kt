package com.devsusana.hometutorpro.domain.usecases.implementations.sue

import java.text.Normalizer
import java.util.Locale
import javax.inject.Inject

/**
 * Phonetic matcher and normalizer tailored for Spanish spoken queries,
 * speech-to-text acoustic ambiguities, and colloquial educational terminology.
 */
class SpanishPhoneticMatcher @Inject constructor() : ISpanishPhoneticMatcher {

    /**
     * Normalizes a name string by removing accents and mapping homophones in Spanish.
     */
    override fun normalizeName(name: String): String {
        val temp = Normalizer.normalize(name.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        val cleaned = temp.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        return cleaned
            .replace("christian", "cristian")
            .replace("ph", "f")
            .replace("th", "t")
            .replace("ch", "c")
            .replace("k", "c")
            .replace("y", "i")
            .replace("v", "b")
            .replace("z", "s")
            .trim()
    }

    /**
     * Returns the consonant skeleton of a name by stripping vowels from its normalized form.
     */
    override fun normalizeConsonants(name: String): String {
        val norm = normalizeName(name)
        return norm.replace(Regex("[aeiou]"), "")
    }

    /**
     * Normalizes a single colloquial or abbreviated subject name to its official standard name.
     */
    override fun normalizeSingleSubject(rawSubject: String): String {
        val trimmed = rawSubject.trim()
        if (trimmed.isBlank()) return ""
        val clean = Normalizer.normalize(trimmed.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")

        // Filter out non-subject conversational filler words
        val nonSubjectWords = setOf(
            "clase", "clases", "dar clase", "dar clases", "la clase", "las clases",
            "general", "ninguna", "nada", "asignatura", "asignaturas"
        )
        if (clean in nonSubjectWords) return ""

        return when (clean) {
            "mates", "mate", "matematicas" -> "Matemáticas"
            "lengua", "castellano", "literatura", "lengua y literatura" -> "Lengua y Literatura"
            "ingles", "english" -> "Inglés"
            "frances", "french" -> "Francés"
            "quimica", "quim" -> "Química"
            "fisica" -> "Física"
            "cono", "conocimiento del medio" -> "Conocimiento del Medio"
            "bio", "biologia" -> "Biología"
            "geo", "geografia" -> "Geografía"
            "historia" -> "Historia"
            "filo", "filosofia" -> "Filosofía"
            "economia" -> "Economía"
            "musica" -> "Música"
            "plastica", "dibujo" -> "Plástica"
            "latin" -> "Latín"
            "griego" -> "Griego"
            "tecnologia", "tecno" -> "Tecnología"
            "informatica" -> "Informática"
            else -> trimmed.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }
    }

    /**
     * Normalizes colloquial, abbreviated, or compound subject names (e.g. "mates y física", "lengua, inglés")
     * to their official standard names joined by comma. Returns an empty string if blank or non-subject filler.
     */
    override fun normalizeSubjects(rawSubjects: String): String {
        if (rawSubjects.isBlank()) return ""
        val splitRegex = Regex("""\s*(?:,|;|/|\s+y\s+|\s+e\s+)\s*""", RegexOption.IGNORE_CASE)
        val items = rawSubjects.split(splitRegex)
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (items.isEmpty()) return ""

        val normalized = items.mapNotNull { single ->
            val res = normalizeSingleSubject(single)
            if (res.isNotBlank()) res else null
        }
        return normalized.distinct().joinToString(", ")
    }

    /**
     * Normalizes colloquial or abbreviated subject names to their official standard names.
     */
    override fun normalizeSubject(rawSubject: String): String = normalizeSubjects(rawSubject)

    /**
     * Resolves conversational Spanish temporal idioms to 24-hour HH:mm representations.
     */
    override fun resolveColloquialTimePhrase(query: String): String? {
        val clean = Normalizer.normalize(query.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")

        return when {
            "despues de comer" in clean || "al salir de comer" in clean -> "15:00"
            "a primera hora de la tarde" in clean || "primera hora de la tarde" in clean -> "16:00"
            "a media tarde" in clean || "media tarde" in clean -> "17:30"
            "a ultima hora de la tarde" in clean || "al final de la tarde" in clean -> "19:30"
            "a primera hora de la manana" in clean || "primera hora de la manana" in clean -> "09:00"
            "a media manana" in clean || "media manana" in clean -> "11:30"
            "al mediodia" in clean || "a mediodia" in clean -> "12:00"
            else -> null
        }
    }
}
