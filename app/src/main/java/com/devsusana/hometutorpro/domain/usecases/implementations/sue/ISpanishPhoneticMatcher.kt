package com.devsusana.hometutorpro.domain.usecases.implementations.sue

/**
 * Contract for phonetic matching and Spanish conversational query normalization.
 */
interface ISpanishPhoneticMatcher {
    /** Normalizes a name string by removing accents and mapping homophones in Spanish. */
    fun normalizeName(name: String): String

    /** Returns the consonant skeleton of a name by stripping vowels from its normalized form. */
    fun normalizeConsonants(name: String): String

    /** Normalizes a single colloquial or abbreviated subject name to its official standard name. */
    fun normalizeSingleSubject(rawSubject: String): String

    /** Normalizes colloquial or compound subject names to official standard names joined by comma. */
    fun normalizeSubjects(rawSubjects: String): String

    /** Alias for normalizeSubjects. */
    fun normalizeSubject(rawSubject: String): String

    /** Resolves conversational Spanish temporal idioms to 24-hour HH:mm representations. */
    fun resolveColloquialTimePhrase(query: String): String?
}
