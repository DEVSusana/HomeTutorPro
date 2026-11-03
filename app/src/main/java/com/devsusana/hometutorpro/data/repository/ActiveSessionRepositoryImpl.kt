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
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    prefsOverride: SharedPreferences? = null
) : IActiveSessionRepository {

    @Inject
    constructor(@ApplicationContext context: Context) : this(context, Dispatchers.IO, null)

    private val prefs: SharedPreferences by lazy {
        prefsOverride ?: createEncryptedPreferences()
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
                SafeLogger.e(TAG, "EncryptedSharedPreferences fallback failed: ${fallbackEx.message}. Falling back to in-memory storage to prevent plaintext persistence.", fallbackEx)
                InMemorySharedPreferences()
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
        // If already billed, consider it expired and return null without mutating state (CQS).
        // If unbilled, do not consider it expired so alarms (ClassEndReceiver) and
        // consolidation mechanisms can process and bill the session without losing state.
        if (startTimeMillis > 0 && durationMinutes > 0) {
            val endTimeMillis = startTimeMillis + (durationMinutes * 60 * 1000L)
            if (isBilled && System.currentTimeMillis() >= endTimeMillis) {
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

    /**
     * In-memory fallback for [SharedPreferences] used when [EncryptedSharedPreferences]
     * fails completely (e.g. MasterKey corruption or keystore failure).
     * Guarantees that sensitive data (studentId, tutorId) is never written to disk in plaintext.
     */
    internal class InMemorySharedPreferences : SharedPreferences {
        private val data = java.util.concurrent.ConcurrentHashMap<String, Any>()
        private val listeners = java.util.concurrent.CopyOnWriteArraySet<SharedPreferences.OnSharedPreferenceChangeListener>()

        override fun getAll(): Map<String, *> = HashMap(data)

        override fun getString(key: String?, defValue: String?): String? =
            (data[key] as? String) ?: defValue

        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(key: String?, defValues: Set<String>?): Set<String>? =
            (data[key] as? Set<String>) ?: defValues

        override fun getInt(key: String?, defValue: Int): Int =
            (data[key] as? Int) ?: defValue

        override fun getLong(key: String?, defValue: Long): Long =
            (data[key] as? Long) ?: defValue

        override fun getFloat(key: String?, defValue: Float): Float =
            (data[key] as? Float) ?: defValue

        override fun getBoolean(key: String?, defValue: Boolean): Boolean =
            (data[key] as? Boolean) ?: defValue

        override fun contains(key: String?): Boolean = key != null && data.containsKey(key)

        override fun edit(): SharedPreferences.Editor = EditorImpl()

        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
            if (listener != null) listeners.add(listener)
        }

        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
            if (listener != null) listeners.remove(listener)
        }

        private inner class EditorImpl : SharedPreferences.Editor {
            private val pendingChanges = HashMap<String, Any?>()
            private var clearPending = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply {
                if (key != null) pendingChanges[key] = value
            }

            override fun putStringSet(key: String?, values: Set<String>?): SharedPreferences.Editor = apply {
                if (key != null) pendingChanges[key] = values
            }

            override fun putInt(key: String?, value: Int): SharedPreferences.Editor = apply {
                if (key != null) pendingChanges[key] = value
            }

            override fun putLong(key: String?, value: Long): SharedPreferences.Editor = apply {
                if (key != null) pendingChanges[key] = value
            }

            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = apply {
                if (key != null) pendingChanges[key] = value
            }

            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = apply {
                if (key != null) pendingChanges[key] = value
            }

            override fun remove(key: String?): SharedPreferences.Editor = apply {
                if (key != null) pendingChanges[key] = this
            }

            override fun clear(): SharedPreferences.Editor = apply {
                clearPending = true
            }

            override fun commit(): Boolean {
                apply()
                return true
            }

            override fun apply() {
                synchronized(this@InMemorySharedPreferences) {
                    if (clearPending) {
                        data.clear()
                    }
                    for ((k, v) in pendingChanges) {
                        if (v === this) {
                            data.remove(k)
                        } else if (v != null) {
                            data[k] = v
                        } else {
                            data.remove(k)
                        }
                    }
                }
            }
        }
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
