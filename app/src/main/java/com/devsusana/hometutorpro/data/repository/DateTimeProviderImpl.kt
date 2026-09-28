package com.devsusana.hometutorpro.data.repository

import com.devsusana.hometutorpro.domain.repository.DateTimeProvider
import java.time.LocalDateTime
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [DateTimeProvider] that delegates queries to system-wide defaults.
 */
@Singleton
class DateTimeProviderImpl @Inject constructor() : DateTimeProvider {
    override fun getNow(): LocalDateTime = LocalDateTime.now()
    override fun getLocale(): Locale = Locale.getDefault()
}
