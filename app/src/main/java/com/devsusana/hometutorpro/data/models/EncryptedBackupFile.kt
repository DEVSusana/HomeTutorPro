package com.devsusana.hometutorpro.data.models

import kotlinx.serialization.Serializable

/**
 * Encrypted container model for exported application backups.
 *
 * Uses AES-256-GCM authenticated encryption with PBKDF2 key derivation.
 * Ensures data at rest in exported backup files is protected and only decryptable
 * by the user possessing the corresponding account UID.
 */
@Serializable
data class EncryptedBackupFile(
    val version: Int = 2,
    val algorithm: String = "AES-256-GCM-PBKDF2",
    val saltBase64: String,
    val ivBase64: String,
    val cipherTextBase64: String,
    val timestamp: Long = System.currentTimeMillis()
)
