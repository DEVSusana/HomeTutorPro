package com.devsusana.hometutorpro.core.utils

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Sanitized logger to prevent PII leakage in breadcrumbs and logs.
 */
object SafeLogger {
    private const val TAG_PREFIX = "HTP_"
    
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val sanitizedMessage = sanitize(message)
        Log.e(TAG_PREFIX + tag, sanitizedMessage, throwable)
        
        FirebaseCrashlytics.getInstance().apply {
            log(sanitizedMessage)
            throwable?.let { recordException(it) }
        }
    }

    fun d(tag: String, message: String) {
        Log.d(TAG_PREFIX + tag, sanitize(message))
    }

    private fun sanitize(message: String): String {
        // Regex to match potential emails
        val emailRegex = """[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,6}""".toRegex()
        // Regex to match potential phone numbers (simple version)
        val phoneRegex = """(\+?\d[\d -]{7,12}\d)""".toRegex()
        // Regex for potential Firestore paths with names
        val pathRegex = """(students/)[^/]+""".toRegex()

        return message
            .replace(emailRegex, "[EMAIL_REDACTED]")
            .replace(phoneRegex, "[PHONE_REDACTED]")
            .replace(pathRegex, "$1[ID_REDACTED]")
    }
}
