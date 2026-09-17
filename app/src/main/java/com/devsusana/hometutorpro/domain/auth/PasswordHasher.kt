package com.devsusana.hometutorpro.domain.auth

/**
 * Interface defining operations for hashing and verifying passwords securely.
 */
interface PasswordHasher {
    /**
     * Generates a new secure random salt, Base64 encoded.
     *
     * @return A Base64 encoded salt string.
     */
    fun generateSalt(): String

    /**
     * Hashes a password with the given salt.
     *
     * @param password The plaintext password.
     * @param saltBase64 The Base64 encoded salt.
     * @return The Base64 encoded hashed password.
     */
    fun hashPassword(password: String, saltBase64: String): String

    /**
     * Verifies a password against a stored hash and salt.
     *
     * @param password The plaintext password to check.
     * @param storedHash The Base64 encoded hash.
     * @param storedSalt The Base64 encoded salt.
     * @return True if the password matches the hash, false otherwise.
     */
    fun verifyPassword(password: String, storedHash: String, storedSalt: String): Boolean
}
