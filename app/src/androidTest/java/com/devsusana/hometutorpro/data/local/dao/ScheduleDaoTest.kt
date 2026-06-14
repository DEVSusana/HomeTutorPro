package com.devsusana.hometutorpro.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.devsusana.hometutorpro.data.local.AppDatabase
import com.devsusana.hometutorpro.data.local.entities.ScheduleEntity
import com.devsusana.hometutorpro.data.local.entities.StudentEntity
import com.devsusana.hometutorpro.data.local.entities.SyncStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek

/**
 * Instrumented integration tests for [ScheduleDao].
 */
@RunWith(AndroidJUnit4::class)
class ScheduleDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var studentDao: StudentDao
    private lateinit var scheduleDao: ScheduleDao

    private val professorId = "test_prof_id"
    private var studentId: Long = 0L

    /**
     * Initializes an in-memory Room database and seeds a test student to satisfy foreign keys.
     */
    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        studentDao = database.studentDao()
        scheduleDao = database.scheduleDao()

        // Seed student first (schedules table has foreign key constraint on studentId)
        val student = StudentEntity(
            id = 1L,
            professorId = professorId,
            name = "John Doe",
            age = 20,
            address = "123 Street",
            parentPhones = "555-1234",
            studentPhone = "555-5678",
            studentEmail = "john.doe@example.com",
            subjects = "Math",
            course = "Algebra",
            pricePerHour = 45.0,
            educationalAttention = "Regular",
            lastPaymentDate = null,
            syncStatus = SyncStatus.SYNCED
        )
        studentId = studentDao.insertStudent(student)
    }

    /**
     * Closes the database helper after each test.
     */
    @After
    fun tearDown() {
        database.close()
    }

    /**
     * Tests basic CRUD operations: Insert, Read, Update, and Delete.
     */
    @Test
    fun testBasicCRUD() = runBlocking {
        val schedule = ScheduleEntity(
            id = 1L,
            professorId = professorId,
            studentId = studentId,
            dayOfWeek = DayOfWeek.MONDAY,
            startTime = "10:00",
            endTime = "11:00",
            syncStatus = SyncStatus.SYNCED
        )

        // 1. Insert
        val insertedId = scheduleDao.insertSchedule(schedule)
        assertEquals(1L, insertedId)

        // 2. Read (getById)
        val retrieved = scheduleDao.getScheduleById(insertedId, professorId)
        assertNotNull(retrieved)
        assertEquals("10:00", retrieved?.startTime)

        // 3. Update
        val updated = retrieved!!.copy(startTime = "10:30")
        scheduleDao.updateSchedule(updated)
        val afterUpdate = scheduleDao.getScheduleById(insertedId, professorId)
        assertEquals("10:30", afterUpdate?.startTime)

        // 4. Delete
        scheduleDao.deleteSchedule(updated)
        val afterDelete = scheduleDao.getScheduleById(insertedId, professorId)
        assertNull(afterDelete)
    }

    /**
     * Tests business logic conflict verification with various time overlap edge cases.
     */
    @Test
    fun testConflictVerification() = runBlocking {
        val schedule = ScheduleEntity(
            id = 1L,
            professorId = professorId,
            studentId = studentId,
            dayOfWeek = DayOfWeek.MONDAY,
            startTime = "10:00",
            endTime = "12:00",
            syncStatus = SyncStatus.SYNCED
        )
        scheduleDao.insertSchedule(schedule)

        // Overlap cases:
        // Case A: Fully inside (10:30 to 11:30) - Expected: conflict
        assertTrue(scheduleDao.hasConflict(DayOfWeek.MONDAY.value, "10:30", "11:30", professorId))

        // Case B: Starts before, ends inside (09:30 to 10:30) - Expected: conflict
        assertTrue(scheduleDao.hasConflict(DayOfWeek.MONDAY.value, "09:30", "10:30", professorId))

        // Case C: Starts inside, ends after (11:30 to 12:30) - Expected: conflict
        assertTrue(scheduleDao.hasConflict(DayOfWeek.MONDAY.value, "11:30", "12:30", professorId))

        // Case D: Exact match (10:00 to 12:00) - Expected: conflict
        assertTrue(scheduleDao.hasConflict(DayOfWeek.MONDAY.value, "10:00", "12:00", professorId))

        // Case E: Touching boundary - Starts exactly at end (12:00 to 13:00) - Expected: no conflict
        assertFalse(scheduleDao.hasConflict(DayOfWeek.MONDAY.value, "12:00", "13:00", professorId))

        // Case F: Touching boundary - Ends exactly at start (09:00 to 10:00) - Expected: no conflict
        assertFalse(scheduleDao.hasConflict(DayOfWeek.MONDAY.value, "09:00", "10:00", professorId))

        // Case G: Different day of week - Expected: no conflict
        assertFalse(scheduleDao.hasConflict(DayOfWeek.TUESDAY.value, "10:30", "11:30", professorId))

        // Case H: Conflicting schedule details
        val conflict = scheduleDao.getConflictingSchedule(DayOfWeek.MONDAY.value, "10:30", "11:30", professorId)
        assertNotNull(conflict)
        assertEquals(1L, conflict?.id)
    }

    /**
     * Tests join query mapping between ScheduleEntity and Student.
     */
    @Test
    fun testJoinQueryMapping() = runBlocking {
        val schedule = ScheduleEntity(
            id = 1L,
            professorId = professorId,
            studentId = studentId,
            dayOfWeek = DayOfWeek.MONDAY,
            startTime = "10:00",
            endTime = "11:00",
            syncStatus = SyncStatus.SYNCED
        )
        scheduleDao.insertSchedule(schedule)

        val schedulesWithStudentList = scheduleDao.getAllSchedulesWithStudent(professorId).first()
        assertEquals(1, schedulesWithStudentList.size)

        val item = schedulesWithStudentList.first()
        assertEquals("John Doe", item.studentName)
        assertEquals("Math", item.studentSubjects)
    }

    /**
     * Tests sync status transitions: updateSyncStatus and markForDeletion.
     */
    @Test
    fun testSyncStatusTransitions() = runBlocking {
        val schedule = ScheduleEntity(
            id = 1L,
            professorId = professorId,
            studentId = studentId,
            dayOfWeek = DayOfWeek.MONDAY,
            startTime = "10:00",
            endTime = "11:00",
            syncStatus = SyncStatus.SYNCED
        )
        scheduleDao.insertSchedule(schedule)

        // Update sync status
        scheduleDao.updateSyncStatus(1L, professorId, SyncStatus.PENDING_UPLOAD)
        var retrieved = scheduleDao.getScheduleById(1L, professorId)
        assertEquals(SyncStatus.PENDING_UPLOAD, retrieved?.syncStatus)

        // Mark for deletion (soft delete)
        scheduleDao.markForDeletion(1L, professorId, SyncStatus.PENDING_DELETE)
        retrieved = scheduleDao.getScheduleById(1L, professorId)
        // Since getScheduleById filters out pendingDelete = 0, retrieved should be null!
        assertNull(retrieved)

        // Check raw record status using getAllSchedulesOnce which doesn't filter pendingDelete
        val rawList = scheduleDao.getAllSchedulesOnce(professorId)
        assertEquals(1, rawList.size)
        assertTrue(rawList.first().pendingDelete)
        assertEquals(SyncStatus.PENDING_DELETE, rawList.first().syncStatus)
    }
}
