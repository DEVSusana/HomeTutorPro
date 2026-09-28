package com.devsusana.hometutorpro.domain.repository

import java.time.LocalDateTime
import java.util.Locale

/**
 * Provider interface to decouple date/time and locale dependencies from system-wide statics.
 * Allows clean mocking and deterministic date/time assertions in tests.
 */
interface DateTimeProvider {
    /** Returns the current system date and time. */
    fun getNow(): LocalDateTime

    /** Returns the default system locale. */
    fun getLocale(): Locale
}
