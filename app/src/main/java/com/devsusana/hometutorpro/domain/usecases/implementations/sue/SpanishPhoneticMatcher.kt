package com.devsusana.hometutorpro.domain.usecases.implementations.sue

import java.text.Normalizer
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phonetic matcher and normalizer tailored for Spanish spoken queries,
 * speech-to-text acoustic ambiguities, and colloquial educational terminology.
 */
@Singleton
class SpanishPhoneticMatcher @Inject constructor() {

    /**
     * Normalizes a name string by removing accents and mapping homophones in Spanish.
     */
    fun normalizeName(name: String): String {
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
    fun normalizeConsonants(name: String): String {
        val norm = normalizeName(name)
        return norm.replace(Regex("[aeiou]"), "")
    }

    /**
     * Normalizes colloquial or abbreviated subject names to their official standard names.
     */
    fun normalizeSubject(rawSubject: String): String {
        val clean = Normalizer.normalize(rawSubject.lowercase(Locale.ROOT).trim(), Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")

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
            else -> rawSubject.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }
    }

    /**
     * Resolves conversational Spanish temporal idioms to 24-hour HH:mm representations.
     */
    fun resolveColloquialTimePhrase(query: String): String? {
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
