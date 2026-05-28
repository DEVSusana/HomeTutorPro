package com.devsusana.hometutorpro.data.security

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class Pbkdf2PasswordHasherTest {

    private lateinit var passwordHasher: Pbkdf2PasswordHasher

    @Before
    fun setUp() {
        passwordHasher = Pbkdf2PasswordHasher()
    }

    @Test
    fun generateSalt_shouldReturnNonEmptyString() {
        val salt = passwordHasher.generateSalt()
        assertNotNull(salt)
        assertTrue(salt.isNotEmpty())
    }

    @Test
    fun hashPassword_shouldProduceConsistentHashForSameSalt() {
        val password = "MySuperSecretPassword123!"
        val salt = passwordHasher.generateSalt()

        val hash1 = passwordHasher.hashPassword(password, salt)
        val hash2 = passwordHasher.hashPassword(password, salt)

        assertEquals(hash1, hash2)
    }

    @Test
    fun hashPassword_shouldProduceDifferentHashesForDifferentSalts() {
        val password = "MySuperSecretPassword123!"
        val salt1 = passwordHasher.generateSalt()
        val salt2 = passwordHasher.generateSalt()

        val hash1 = passwordHasher.hashPassword(password, salt1)
        val hash2 = passwordHasher.hashPassword(password, salt2)

        assertNotEquals(hash1, hash2)
    }

    @Test
    fun verifyPassword_shouldReturnTrueForCorrectPassword() {
        val password = "MySuperSecretPassword123!"
        val salt = passwordHasher.generateSalt()
        val hash = passwordHasher.hashPassword(password, salt)

        val isValid = passwordHasher.verifyPassword(password, hash, salt)

        assertTrue(isValid)
    }

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
