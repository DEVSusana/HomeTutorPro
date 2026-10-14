package com.devsusana.hometutorpro.presentation.sue

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

/**
 * Unit tests for [SueSuggestionProvider].
 */
class SueSuggestionProviderTest {

    @Test
    fun `morning suggestions are provided between 06_00 and 13_59`() {
        val morningTimes = listOf(
            LocalTime.of(6, 0),
            LocalTime.of(9, 30),
            LocalTime.of(13, 59)
        )

        for (time in morningTimes) {
            val suggestions = SueSuggestionProvider.getSuggestionsForTime(time)
            assertFalse(suggestions.isEmpty())
            assertTrue(suggestions.contains("¿Qué clases tengo hoy?"))
            assertTrue(suggestions.contains("¿Quién me debe dinero?"))
            assertTrue(suggestions.contains("¿Tengo huecos libres hoy?"))
        }
    }

    @Test
    fun `afternoon suggestions are provided between 14_00 and 19_59`() {
        val afternoonTimes = listOf(
            LocalTime.of(14, 0),
            LocalTime.of(16, 45),
            LocalTime.of(19, 59)
        )

        for (time in afternoonTimes) {
            val suggestions = SueSuggestionProvider.getSuggestionsForTime(time)
            assertFalse(suggestions.isEmpty())
            assertTrue(suggestions.contains("Próxima clase"))
            assertTrue(suggestions.contains("¿Qué clases me quedan hoy?"))
            assertTrue(suggestions.contains("Huecos libres esta tarde"))
        }
    }

    @Test
    fun `evening and night suggestions are provided between 20_00 and 05_59`() {
        val nightTimes = listOf(
            LocalTime.of(20, 0),
            LocalTime.of(23, 15),
            LocalTime.of(2, 30),
            LocalTime.of(5, 59)
        )

        for (time in nightTimes) {
            val suggestions = SueSuggestionProvider.getSuggestionsForTime(time)
            assertFalse(suggestions.isEmpty())
            assertTrue(suggestions.contains("¿Qué clases tengo mañana?"))
            assertTrue(suggestions.contains("Resumen de hoy"))
            assertTrue(suggestions.contains("¿Quién tiene saldo pendiente?"))
        }
    }

    @Test
    fun `all suggestions are non-blank and non-trivial`() {
        val allTimes = listOf(
            LocalTime.of(8, 0),
            LocalTime.of(15, 0),
            LocalTime.of(21, 0)
        )

        for (time in allTimes) {
            val suggestions = SueSuggestionProvider.getSuggestionsForTime(time)
            for (suggestion in suggestions) {
                assertTrue("Suggestion should not be blank", suggestion.isNotBlank())
                assertTrue("Suggestion should be a reasonable prompt length", suggestion.length >= 5)
            }
        }
    }
}
