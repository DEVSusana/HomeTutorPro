package com.devsusana.hometutorpro.data.security

import com.devsusana.hometutorpro.domain.auth.PasswordHasher
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * PBKDF2-based implementation of [PasswordHasher].
 *
 * Resides in the data layer because it provides the concrete security implementation.
 * Uses [java.util.Base64] (available since Java 8) instead of [android.util.Base64]
 * so the class can be unit-tested on a standard JVM without Robolectric.
 */
class Pbkdf2PasswordHasher : PasswordHasher {

    /**
     * Generates a cryptographically secure random 16-byte salt, encoded as Base64.
     *
     * @return A Base64 encoded string of the generated salt.
     */
    override fun generateSalt(): String {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return Base64.getEncoder().encodeToString(salt)
    }

    /**
     * Hashes [password] using PBKDF2WithHmacSHA256 with [saltBase64].
     *
     * @param password The plaintext password to hash.
     * @param saltBase64 The Base64 encoded salt.
     * @return The Base64 encoded hash string.
     */
    override fun hashPassword(password: String, saltBase64: String): String {
        val salt = Base64.getDecoder().decode(saltBase64)
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val hash = factory.generateSecret(spec).encoded
        return Base64.getEncoder().encodeToString(hash)
    }

    /**
     * Verifies [password] against [storedHash] using [storedSalt].
     *
     * @param password The plaintext password to verify.
     * @param storedHash The original Base64 encoded hash to compare against.
     * @param storedSalt The Base64 encoded salt used during hashing.
     * @return True if the password matches, false otherwise.
     */
    override fun verifyPassword(password: String, storedHash: String, storedSalt: String): Boolean {
        val computedHash = hashPassword(password, storedSalt)
        return computedHash == storedHash
    }

    companion object {
        private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val PBKDF2_ITERATIONS = 600000
        private const val PBKDF2_KEY_LENGTH = 256
    }
}
