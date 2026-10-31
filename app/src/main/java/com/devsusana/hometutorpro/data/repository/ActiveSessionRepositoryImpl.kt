package com.devsusana.hometutorpro.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.devsusana.hometutorpro.core.utils.SafeLogger
import com.devsusana.hometutorpro.data.local.SupportFactoryHelper
import com.devsusana.hometutorpro.domain.entities.ActiveSession
import com.devsusana.hometutorpro.domain.repository.IActiveSessionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActiveSessionRepositoryImpl internal constructor(
    @param:ApplicationContext private val context: Context,
    private val ioDispatcher: CoroutineDispatcher
) : IActiveSessionRepository {

    @Inject
    constructor(@ApplicationContext context: Context) : this(context, Dispatchers.IO)

    private val prefs: SharedPreferences by lazy {
        createEncryptedPreferences()
    }

    private fun createEncryptedPreferences(): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            SafeLogger.e(TAG, "Failed to initialize EncryptedSharedPreferences: ${e.message}. Recreating...", e)
            try {
                SupportFactoryHelper.deletePreferencesFile(context, PREFS_NAME)
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (fallbackEx: Exception) {
                SafeLogger.e(TAG, "EncryptedSharedPreferences fallback failed: ${fallbackEx.message}. Using standard SharedPreferences", fallbackEx)
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            }
        }
    }

    override suspend fun getActiveSession(): ActiveSession? = withContext(ioDispatcher) {
        val isOngoing = prefs.getBoolean(KEY_IS_ONGOING, false)
        if (!isOngoing) return@withContext null

        val studentId = prefs.getString(KEY_STUDENT_ID, null) ?: return@withContext null
        val studentName = prefs.getString(KEY_STUDENT_NAME, "") ?: ""
        val startTimeMillis = prefs.getLong(KEY_START_TIME_MILLIS, 0)
        val durationMinutes = prefs.getLong(KEY_DURATION_MINUTES, 0)
        val tutorId = prefs.getString(KEY_TUTOR_ID, "") ?: ""
        val isBilled = prefs.getBoolean(KEY_IS_BILLED, false)

        // Check if the session has expired past its scheduled duration.
        // If already billed, clear as soon as the scheduled end time has arrived.
        // If unbilled, do not auto-delete on read so alarms (ClassEndReceiver) and
        // consolidation mechanisms can process and bill the session without losing state.
        if (startTimeMillis > 0 && durationMinutes > 0) {
            val endTimeMillis = startTimeMillis + (durationMinutes * 60 * 1000L)
            if (isBilled && System.currentTimeMillis() >= endTimeMillis) {
                clearSessionSync()
                return@withContext null
            }
        }

        ActiveSession(
            studentId = studentId,
            studentName = studentName,
            startTimeMillis = startTimeMillis,
            durationMinutes = durationMinutes,
            isOngoing = true,
            tutorId = tutorId,
            isBilled = isBilled
        )
    }

    override suspend fun startSession(session: ActiveSession): Unit = withContext(ioDispatcher) {
        prefs.edit()
            .putBoolean(KEY_IS_ONGOING, true)
            .putString(KEY_STUDENT_ID, session.studentId)
            .putString(KEY_STUDENT_NAME, session.studentName)
            .putLong(KEY_START_TIME_MILLIS, session.startTimeMillis)
            .putLong(KEY_DURATION_MINUTES, session.durationMinutes)
            .putString(KEY_TUTOR_ID, session.tutorId)
            .putBoolean(KEY_IS_BILLED, session.isBilled)
            .apply()
    }

    override suspend fun stopSession(): Unit = withContext(ioDispatcher) {
        clearSessionSync()
    }

    private fun clearSessionSync() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val TAG = "ActiveSessionRepo"
        private const val PREFS_NAME = "active_session_prefs"
        private const val KEY_IS_ONGOING = "is_ongoing"
        private const val KEY_STUDENT_ID = "student_id"
        private const val KEY_STUDENT_NAME = "student_name"
        private const val KEY_START_TIME_MILLIS = "start_time_millis"
        private const val KEY_DURATION_MINUTES = "duration_minutes"
        private const val KEY_TUTOR_ID = "tutor_id"
        private const val KEY_IS_BILLED = "is_billed"
    }
}
