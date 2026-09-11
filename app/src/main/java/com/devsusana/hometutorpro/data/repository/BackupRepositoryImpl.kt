package com.devsusana.hometutorpro.data.repository

import androidx.room.withTransaction
import com.devsusana.hometutorpro.data.local.AppDatabase
import com.devsusana.hometutorpro.data.models.AppBackup
import com.devsusana.hometutorpro.domain.repository.BackupRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [BackupRepository] that interacts with Room database and Firebase Auth.
 *
 * Implements clean architecture by decoupling database I/O from Android Context/Uri.
 * Uses atomic transactions to ensure data consistency during restore.
 */
@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val auth: FirebaseAuth
) : BackupRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun createBackup(): String = withContext(Dispatchers.IO) {
        val professorId = auth.currentUser?.uid ?: ""
        val backup = AppBackup(
            version = 1,
            students = database.studentDao().getAllStudentsOnce(professorId),
            schedules = database.scheduleDao().getAllSchedulesOnce(professorId),
            exceptions = database.scheduleExceptionDao().getAllExceptionsOnce(professorId),
            resources = database.resourceDao().getAllResourcesOnce(professorId)
        )
        json.encodeToString(backup)
    }

    override suspend fun restoreBackup(jsonContent: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val backup = json.decodeFromString<AppBackup>(jsonContent)
            val currentProfessorId = auth.currentUser?.uid ?: ""

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

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
