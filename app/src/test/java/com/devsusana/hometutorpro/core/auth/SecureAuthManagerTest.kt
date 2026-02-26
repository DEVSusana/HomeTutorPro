package com.devsusana.hometutorpro.core.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33]) // Explicitly setting SDK to avoid potential Robolectric issues with newer ones
class SecureAuthManagerTest {

    private lateinit var context: Context
    private lateinit var authManager: SecureAuthManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val prefs = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
        // Simple provider for testing that doesn't use AndroidKeyStore
        val mockCrypto = object : CryptographyProvider {
            override fun encrypt(text: String?): String = text?.reversed() ?: ""
            override fun decrypt(encrypted: String?): String = encrypted?.reversed() ?: ""
        }
        authManager = SecureAuthManager(prefs, mockCrypto)
        authManager.clearCredentials()
    }

    @Test
    fun `saveCredentials should persist user data and indicate logged in`() {
        // Given
        val email = "test@example.com"
        val password = "Password123!"
        val name = "Test User"
        val userId = "user123"

        // When
        val savedId = authManager.saveCredentials(email, password, name, userId)

        // Then
        assertEquals(userId, savedId)
        assertEquals(userId, authManager.getUserId())
        assertEquals(name, authManager.getUserName())
        assertEquals(email, authManager.getEmail())
        assertTrue(authManager.isUserLoggedIn())
    }

    @Test
    fun `validateCredentials should return true for correct credentials`() {
        // Given
        val email = "valid@example.com"
        val password = "SecurePassword1!"
        authManager.saveCredentials(email, password, "User")

        // When
        val isValid = authManager.validateCredentials(email, password)

        // Then
        assertTrue(isValid)
    }

    @Test
    fun `validateCredentials should return false for incorrect password`() {
        // Given
        val email = "valid@example.com"
        val password = "CorrectPassword1!"
        authManager.saveCredentials(email, password, "User")

        // When
        val isValid = authManager.validateCredentials(email, "WrongPassword")

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `clearCredentials should remove all data`() {
        // Given
        authManager.saveCredentials("test@test.com", "pass", "Name")
        assertTrue(authManager.userExists())

        // When
        authManager.clearCredentials()

        // Then
        assertFalse(authManager.userExists())
        assertNull(authManager.getUserId())
        assertFalse(authManager.isUserLoggedIn())
    }

    @Test
    fun `updateName should change stored name`() {
        // Given
        authManager.saveCredentials("test@test.com", "pass", "Old Name")

        // When
        authManager.updateName("New Name")

        // Then
        assertEquals("New Name", authManager.getUserName())
    }

    @Test
    fun `updateWorkingTimes should persist times`() {
        // When
        authManager.updateWorkingStartTime("09:00")
        authManager.updateWorkingEndTime("21:00")

        // Then
        assertEquals("09:00", authManager.getWorkingStartTime())
        assertEquals("21:00", authManager.getWorkingEndTime())
    }

    @Test
    fun `encrypt and decrypt PII should return original text`() {
        // Note: Encryption might fall back to original text if KeyStore is not fully supported in Robolectric,
        // but it should still pass the assertion.
        
        // Given
        val originalText = "Sensitive PII Information"

        // When
        val encrypted = authManager.encryptPII(originalText)
        val decrypted = authManager.decryptPII(encrypted)

        // Then
        assertEquals(originalText, decrypted)
    }

    @Test
    fun `encrypt and decrypt PII should handle null or empty`() {
        // When
        assertEquals("", authManager.encryptPII(null))
        assertEquals("", authManager.encryptPII(""))
        assertEquals("", authManager.decryptPII(null))
        assertEquals("", authManager.decryptPII(""))
    }
}
