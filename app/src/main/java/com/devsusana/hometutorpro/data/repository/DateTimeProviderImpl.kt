package com.devsusana.hometutorpro.data.repository

import com.devsusana.hometutorpro.domain.repository.DateTimeProvider
import java.time.LocalDateTime
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [DateTimeProvider] that delegates queries to system-wide defaults.
 */
class DateTimeProviderImpl @Inject constructor() : DateTimeProvider {
    /**
     * Provides the current date and time using [LocalDateTime.now].
     */
    override fun getNow(): LocalDateTime = LocalDateTime.now()

    /**
     * Retrieves the default system [Locale].
     */
    override fun getLocale(): Locale = Locale.getDefault()

    /**
     * Returns the current epoch millisecond timestamp using [System.currentTimeMillis].
     */
    override fun currentTimeMillis(): Long = System.currentTimeMillis()

    /**
     * Calculates the start of the current day in milliseconds since epoch.
     */
    override fun getStartOfDayMillis(): Long {
        return getNow().toLocalDate()
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }
}
