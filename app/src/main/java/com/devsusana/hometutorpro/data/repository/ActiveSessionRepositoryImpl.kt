package com.devsusana.hometutorpro.data.repository

import android.content.Context
import com.devsusana.hometutorpro.domain.entities.ActiveSession
import com.devsusana.hometutorpro.domain.repository.IActiveSessionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ActiveSessionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : IActiveSessionRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getActiveSession(): ActiveSession? {
        val isOngoing = prefs.getBoolean(KEY_IS_ONGOING, false)
        if (!isOngoing) return null

        val studentId = prefs.getString(KEY_STUDENT_ID, null) ?: return null
        val studentName = prefs.getString(KEY_STUDENT_NAME, "") ?: ""
        val startTimeMillis = prefs.getLong(KEY_START_TIME_MILLIS, 0)
        val durationMinutes = prefs.getLong(KEY_DURATION_MINUTES, 0)

        return ActiveSession(
            studentId = studentId,
            studentName = studentName,
            startTimeMillis = startTimeMillis,
            durationMinutes = durationMinutes,
            isOngoing = true
        )
    }

    override fun startSession(session: ActiveSession) {
        prefs.edit().apply {
            putBoolean(KEY_IS_ONGOING, true)
            putString(KEY_STUDENT_ID, session.studentId)
            putString(KEY_STUDENT_NAME, session.studentName)
            putLong(KEY_START_TIME_MILLIS, session.startTimeMillis)
            putLong(KEY_DURATION_MINUTES, session.durationMinutes)
            apply()
        }
    }

    override fun stopSession() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "active_session_prefs"
        private const val KEY_IS_ONGOING = "is_ongoing"
        private const val KEY_STUDENT_ID = "student_id"
        private const val KEY_STUDENT_NAME = "student_name"
        private const val KEY_START_TIME_MILLIS = "start_time_millis"
        private const val KEY_DURATION_MINUTES = "duration_minutes"
    }
}
