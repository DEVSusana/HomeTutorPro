package com.devsusana.hometutorpro.domain.auth

import com.devsusana.hometutorpro.data.security.Pbkdf2PasswordHasher
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying the behavior of [PasswordHasher] using its concrete implementation [Pbkdf2PasswordHasher].
 */
class PasswordHasherTest {

    private lateinit var passwordHasher: PasswordHasher

    /**
     * Sets up the [Pbkdf2PasswordHasher] concrete instance before each test.
     */
    @Before
    fun setUp() {
        passwordHasher = Pbkdf2PasswordHasher()
    }

    /**
     * Verifies that [PasswordHasher.generateSalt] generates a non-empty, non-null string.
     */
    @Test
    fun generateSalt_shouldReturnNonEmptyString() {
        val salt = passwordHasher.generateSalt()
        assertNotNull(salt)
        assertTrue(salt.isNotEmpty())
    }

    /**
     * Verifies that [PasswordHasher.hashPassword] produces consistent output hashes when using the same salt.
     */
    @Test
    fun hashPassword_shouldProduceConsistentHashForSameSalt() {
        val password = "MySuperSecretPassword123!"
        val salt = passwordHasher.generateSalt()

        val hash1 = passwordHasher.hashPassword(password, salt)
        val hash2 = passwordHasher.hashPassword(password, salt)

        assertEquals(hash1, hash2)
    }

    /**
     * Verifies that [PasswordHasher.hashPassword] produces distinct hashes for different salts with same password.
     */
    @Test
    fun hashPassword_shouldProduceDifferentHashesForDifferentSalts() {
        val password = "MySuperSecretPassword123!"
        val salt1 = passwordHasher.generateSalt()
        val salt2 = passwordHasher.generateSalt()

        val hash1 = passwordHasher.hashPassword(password, salt1)
        val hash2 = passwordHasher.hashPassword(password, salt2)

        assertNotEquals(hash1, hash2)
    }

    /**
     * Verifies that [PasswordHasher.verifyPassword] returns true when providing correct credentials.
     */
    @Test
    fun verifyPassword_shouldReturnTrueForCorrectPassword() {
        val password = "MySuperSecretPassword123!"
        val salt = passwordHasher.generateSalt()
        val hash = passwordHasher.hashPassword(password, salt)

        val isValid = passwordHasher.verifyPassword(password, hash, salt)

        assertTrue(isValid)
    }

    /**
     * Verifies that [PasswordHasher.verifyPassword] returns false when providing incorrect credentials.
     */
    @Test
    fun verifyPassword_shouldReturnFalseForIncorrectPassword() {
        val password = "MySuperSecretPassword123!"
        val wrongPassword = "WrongPassword123!"
        val salt = passwordHasher.generateSalt()
        val hash = passwordHasher.hashPassword(password, salt)

        val isValid = passwordHasher.verifyPassword(wrongPassword, hash, salt)

        assertFalse(isValid)
    }
}
