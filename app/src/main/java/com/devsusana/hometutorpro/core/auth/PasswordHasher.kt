package com.devsusana.hometutorpro.core.auth

/**
 * Interface defining operations for hashing and verifying passwords securely.
 */
interface PasswordHasher {
    
    /** Generates a new secure random base64 salt. */
    fun generateSalt(): String
    
    /** Hashes a password with the given base64 salt. */
    fun hashPassword(password: String, saltBase64: String): String
    
    /** Verifies a password against a stored hash and base64 salt. */
    fun verifyPassword(password: String, storedHash: String, storedSalt: String): Boolean
}
