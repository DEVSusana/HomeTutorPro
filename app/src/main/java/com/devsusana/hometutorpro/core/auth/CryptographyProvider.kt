package com.devsusana.hometutorpro.core.auth

/**
 * Interface for PII encryption/decryption.
 * Allows for different implementations (e.g., AndroidKeyStore for production,
 * No-op or different algorithms for unit tests).
 */
interface CryptographyProvider {
    fun encrypt(text: String?): String
    fun decrypt(encrypted: String?): String
}
