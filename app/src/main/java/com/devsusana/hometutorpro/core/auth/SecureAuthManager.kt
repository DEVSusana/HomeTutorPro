package com.devsusana.hometutorpro.core.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.devsusana.hometutorpro.domain.core.AuthValidator
import java.security.KeyStore
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec

class SecureAuthManager(
    private val sharedPreferences: SharedPreferences,
    private val cryptographyProvider: CryptographyProvider = AndroidCryptographyProvider()
) {

    fun saveCredentials(email: String, password: String, name: String, userId: String? = null): String {
        val idToSave = userId ?: UUID.randomUUID().toString()
        val salt = generateSalt()
        val hashedPassword = hashPassword(password, salt)
        sharedPreferences.edit().apply {
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
        val email = sharedPreferences.getString(KEY_EMAIL, null)
        val name = sharedPreferences.getString(KEY_NAME, null)
        // Password hash is not returned — use validateCredentials() instead
        return if (email != null && name != null) {
            Triple(email, "", name)
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
        val storedHash = sharedPreferences.getString(KEY_PASSWORD_HASH, null)
        val storedSalt = sharedPreferences.getString(KEY_PASSWORD_SALT, null)
        if (storedEmail == null || storedHash == null || storedSalt == null) return false
        return email == storedEmail && verifyPassword(password, storedHash, storedSalt)
    }

    fun clearCredentials() {
        sharedPreferences.edit().clear().apply()
    }

    fun validateEmail(email: String): Boolean = AuthValidator.isValidEmail(email)

    fun validatePassword(password: String): Boolean = AuthValidator.isValidPassword(password)

    fun updateName(newName: String) {
        sharedPreferences.edit().putString(KEY_NAME, newName).apply()
    }

    fun updateEmail(newEmail: String) {
        sharedPreferences.edit().putString(KEY_EMAIL, newEmail).apply()
    }

    fun updatePassword(newPassword: String) {
        val salt = generateSalt()
        val hashedPassword = hashPassword(newPassword, salt)
        sharedPreferences.edit()
            .putString(KEY_PASSWORD_HASH, hashedPassword)
            .putString(KEY_PASSWORD_SALT, salt)
            .apply()
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

    

        fun getNotes(): String = sharedPreferences.getString(KEY_NOTES, "") ?: ""

    

            fun updateNotes(notes: String) {
                sharedPreferences.edit().putString(KEY_NOTES, notes).apply()
            }
        
    fun encryptPII(text: String?): String = cryptographyProvider.encrypt(text)

    fun decryptPII(encrypted: String?): String = cryptographyProvider.decrypt(encrypted)

    /**
     * Default implementation using AndroidKeyStore and AES/GCM.
     */
    private class AndroidCryptographyProvider : CryptographyProvider {
        override fun encrypt(text: String?): String {
            if (text.isNullOrEmpty()) return ""
            return try {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                val secretKey = keyStore.getKey(MasterKey.DEFAULT_MASTER_KEY_ALIAS, null) as SecretKey
                
                cipher.init(Cipher.ENCRYPT_MODE, secretKey)
                
                val iv = cipher.iv
                val encryptedBytes = cipher.doFinal(text.toByteArray(Charsets.UTF_8))
                
                val combined = ByteArray(iv.size + encryptedBytes.size)
                System.arraycopy(iv, 0, combined, 0, iv.size)
                System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
                
                Base64.encodeToString(combined, Base64.NO_WRAP)
            } catch (e: Exception) {
                text
            }
        }

        override fun decrypt(encrypted: String?): String {
            if (encrypted.isNullOrEmpty()) return ""
            return try {
                val combined = Base64.decode(encrypted, Base64.NO_WRAP)
                if (combined.size < 12) return encrypted // Not encrypted or invalid

                val iv = combined.sliceArray(0 until 12)
                val encryptedBytes = combined.sliceArray(12 until combined.size)
                
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                val secretKey = keyStore.getKey(MasterKey.DEFAULT_MASTER_KEY_ALIAS, null) as SecretKey
                
                val spec = GCMParameterSpec(128, iv)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
                
                String(cipher.doFinal(encryptedBytes), Charsets.UTF_8)
            } catch (e: Exception) {
                encrypted
            }
        }
    }
        
            private fun generateSalt(): String {
        
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    private fun hashPassword(password: String, saltBase64: String): String {
        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val hash = factory.generateSecret(spec).encoded
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    private fun verifyPassword(password: String, storedHash: String, storedSalt: String): Boolean {
        val computedHash = hashPassword(password, storedSalt)
        return computedHash == storedHash
    }

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
        private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val PBKDF2_ITERATIONS = 600000
        private const val PBKDF2_KEY_LENGTH = 256
    }
}

    