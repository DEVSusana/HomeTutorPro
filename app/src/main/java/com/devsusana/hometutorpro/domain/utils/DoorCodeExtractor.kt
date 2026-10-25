package com.devsusana.hometutorpro.domain.utils

/**
 * Utility to extract door, intercom, or gate access codes from student addresses or notes.
 *
 * This provides a focused, privacy-friendly access snippet (e.g., "Portero: 1402", "Código: 5678")
 * suitable for display on the home screen widget when arriving at a student's residence.
 */
object DoorCodeExtractor {

    // Matches explicit keywords followed by separator, conversational "es (el)", or target transition "a / por" (e.g. "portero de Hugo a 387", "edítale el portero a Hugo a 4B")
    private val EXPLICIT_CODE_REGEX = Regex(
        "(?i)\\b(?:(?:el|de|del|como)\\s+)?(portero\\s+autom[aá]tico|c[oó]digo(?:\\s+(?:de|del))?\\s+(?:acceso|portero|portal|puerta|entrada|porte)|door\\s*code|entry\\s*code|gate\\s*code|c[oó]digo|codigo|portero|porte|telefonillo|intercom|interfono|timbre|buzzer)\\b(?:\\s+(?:de|a|del)\\s+[\\p{L}]+(?:\\s+[\\p{L}]+)?)?\\s*(?:[:#=\\-]\\s*|\\s+es\\s+(?:el\\s+|al\\s+)?|\\s+(?:a|por|como|en)\\s+|\\s+el\\s+|\\s+al\\s+)\\s*([\\p{L}0-9ºª#\\-_/]+(?:\\s+[\\p{L}0-9ºª#\\-_/]+){0,3})"
    )

    // Matches keywords without a separator but followed directly by a code with digits (e.g. "portero 14B", "telefonillo 23", "codigo de porte 387")
    private val DIRECT_CODE_REGEX = Regex(
        "(?i)\\b(?:(?:el|de|del|como)\\s+)?(portero\\s+autom[aá]tico|c[oó]digo(?:\\s+(?:de|del))?\\s+(?:acceso|portero|portal|puerta|entrada|porte)|door\\s*code|entry\\s*code|gate\\s*code|portero|porte|telefonillo|intercom|interfono|buzzer)\\b(?:\\s+(?:de|a|del)\\s+[\\p{L}]+(?:\\s+[\\p{L}]+)?)?\\s*(?:\\s+es\\s+(?:el\\s+|al\\s+)?|\\s+(?:a|por|como|en)\\s+|\\s+el\\s+|\\s+al\\s+)?\\s+([\\p{L}0-9ºª#\\-_/]*\\d+[\\p{L}0-9ºª#\\-_/]*(?:\\s+[\\p{L}0-9ºª#\\-_/]+){0,2})"
    )

    // Matches dial action phrases: "marcar 1402", "marcar el 1402", "pulsar 23B en el telefonillo"
    private val DIAL_ACTION_REGEX = Regex(
        "(?i)\\b(?:marcar|pulsar|llamar\\s+al)\\s+(?:el\\s+|al\\s+)?([\\p{L}0-9ºª#\\-_/]+)(?:\\s+(?:en\\s+el\\s+|al\\s+)?(portero|telefonillo|intercom))?"
    )

    // Matches timbre with names: e.g. "Timbre: Gómez", "Timbre Familia Pérez"
    private val TIMBRE_NAME_REGEX = Regex(
        "(?i)\\btimbre\\b\\s*[:#=\\-]?\\s*([\\p{L}]+(?:\\s+[\\p{L}]+){0,2})"
    )

    /**
     * Extracts a door / intercom code by inspecting the [address] first,
     * and falling back to [notes] if no code was detected.
     *
     * @param address Physical address string of the student.
     * @param notes Additional tutor notes regarding the student.
     * @return Formatted snippet (e.g. "Portero: 1402") or null if none found.
     */
    fun extractDoorCode(address: String?, notes: String? = null): String? {
        val fromAddress = address?.takeIf { it.isNotBlank() }?.let { extractFromText(it) }
        if (!fromAddress.isNullOrBlank()) return fromAddress

        val fromNotes = notes?.takeIf { it.isNotBlank() }?.let { extractFromText(it) }
        if (!fromNotes.isNullOrBlank()) return fromNotes

        return null
    }

    /**
     * Extracts a door / intercom code from a single string.
     *
     * @param text Input string to inspect.
     * @return Formatted snippet or null if none found.
     */
    fun extractFromText(text: String): String? {
        if (text.isBlank()) return null

        // 1. Try explicit keyword with separator (e.g. "Portero: 1402", "Código: #1234")
        val explicitMatch = EXPLICIT_CODE_REGEX.find(text)
        if (explicitMatch != null) {
            val label = explicitMatch.groupValues[1]
            val code = cleanCode(explicitMatch.groupValues[2])
            if (isValidCode(label, code)) {
                return formatResult(label, code)
            }
        }

        // 2. Try direct code keyword with code containing digits (e.g. "Portero 14B", "Telefonillo 23")
        val directMatch = DIRECT_CODE_REGEX.find(text)
        if (directMatch != null) {
            val label = directMatch.groupValues[1]
            val code = cleanCode(directMatch.groupValues[2])
            if (isValidCode(label, code)) {
                return formatResult(label, code)
            }
        }

        // 3. Try dial action phrase (e.g. "marcar el 1402", "marcar 14B en el telefonillo")
        val dialMatch = DIAL_ACTION_REGEX.find(text)
        if (dialMatch != null) {
            val code = cleanCode(dialMatch.groupValues[1])
            val target = dialMatch.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() } ?: "portero"
            if (code.any { it.isDigit() }) {
                return formatResult(target, code)
            }
        }

        // 4. Try timbre with name (e.g. "Timbre: Gómez")
        val timbreMatch = TIMBRE_NAME_REGEX.find(text)
        if (timbreMatch != null) {
            val name = cleanCode(timbreMatch.groupValues[1])
            if (name.isNotBlank()) {
                return "Timbre: $name"
            }
        }

        return null
    }

    private fun cleanCode(code: String): String {
        return code.trim()
            .trimEnd(',', '.', ';', ':', '!', '?')
            .replace(Regex("\\s+"), " ")
    }

    private fun isValidCode(label: String, code: String): Boolean {
        if (code.isBlank() || code.length > 25) return false

        val lowerLabel = label.lowercase()
        // If the label is just generic "código" or "codigo", require at least one digit to avoid false positives
        if (lowerLabel == "código" || lowerLabel == "codigo") {
            return code.any { it.isDigit() }
        }

        return true
    }

    private fun formatResult(label: String, code: String): String {
        val lower = label.lowercase().trim()
        val formattedLabel = when {
            lower.contains("telefonillo") -> "Telefonillo"
            lower.contains("intercom") || lower.contains("interfono") -> "Intercom"
            lower.contains("timbre") -> "Timbre"
            lower.contains("buzzer") -> "Buzzer"
            lower.contains("door code") || lower.contains("entry code") || lower.contains("gate code") -> "Door code"
            lower.contains("portero") || lower.contains("porte") -> "Portero"
            else -> "Código"
        }
        return "$formattedLabel: $code"
    }
}
