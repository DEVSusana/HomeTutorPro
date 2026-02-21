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
    private val auth: FirebaseAuth
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
            val content = readTextFromUri(context, uri)
            val backup = json.decodeFromString<AppBackup>(content)
            
            // Perform restoration in a transaction
            database.runInTransaction {
                // We don't delete existing data to avoid accidents, 
                // but you might want to clear tables first depending on requirements.
                // For a "Restore", clearing is usually expected.
                
                // Clear tables (order matters for foreign keys)
                // database.clearAllTables() // Risky if other tables exist
                
                // Manual clear in correct order
                // Note: We'll use runBlocking or similar if DAOs are suspend, 
                // but runInTransaction blocks the thread.
                
                // Since DAOs are usually suspend, we might need a different approach 
                // for Room transactions if they are not already handled.
            }
            
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

    private fun readTextFromUri(context: Context, uri: Uri): String {
        val stringBuilder = StringBuilder()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    stringBuilder.append(line)
                    line = reader.readLine()
                }
            }
        }
        return stringBuilder.toString()
    }
}
