package com.devsusana.hometutorpro.data.repository

import com.devsusana.hometutorpro.data.local.dao.StudentDao
import com.devsusana.hometutorpro.data.local.entities.StudentEntity
import com.devsusana.hometutorpro.data.local.entities.SyncStatus
import com.devsusana.hometutorpro.data.mappers.toDomain
import com.devsusana.hometutorpro.data.mappers.toEntity
import com.devsusana.hometutorpro.data.local.dao.ScheduleDao
import com.devsusana.hometutorpro.data.local.dao.ScheduleExceptionDao
import com.devsusana.hometutorpro.data.local.dao.ResourceDao
import com.devsusana.hometutorpro.data.local.dao.SharedResourceDao
import com.devsusana.hometutorpro.data.sync.SyncScheduler
import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.PaymentType
import com.devsusana.hometutorpro.domain.entities.Student
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

/**
 * Unit tests for StudentRepositoryImpl
 * Tests student CRUD operations and data synchronization
 */
class StudentRepositoryImplTest {

    private lateinit var repository: StudentRepositoryImpl
    private lateinit var studentDao: StudentDao
    private lateinit var scheduleDao: ScheduleDao
    private lateinit var scheduleExceptionDao: ScheduleExceptionDao
    private lateinit var resourceDao: ResourceDao
    private lateinit var sharedResourceDao: SharedResourceDao
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var syncScheduler: SyncScheduler

    private val testStudentEntity = StudentEntity(
        id = 1L,
        professorId = "prof123",
        cloudId = "student1",
        name = "Test Student",
        age = 20,
        course = "Computer Science",
        subjects = "Math, Physics",
        parentPhones = "123456789",
        address = "Test Address",
        pricePerHour = 25.0,
        educationalAttention = "None",
        pendingBalance = 0.0,
        lastPaymentDate = null,
        studentPhone = "",
        studentEmail = null,
        color = -123456, // Random int color
        syncStatus = SyncStatus.SYNCED,
        lastModifiedTimestamp = System.currentTimeMillis(),
        pendingDelete = false
    )

    private val testStudentSummaryEntity = com.devsusana.hometutorpro.data.local.entities.StudentSummaryEntity(
        id = 1L,
        name = "Test Student",
        subjects = "Math, Physics",
        color = -123456,
        pendingBalance = 0.0,
        pricePerHour = 25.0,
        isActive = true,
        lastClassDate = null
    )

