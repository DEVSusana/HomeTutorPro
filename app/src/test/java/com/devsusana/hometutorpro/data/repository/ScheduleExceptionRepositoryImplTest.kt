package com.devsusana.hometutorpro.data.repository

import com.devsusana.hometutorpro.data.local.dao.ScheduleExceptionDao
import com.devsusana.hometutorpro.data.local.entities.ScheduleExceptionEntity
import com.devsusana.hometutorpro.data.local.entities.SyncStatus
import com.devsusana.hometutorpro.data.mappers.toDomain
import com.devsusana.hometutorpro.data.mappers.toEntity
import com.devsusana.hometutorpro.data.sync.SyncScheduler
import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.ExceptionType
import com.devsusana.hometutorpro.domain.entities.ScheduleException
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

/**
 * Unit tests for ScheduleExceptionRepositoryImpl
 * Tests schedule exception management (cancellations and rescheduling)
 */
class ScheduleExceptionRepositoryImplTest {

    private lateinit var repository: ScheduleExceptionRepositoryImpl
    private lateinit var exceptionDao: ScheduleExceptionDao
    private lateinit var syncScheduler: SyncScheduler

    private val testUserId = "user123"
    private val testStudentId = "1"
    private val testException = ScheduleException(
        id = "1",
        studentId = testStudentId,
        date = 1234567890L,
        type = ExceptionType.RESCHEDULED,
        newDayOfWeek = DayOfWeek.TUESDAY,
        newStartTime = "10:00",
        newEndTime = "11:00",
        reason = "Student request",
        originalScheduleId = "schedule1"
    )

    private val testExceptionEntity = ScheduleExceptionEntity(
        id = 1L,
        professorId = testUserId,
        studentId = 1L,
        cloudId = "exception1",
        originalScheduleId = "schedule1",
        exceptionDate = 1234567890L,
        reason = "Student request",
        type = "RESCHEDULED",
        newDayOfWeek = DayOfWeek.TUESDAY,
        newStartTime = "10:00",
        newEndTime = "11:00",
        syncStatus = SyncStatus.SYNCED,
        lastModifiedTimestamp = System.currentTimeMillis(),
        pendingDelete = false
    )

    @Before
    fun setup() {
        exceptionDao = mockk(relaxed = true)
        syncScheduler = mockk(relaxed = true)

        repository = ScheduleExceptionRepositoryImpl(
            exceptionDao = exceptionDao,
            syncScheduler = syncScheduler
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ============================================================================
    // Get Exceptions Tests
    // ============================================================================

    @Test
    fun `getExceptions returns flow of exceptions`() = runTest {
        // Given
        val entities = listOf(testExceptionEntity)
        every { exceptionDao.getExceptionsByStudentId(1L, testUserId) } returns flowOf(entities)

        // When
        val result = repository.getExceptions(testUserId, testStudentId).first()

        // Then
        assertEquals(1, result.size)
        assertEquals(testException.id, result[0].id)
        assertEquals(testException.type, result[0].type)
        verify { exceptionDao.getExceptionsByStudentId(1L, testUserId) }
    }

    @Test
    fun `getExceptions returns empty list when no exceptions`() = runTest {
        // Given
        every { exceptionDao.getExceptionsByStudentId(1L, testUserId) } returns flowOf(emptyList())

        // When
        val result = repository.getExceptions(testUserId, testStudentId).first()

        // Then
        assertTrue(result.isEmpty())
    }

    // ============================================================================
    // Save Exception Tests
    // ============================================================================

    @Test
    fun `saveException inserts exception successfully`() = runTest {
        // Given
        coEvery { exceptionDao.insertException(any()) } returns 1L
        coEvery { syncScheduler.scheduleSyncNow() } just Runs

        // When
        val result = repository.saveException(testUserId, testStudentId, testException)

        // Then
        assertTrue(result is Result.Success)
        coVerify { exceptionDao.insertException(any()) }
        coVerify { syncScheduler.scheduleSyncNow() }
    }

    @Test
    fun `saveException returns error on database exception`() = runTest {
        // Given
        coEvery { exceptionDao.insertException(any()) } throws Exception("Database error")

        // When
        val result = repository.saveException(testUserId, testStudentId, testException)

        // Then
        assertTrue(result is Result.Error)
        assertEquals(DomainError.Unknown, (result as Result.Error).error)
    }

    // ============================================================================
    // Delete Exception Tests
    // ============================================================================

    @Test
    fun `deleteException removes exception successfully`() = runTest {
        // Given
        val exceptionId = "1"
        coEvery { exceptionDao.markForDeletion(1L, testUserId, any(), any()) } just Runs
        coEvery { syncScheduler.scheduleSyncNow() } just Runs

        // When
        val result = repository.deleteException(testUserId, testStudentId, exceptionId)

        // Then
        assertTrue(result is Result.Success)
        coVerify { exceptionDao.markForDeletion(1L, testUserId, any(), any()) }
        coVerify { syncScheduler.scheduleSyncNow() }
    }

    @Test
    fun `deleteException returns error on database exception`() = runTest {
        // Given
        val exceptionId = "1"
        coEvery { exceptionDao.markForDeletion(any(), any(), any(), any()) } throws Exception("Database error")

        // When
        val result = repository.deleteException(testUserId, testStudentId, exceptionId)

        // Then
        assertTrue(result is Result.Error)
        assertEquals(DomainError.Unknown, (result as Result.Error).error)
    }

    // ============================================================================
    // Exception Type Tests
    // ============================================================================

    @Test
    fun `saveException handles CANCELLED type correctly`() = runTest {
        // Given
        val cancelException = testException.copy(
            type = ExceptionType.CANCELLED,
            newDayOfWeek = null,
            newStartTime = "",
            newEndTime = ""
        )
        coEvery { exceptionDao.insertException(any()) } returns 1L
        coEvery { syncScheduler.scheduleSyncNow() } just Runs

        // When
        val result = repository.saveException(testUserId, testStudentId, cancelException)

        // Then
        assertTrue(result is Result.Success)
        coVerify { 
            exceptionDao.insertException(match { 
                it.type == "CANCELLED" && 
                it.newDayOfWeek == null &&
                it.newStartTime == null &&
                it.newEndTime == null
            }) 
        }
    }

    @Test
    fun `saveException handles RESCHEDULED type correctly`() = runTest {
        // Given
        coEvery { exceptionDao.insertException(any()) } returns 1L
        coEvery { syncScheduler.scheduleSyncNow() } just Runs

        // When
        val result = repository.saveException(testUserId, testStudentId, testException)

        // Then
        assertTrue(result is Result.Success)
        coVerify { 
            exceptionDao.insertException(match { 
                it.type == "RESCHEDULED" && 
                it.newDayOfWeek == DayOfWeek.TUESDAY &&
                it.newStartTime == "10:00" &&
                it.newEndTime == "11:00"
            }) 
        }
    }
}
