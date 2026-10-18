package com.devsusana.hometutorpro.presentation.sue

import java.time.LocalTime

/**
 * Generates dynamic, context-aware suggestion prompts for SUE based on the time of day
 * and typical private tutor teaching routines.
 */
object SueSuggestionProvider {

    /**
     * Returns a list of natural conversational suggestion strings appropriate for [time].
     *
     * - Morning (06:00 - 13:59): Day planning, overview of today's classes, debt checks.
     * - Afternoon (14:00 - 19:59): Peak tutoring hours, upcoming classes, payment logging, afternoon gaps.
     * - Evening / Night (20:00 - 05:59): Day closing recap, preparing tomorrow's schedule.
     */
    fun getSuggestionsForTime(time: LocalTime = LocalTime.now()): List<String> {
        val hour = time.hour
        return when (hour) {
            in 6..13 -> listOf(
                "¿Qué clases tengo hoy?",
                "¿Quién me debe dinero?",
                "¿Tengo huecos libres hoy?",
                "Próxima clase"
            )
            in 14..19 -> listOf(
                "Próxima clase",
                "¿Qué clases me quedan hoy?",
                "Huecos libres esta tarde",
                "Registrar un pago"
            )
            else -> listOf(
                "¿Qué clases tengo mañana?",
                "Resumen de hoy",
                "¿Quién tiene saldo pendiente?",
                "Huecos libres mañana"
            )
        }
    }
}
