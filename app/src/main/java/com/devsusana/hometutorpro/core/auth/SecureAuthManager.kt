package com.devsusana.hometutorpro.core.auth

import android.content.SharedPreferences
import com.devsusana.hometutorpro.domain.core.AuthValidator
import java.util.UUID

/**
 * Manages secure storage of credentials and sensitive user data.
 * Decoupled from cryptography and hashing implementations using constructor injection.
 */
class SecureAuthManager(
    private val encryptedSharedPreferences: SharedPreferences,
    private val cryptographyProvider: CryptographyProvider,
    private val passwordHasher: PasswordHasher
) {

    fun saveCredentials(email: String, credentialsToken: String, name: String, userId: String? = null): String {
        val idToSave = userId ?: UUID.randomUUID().toString()
        val salt = passwordHasher.generateSalt()
        val hashedPassword = passwordHasher.hashPassword(credentialsToken, salt)
        encryptedSharedPreferences.edit().apply {
            putString(KEY_EMAIL, email)
            putString(KEY_PASSWORD_HASH, hashedPassword)
            putString(KEY_PASSWORD_SALT, salt)
            putString(KEY_NAME, name)
            putString(KEY_USER_ID, idToSave)
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_WORKING_START_TIME, "08:00")
            putString(KEY_WORKING_END_TIME, "23:00")
            apply()
        }
        return idToSave
    }

    fun getCredentials(): Triple<String, String, String>? {
        val email = encryptedSharedPreferences.getString(KEY_EMAIL, null)
        val name = encryptedSharedPreferences.getString(KEY_NAME, null)
        // Password hash is not returned — use validateCredentials() instead
        return if (email != null && name != null) {
            Triple(email, "", name)
        } else {
            null
        }
    }

    fun getUserId(): String? = encryptedSharedPreferences.getString(KEY_USER_ID, null)

    fun getUserName(): String? = encryptedSharedPreferences.getString(KEY_NAME, null)

    fun getEmail(): String? = encryptedSharedPreferences.getString(KEY_EMAIL, null)

    fun isUserLoggedIn(): Boolean = encryptedSharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)

    fun validateCredentials(email: String, credentialsToken: String): Boolean {
        val storedEmail = encryptedSharedPreferences.getString(KEY_EMAIL, null)
        val storedHash = encryptedSharedPreferences.getString(KEY_PASSWORD_HASH, null)
        val storedSalt = encryptedSharedPreferences.getString(KEY_PASSWORD_SALT, null)
        if (storedEmail == null || storedHash == null || storedSalt == null) return false
        return email == storedEmail && passwordHasher.verifyPassword(credentialsToken, storedHash, storedSalt)
    }

    fun clearCredentials() {
        encryptedSharedPreferences.edit().clear().apply()
    }

    fun validateEmail(email: String): Boolean = AuthValidator.isValidEmail(email)

    fun validatePassword(password: String): Boolean = AuthValidator.isValidPassword(password)

    fun updateName(newName: String) {
        encryptedSharedPreferences.edit().putString(KEY_NAME, newName).apply()
    }

    fun updateEmail(newEmail: String) {
        encryptedSharedPreferences.edit().putString(KEY_EMAIL, newEmail).apply()
    }

    fun updatePassword(newCredentialsToken: String) {
        val salt = passwordHasher.generateSalt()
        val hashedPassword = passwordHasher.hashPassword(newCredentialsToken, salt)
        encryptedSharedPreferences.edit()
            .putString(KEY_PASSWORD_HASH, hashedPassword)
            .putString(KEY_PASSWORD_SALT, salt)
            .apply()
    }

    fun userExists(): Boolean = encryptedSharedPreferences.getString(KEY_EMAIL, null) != null

    fun getWorkingStartTime(): String = encryptedSharedPreferences.getString(KEY_WORKING_START_TIME, "08:00") ?: "08:00"

    fun getWorkingEndTime(): String = encryptedSharedPreferences.getString(KEY_WORKING_END_TIME, "23:00") ?: "23:00"

    fun updateWorkingStartTime(time: String) {
        encryptedSharedPreferences.edit().putString(KEY_WORKING_START_TIME, time).apply()
    }

    fun updateWorkingEndTime(time: String) {
        encryptedSharedPreferences.edit().putString(KEY_WORKING_END_TIME, time).apply()
    }

    fun getNotes(): String = encryptedSharedPreferences.getString(KEY_NOTES, "") ?: ""

    fun updateNotes(notes: String) {
        encryptedSharedPreferences.edit().putString(KEY_NOTES, notes).apply()
    }
        
    fun encryptPII(text: String?): String = cryptographyProvider.encrypt(text)

    fun decryptPII(encrypted: String?): String = cryptographyProvider.decrypt(encrypted)

    companion object {
        private const val PREFS_NAME = "secure_auth_prefs"
        private const val KEY_EMAIL = "email"
        private const val KEY_PASSWORD_HASH = "password_hash"
        private const val KEY_PASSWORD_SALT = "password_salt"
        private const val KEY_NAME = "name"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_WORKING_START_TIME = "working_start_time"
        private const val KEY_WORKING_END_TIME = "working_end_time"
        private const val KEY_NOTES = "notes"
    }
}