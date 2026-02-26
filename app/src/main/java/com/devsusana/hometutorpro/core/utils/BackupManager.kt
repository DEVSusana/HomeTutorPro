package com.devsusana.hometutorpro.core.utils

import android.content.Context
import android.net.Uri
import com.devsusana.hometutorpro.data.local.AppDatabase
import com.devsusana.hometutorpro.data.models.AppBackup
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    private val database: AppDatabase,
    private val auth: FirebaseAuth,
    private val uriReader: IUriReader
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun createBackup(): String = withContext(Dispatchers.IO) {
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

    suspend fun restoreBackup(context: Context, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val content = uriReader.readTextFromUri(context, uri)
            val backup = json.decodeFromString<AppBackup>(content)
            
            // Simplified restoration (Room handles transactions better with suspend)
            restoreData(backup)
            
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private suspend fun restoreData(backup: AppBackup) {
        // We use a transaction-like approach by using the DAOs
        withContext(Dispatchers.IO) {
            // 1. Students (Parent)
            backup.students.forEach { database.studentDao().insertStudent(it) }
            // 2. Schedules (Child)
            backup.schedules.forEach { database.scheduleDao().insertSchedule(it) }
            // 3. Exceptions
            backup.exceptions.forEach { database.scheduleExceptionDao().insertException(it) }
            // 4. Resources
            backup.resources.forEach { database.resourceDao().insertResource(it) }
        }
    }
}
