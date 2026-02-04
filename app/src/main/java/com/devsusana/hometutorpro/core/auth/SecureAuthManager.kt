package com.devsusana.hometutorpro.core.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.UUID

@Suppress("DEPRECATION")
class SecureAuthManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveCredentials(email: String, password: String, name: String, userId: String? = null): String {
        val idToSave = userId ?: UUID.randomUUID().toString()
        sharedPreferences.edit().apply {
            putString(KEY_EMAIL, email)
            putString(KEY_PASSWORD, password)
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
        val email = sharedPreferences.getString(KEY_EMAIL, null)
        val password = sharedPreferences.getString(KEY_PASSWORD, null)
        val name = sharedPreferences.getString(KEY_NAME, null)

        return if (email != null && password != null && name != null) {
            Triple(email, password, name)
        } else {
            null
        }
    }

    fun getUserId(): String? = sharedPreferences.getString(KEY_USER_ID, null)

    fun getUserName(): String? = sharedPreferences.getString(KEY_NAME, null)

    fun getEmail(): String? = sharedPreferences.getString(KEY_EMAIL, null)

    fun isUserLoggedIn(): Boolean = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)

    fun validateCredentials(email: String, password: String): Boolean {
        val storedEmail = sharedPreferences.getString(KEY_EMAIL, null)
        val storedPassword = sharedPreferences.getString(KEY_PASSWORD, null)
        return email == storedEmail && password == storedPassword
    }

    fun clearCredentials() {
        sharedPreferences.edit().clear().apply()
    }

    fun validateEmail(email: String): Boolean {
        return email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun validatePassword(password: String): Boolean = password.length >= 6

    fun updateName(newName: String) {
        sharedPreferences.edit().putString(KEY_NAME, newName).apply()
    }

    fun updateEmail(newEmail: String) {
        sharedPreferences.edit().putString(KEY_EMAIL, newEmail).apply()
    }

    fun updatePassword(newPassword: String) {
        sharedPreferences.edit().putString(KEY_PASSWORD, newPassword).apply()
    }

    fun userExists(): Boolean = sharedPreferences.getString(KEY_EMAIL, null) != null

    fun getWorkingStartTime(): String = sharedPreferences.getString(KEY_WORKING_START_TIME, "08:00") ?: "08:00"

    fun getWorkingEndTime(): String = sharedPreferences.getString(KEY_WORKING_END_TIME, "23:00") ?: "23:00"

    fun updateWorkingStartTime(time: String) {
        sharedPreferences.edit().putString(KEY_WORKING_START_TIME, time).apply()
    }

    fun updateWorkingEndTime(time: String) {
        sharedPreferences.edit().putString(KEY_WORKING_END_TIME, time).apply()
    }

    companion object {
        private const val PREFS_NAME = "secure_auth_prefs"
        private const val KEY_EMAIL = "email"
        private const val KEY_PASSWORD = "password"
        private const val KEY_NAME = "name"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_WORKING_START_TIME = "working_start_time"
        private const val KEY_WORKING_END_TIME = "working_end_time"
    }
}