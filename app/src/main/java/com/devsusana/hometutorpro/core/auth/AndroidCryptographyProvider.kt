package com.devsusana.hometutorpro.core.auth

import android.util.Base64
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android Keystore implementation of [CryptographyProvider].
 * Uses AES/GCM/NoPadding for secure encryption and decryption.
 */
class AndroidCryptographyProvider : CryptographyProvider {
    
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