    @Before
    fun setup() {
        studentDao = mockk(relaxed = true)
        scheduleDao = mockk(relaxed = true)
        scheduleExceptionDao = mockk(relaxed = true)
        resourceDao = mockk(relaxed = true)
        sharedResourceDao = mockk(relaxed = true)
        firestore = mockk(relaxed = true)
        auth = mockk(relaxed = true)
        syncScheduler = mockk(relaxed = true)

        repository = StudentRepositoryImpl(
            studentDao = studentDao,
            scheduleDao = scheduleDao,
            scheduleExceptionDao = scheduleExceptionDao,
            resourceDao = resourceDao,
            sharedResourceDao = sharedResourceDao,
            firestore = firestore,
            auth = auth,
            syncScheduler = syncScheduler
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ============================================================================
    // Get Students Tests
    // ============================================================================

    @Test
    fun `getStudents returns flow of students`() = runTest {
        // Given
        val entities = listOf(testStudentSummaryEntity)
        every { studentDao.getStudentSummaries("prof123") } returns flowOf(entities)

        // When
        val result = repository.getStudents("prof123").first()

        // Then
        assertEquals(1, result.size)
        assertEquals(testStudentSummaryEntity.name, result[0].name)
        verify { studentDao.getStudentSummaries("prof123") }
    }

    @Test
    fun `getStudents returns empty list when no students`() = runTest {
        // Given
        every { studentDao.getStudentSummaries("prof123") } returns flowOf(emptyList())

        // When
        val result = repository.getStudents("prof123").first()

        // Then
        assertTrue(result.isEmpty())
    }

    // ============================================================================
    // Get Student By ID Tests
    // ============================================================================

    @Test
    fun `getStudentById returns student when found`() = runTest {
        // Given
        every { studentDao.getStudentById(1L, "prof123") } returns flowOf(testStudentEntity)

        // When
        val result = repository.getStudentById("prof123", "1").first()

        // Then
        assertTrue(result != null)
        assertEquals(testStudentEntity.name, result?.name)
    }

    @Test
    fun `getStudentById returns null when not found`() = runTest {
        // Given
        every { studentDao.getStudentById(999L, "prof123") } returns flowOf(null)

        // When
        val result = repository.getStudentById("prof123", "999").first()

        // Then
        assertTrue(result == null)
    }

    // ============================================================================
    // Save Student Tests
    // ============================================================================

    @Test
    fun `saveStudent inserts student successfully`() = runTest {
        // Given
        val newStudent = testStudentEntity.copy(id = 0L).toDomain().copy(id = "")
        coEvery { studentDao.insertStudent(any()) } returns 1L
        coEvery { syncScheduler.scheduleSyncNow() } just Runs

        // When
        val result = repository.saveStudent("prof123", newStudent)

        // Then
        assertTrue(result is Result.Success)
        coVerify { studentDao.insertStudent(any()) }
        coVerify { syncScheduler.scheduleSyncNow() }
    }

    @Test
    fun `saveStudent returns error on exception`() = runTest {
        // Given
        val newStudent = testStudentEntity.copy(id = 0L).toDomain().copy(id = "")
        coEvery { studentDao.insertStudent(any()) } throws Exception("Database error")

        // When
        val result = repository.saveStudent("prof123", newStudent)

        // Then
        assertTrue(result is Result.Error)
        assertEquals(DomainError.Unknown, (result as Result.Error).error)
    }

    // ============================================================================
    // Delete Student Tests
    // ============================================================================

    @Test
    fun `deleteStudent marks student for deletion`() = runTest {
        // Given
        val studentId = "1"
        coEvery { studentDao.markForDeletion(1L, "prof123", any(), any()) } just Runs
        coEvery { syncScheduler.scheduleSyncNow() } just Runs

        // When
        val result = repository.deleteStudent("prof123", studentId)

        // Then
        assertTrue(result is Result.Success)
        coVerify { scheduleDao.markSchedulesForDeletionByStudentId(1L, "prof123", any(), any()) }
        coVerify { scheduleExceptionDao.markExceptionsForDeletionByStudentId(1L, "prof123", any(), any()) }
        coVerify { sharedResourceDao.markSharedResourcesForDeletionByStudent(1L, "prof123", any(), any()) }
        coVerify { studentDao.markForDeletion(1L, "prof123", any(), any()) }
        coVerify { syncScheduler.scheduleSyncNow() }
    }

    @Test
    fun `deleteStudent returns error on exception`() = runTest {
        // Given
        val studentId = "1"
        coEvery { studentDao.markForDeletion(any(), any(), any(), any()) } throws Exception("Database error")

        // When
        val result = repository.deleteStudent("prof123", studentId)

        // Then
        assertTrue(result is Result.Error)
        assertEquals(DomainError.Unknown, (result as Result.Error).error)
    }

    // ============================================================================
    // Register Payment Tests
    // ============================================================================

    @Test
    fun `registerPayment updates balance successfully`() = runTest {
        // Given
        val studentId = "1"
        val amountPaid = 25.0
        
        every { studentDao.getStudentById(1L, "prof123") } returns flowOf(testStudentEntity)
        coEvery { 
            studentDao.subtractFromBalance(any(), any(), any(), any(), any(), any()) 
        } just Runs
        coEvery { syncScheduler.scheduleSyncNow() } just Runs

        // When
        val result = repository.registerPayment("prof123", studentId, amountPaid, PaymentType.EFFECTIVE)

        // Then
        assertTrue(result is Result.Success)
        coVerify { studentDao.subtractFromBalance(1L, "prof123", amountPaid, any(), com.devsusana.hometutorpro.data.local.entities.SyncStatus.PENDING_UPLOAD, any()) }
        coVerify { syncScheduler.scheduleSyncNow() }
    }

    @Test
    fun `registerPayment returns error when student not found`() = runTest {
        // Given
        val studentId = "999"
        
        every { studentDao.getStudentById(999L, "prof123") } returns flowOf(null)

        // When
        val result = repository.registerPayment("prof123", studentId, 25.0, PaymentType.EFFECTIVE)

        // Then
        assertTrue(result is Result.Error)
        assertEquals(DomainError.StudentNotFound, (result as Result.Error).error)
    }

    @Test
    fun `registerPayment returns error on invalid student ID`() = runTest {
        // Given
        val studentId = "invalid"
        
        // When
        val result = repository.registerPayment("prof123", studentId, 25.0, PaymentType.EFFECTIVE)

        // Then
        assertTrue(result is Result.Error)
        assertEquals(DomainError.StudentNotFound, (result as Result.Error).error)
    }
}
