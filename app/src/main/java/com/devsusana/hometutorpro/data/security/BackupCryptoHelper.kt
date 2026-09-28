package com.devsusana.hometutorpro.data.security

import com.devsusana.hometutorpro.data.models.EncryptedBackupFile
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Helper object providing AES-256-GCM authenticated encryption and PBKDF2 key derivation
 * for application backup files.
 */
object BackupCryptoHelper {

    private const val ALGORITHM = "AES-256-GCM-PBKDF2"
    private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATION_COUNT = 10_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH_BYTES = 16
    private const val IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128

    private val secureRandom = SecureRandom()

    /**
     * Encrypts [plainText] using AES-256-GCM with a secret key derived from [secretKeyString].
     *
     * @param plainText The plain text content (e.g. JSON string).
     * @param secretKeyString The secret key identifier (e.g. professor UID).
     * @return An [EncryptedBackupFile] container holding the salt, IV, and ciphertext.
     */
    fun encrypt(plainText: String, secretKeyString: String): EncryptedBackupFile {
        require(secretKeyString.isNotBlank()) { "Secret key string cannot be blank" }

        val salt = ByteArray(SALT_LENGTH_BYTES).apply { secureRandom.nextBytes(this) }
        val iv = ByteArray(IV_LENGTH_BYTES).apply { secureRandom.nextBytes(this) }

        val secretKey = deriveKey(secretKeyString, salt)
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))

        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        val encoder = Base64.getEncoder()
        return EncryptedBackupFile(
            version = 2,
            algorithm = ALGORITHM,
            saltBase64 = encoder.encodeToString(salt),
            ivBase64 = encoder.encodeToString(iv),
            cipherTextBase64 = encoder.encodeToString(cipherBytes),
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Decrypts an [EncryptedBackupFile] using the provided [secretKeyString].
     *
     * @param encryptedFile The encrypted backup file container.
     * @param secretKeyString The secret key identifier (e.g. professor UID).
     * @return The decrypted plain text string.
     * @throws SecurityException If decryption fails (invalid key or corrupted data).
     */
    fun decrypt(encryptedFile: EncryptedBackupFile, secretKeyString: String): String {
        require(secretKeyString.isNotBlank()) { "Secret key string cannot be blank" }

        return try {
            val decoder = Base64.getDecoder()
            val salt = decoder.decode(encryptedFile.saltBase64)
            val iv = decoder.decode(encryptedFile.ivBase64)
            val cipherBytes = decoder.decode(encryptedFile.cipherTextBase64)

            val secretKey = deriveKey(secretKeyString, salt)
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))

            val decryptedBytes = cipher.doFinal(cipherBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            throw SecurityException("Failed to decrypt backup. Invalid key or corrupted file: ${e.message}", e)
        }
    }

    private fun deriveKey(passphrase: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }
}
