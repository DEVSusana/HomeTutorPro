package com.devsusana.hometutorpro.data.repository

import android.content.Context
import com.devsusana.hometutorpro.domain.entities.ActiveSession
import com.devsusana.hometutorpro.domain.repository.IActiveSessionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActiveSessionRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : IActiveSessionRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getActiveSession(): ActiveSession? {
        val isOngoing = prefs.getBoolean(KEY_IS_ONGOING, false)
        if (!isOngoing) return null

        val studentId = prefs.getString(KEY_STUDENT_ID, null) ?: return null
        val studentName = prefs.getString(KEY_STUDENT_NAME, "") ?: ""
        val startTimeMillis = prefs.getLong(KEY_START_TIME_MILLIS, 0)
        val durationMinutes = prefs.getLong(KEY_DURATION_MINUTES, 0)
        val tutorId = prefs.getString(KEY_TUTOR_ID, "") ?: ""

        // Validate that session belongs to the currently authenticated tutor
        val currentTutorId = try {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        } catch (_: Exception) {
            null
        }
        if (currentTutorId != null && tutorId.isNotBlank() && tutorId != currentTutorId) {
            return null
        }

        // Check if the session has expired past its scheduled duration
        if (startTimeMillis > 0 && durationMinutes > 0) {
            val endTimeMillis = startTimeMillis + (durationMinutes * 60 * 1000L)
            if (System.currentTimeMillis() >= endTimeMillis) {
                stopSession()
                return null
            }
        }

        return ActiveSession(
            studentId = studentId,
            studentName = studentName,
            startTimeMillis = startTimeMillis,
            durationMinutes = durationMinutes,
            isOngoing = true,
            tutorId = tutorId
        )
    }

    override fun startSession(session: ActiveSession) {
        val effectiveTutorId = session.tutorId.ifBlank {
            try {
                com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
            } catch (_: Exception) {
                ""
            }
        }
        prefs.edit().apply {
            putBoolean(KEY_IS_ONGOING, true)
            putString(KEY_STUDENT_ID, session.studentId)
            putString(KEY_STUDENT_NAME, session.studentName)
            putLong(KEY_START_TIME_MILLIS, session.startTimeMillis)
            putLong(KEY_DURATION_MINUTES, session.durationMinutes)
            putString(KEY_TUTOR_ID, effectiveTutorId)
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
        private const val KEY_TUTOR_ID = "tutor_id"
    }
}
