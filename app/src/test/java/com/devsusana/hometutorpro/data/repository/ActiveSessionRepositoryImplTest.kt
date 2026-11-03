package com.devsusana.hometutorpro.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.devsusana.hometutorpro.domain.entities.ActiveSession
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActiveSessionRepositoryImplTest {

    private lateinit var context: Context
    private lateinit var repository: ActiveSessionRepositoryImpl

    @Before
    fun setup() = runTest {
        context = ApplicationProvider.getApplicationContext()
        repository = ActiveSessionRepositoryImpl(context)
        repository.stopSession()
    }

    @After
    fun tearDown() = runTest {
        repository.stopSession()
    }

    @Test
    fun `getActiveSession returns null initially`() = runTest {
        assertNull(repository.getActiveSession())
    }

    @Test
    fun `startSession saves session and getActiveSession retrieves it`() = runTest {
        val session = ActiveSession(
            studentId = "s123",
            studentName = "Alice",
            startTimeMillis = System.currentTimeMillis(),
            durationMinutes = 60L,
            isOngoing = true,
            tutorId = "tutorA"
        )

        repository.startSession(session)

        val retrieved = repository.getActiveSession()
        assertNotNull(retrieved)
        assertEquals("s123", retrieved?.studentId)
        assertEquals("Alice", retrieved?.studentName)
        assertEquals(60L, retrieved?.durationMinutes)
        assertTrue(retrieved?.isOngoing == true)
        assertEquals("tutorA", retrieved?.tutorId)
    }

    @Test
    fun `stopSession clears active session`() = runTest {
        val session = ActiveSession(
            studentId = "s123",
            studentName = "Alice",
            startTimeMillis = System.currentTimeMillis(),
            durationMinutes = 60L,
            isOngoing = true,
            tutorId = "tutorA"
        )

        repository.startSession(session)
        assertNotNull(repository.getActiveSession())

        repository.stopSession()
        assertNull(repository.getActiveSession())
    }

    @Test
    fun `getActiveSession auto expires and clears session when duration passed and session is billed`() = runTest {
        val twoHoursAgo = System.currentTimeMillis() - (120 * 60 * 1000L)
        val expiredSession = ActiveSession(
            studentId = "s456",
            studentName = "Bob",
            startTimeMillis = twoHoursAgo,
            durationMinutes = 60L, // 60 min duration expired 1 hr ago
            isOngoing = true,
            tutorId = "tutorA",
            isBilled = true
        )

        repository.startSession(expiredSession)

        // Reading expired billed session should auto-clear and return null
        val result = repository.getActiveSession()
        assertNull(result)

        // Subsequent read should remain null
        assertNull(repository.getActiveSession())
    }

    @Test
    fun `getActiveSession preserves unbilled session past scheduled duration to allow consolidation`() = runTest {
        val twoHoursAgo = System.currentTimeMillis() - (120 * 60 * 1000L)
        val unbilledSession = ActiveSession(
            studentId = "s456",
            studentName = "Bob",
            startTimeMillis = twoHoursAgo,
            durationMinutes = 60L,
            isOngoing = true,
            tutorId = "tutorA",
            isBilled = false
        )

        repository.startSession(unbilledSession)

        // Reading unbilled session must not clear it, preserving it for consolidation
        val result = repository.getActiveSession()
        assertNotNull(result)
        assertEquals("s456", result?.studentId)
        assertEquals(false, result?.isBilled)
    }

    @Test
    fun `InMemorySharedPreferences functions properly in fallback mode`() = runTest {
        val inMemoryPrefs = ActiveSessionRepositoryImpl.InMemorySharedPreferences()
        val repo = ActiveSessionRepositoryImpl(
            context = context,
            prefsOverride = inMemoryPrefs
        )

        val session = ActiveSession(
            studentId = "sInMemory",
            studentName = "InMemoryUser",
            startTimeMillis = System.currentTimeMillis(),
            durationMinutes = 45L,
            isOngoing = true,
            tutorId = "tutorMem"
        )

        repo.startSession(session)

        val retrieved = repo.getActiveSession()
        assertNotNull(retrieved)
        assertEquals("sInMemory", retrieved?.studentId)
        assertEquals("InMemoryUser", retrieved?.studentName)

        // Exercise in-memory methods directly for coverage and reliability
        inMemoryPrefs.edit()
            .putInt("int_key", 42)
            .putFloat("float_key", 3.14f)
            .putStringSet("set_key", setOf("a", "b"))
            .apply()

        assertEquals(42, inMemoryPrefs.getInt("int_key", 0))
        assertEquals(3.14f, inMemoryPrefs.getFloat("float_key", 0f), 0.01f)
        assertEquals(setOf("a", "b"), inMemoryPrefs.getStringSet("set_key", emptySet()))
        assertTrue(inMemoryPrefs.contains("int_key"))
        assertTrue(inMemoryPrefs.getAll().containsKey("int_key"))

        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> }
        inMemoryPrefs.registerOnSharedPreferenceChangeListener(listener)
        inMemoryPrefs.unregisterOnSharedPreferenceChangeListener(listener)

        inMemoryPrefs.edit().remove("int_key").commit()
        assertEquals(0, inMemoryPrefs.getInt("int_key", 0))

        repo.stopSession()
        assertNull(repo.getActiveSession())
    }
}
