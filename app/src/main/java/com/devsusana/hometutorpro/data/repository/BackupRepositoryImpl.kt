package com.devsusana.hometutorpro.data.repository

import androidx.room.withTransaction
import com.devsusana.hometutorpro.core.utils.SafeLogger
import com.devsusana.hometutorpro.data.local.AppDatabase
import com.devsusana.hometutorpro.data.models.AppBackup
import com.devsusana.hometutorpro.data.models.EncryptedBackupFile
import com.devsusana.hometutorpro.data.security.BackupCryptoHelper
import com.devsusana.hometutorpro.data.security.SecureAuthManager
import com.devsusana.hometutorpro.domain.repository.BackupRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [BackupRepository] that interacts with Room database, Firebase Auth,
 * and securely encrypts backup archives using AES-256-GCM keyed to the professor's UID.
 *
 * Implements clean architecture by decoupling database I/O from Android Context/Uri.
 * Uses atomic transactions to ensure data consistency during restore.
 */
@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val auth: FirebaseAuth,
    private val secureAuthManager: SecureAuthManager
) : BackupRepository {

    companion object {
        private const val TAG = "BackupRepository"
        private const val DEFAULT_LOCAL_USER_ID = "local_user"
    }

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private fun getEffectiveProfessorId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: secureAuthManager.getUserId()?.takeIf { it.isNotBlank() }
            ?: DEFAULT_LOCAL_USER_ID
    }

    override suspend fun createBackup(): String = withContext(Dispatchers.IO) {
        val professorId = getEffectiveProfessorId()
        SafeLogger.d(TAG, "Creating encrypted backup for professor: $professorId")

        val backup = AppBackup(
            version = 1,
            students = database.studentDao().getAllStudentsOnce(professorId),
            schedules = database.scheduleDao().getAllSchedulesOnce(professorId),
            exceptions = database.scheduleExceptionDao().getAllExceptionsOnce(professorId),
            resources = database.resourceDao().getAllResourcesOnce(professorId)
        )

        val plainJson = json.encodeToString(backup)
        val encryptedBackup = BackupCryptoHelper.encrypt(plainJson, professorId)
        json.encodeToString(encryptedBackup)
    }

    override suspend fun restoreBackup(jsonContent: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentProfessorId = getEffectiveProfessorId()
            SafeLogger.d(TAG, "Restoring backup for professor: $currentProfessorId")

            // 1. Try to decode as EncryptedBackupFile (v2+)
            val backup: AppBackup = try {
                val encryptedFile = json.decodeFromString<EncryptedBackupFile>(jsonContent)
                val decryptedJson = BackupCryptoHelper.decrypt(encryptedFile, currentProfessorId)
                json.decodeFromString<AppBackup>(decryptedJson)
            } catch (e: SecurityException) {
                SafeLogger.e(TAG, "Backup decryption failed: unauthorized user or corrupted file", e)
                return@withContext Result.failure(
                    SecurityException("La copia de seguridad no pertenece a este usuario o está dañada.", e)
                )
            } catch (e: Exception) {
                // 2. Fallback: try decoding as legacy unencrypted AppBackup (v1)
                try {
                    SafeLogger.d(TAG, "Attempting legacy v1 unencrypted backup restore")
                    json.decodeFromString<AppBackup>(jsonContent)
                } catch (_: Exception) {
                    SafeLogger.e(TAG, "Failed to parse backup content as v1 or v2 format", e)
                    throw IllegalArgumentException("El formato del archivo de copia de seguridad no es válido o está dañado.", e)
                }
            }

            // Perform atomic transaction
            database.withTransaction {
                // 1. Students (Parent)
                backup.students.forEach {
                    database.studentDao().insertStudent(it.copy(professorId = currentProfessorId))
                }
                // 2. Schedules (Child)
                backup.schedules.forEach {
                    database.scheduleDao().insertSchedule(it.copy(professorId = currentProfessorId))
                }
                // 3. Exceptions
                backup.exceptions.forEach {
                    database.scheduleExceptionDao().insertException(it.copy(professorId = currentProfessorId))
                }
                // 4. Resources
                backup.resources.forEach {
                    database.resourceDao().insertResource(it.copy(professorId = currentProfessorId))
                }
            }

            SafeLogger.d(TAG, "Backup restored successfully: ${backup.students.size} students restored.")
            Result.success(Unit)
        } catch (e: Exception) {
            SafeLogger.e(TAG, "Error during restoreBackup: ${e.message}", e)
            Result.failure(e)
        }
    }
}
