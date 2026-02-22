package com.devsusana.hometutorpro.domain.core

/**
 * Centralized authentication validation rules.
 * Fixes technical debt point 2.5 by providing a single point of truth.
 */
object AuthValidator {
    private const val MIN_PASSWORD_LENGTH = 6
    private val EMAIL_REGEX = """^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$""".toRegex()

    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && EMAIL_REGEX.matches(email)
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= MIN_PASSWORD_LENGTH
    }
}
