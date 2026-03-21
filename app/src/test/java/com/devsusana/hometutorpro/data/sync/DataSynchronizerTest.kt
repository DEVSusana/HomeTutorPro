package com.devsusana.hometutorpro.data.sync

import com.devsusana.hometutorpro.data.local.dao.ScheduleDao
import com.devsusana.hometutorpro.data.local.dao.ScheduleExceptionDao
import com.devsusana.hometutorpro.data.local.dao.StudentDao
import com.devsusana.hometutorpro.data.local.dao.SyncMetadataDao
import com.devsusana.hometutorpro.data.local.entities.StudentEntity
import com.devsusana.hometutorpro.data.local.entities.SyncStatus
import com.devsusana.hometutorpro.core.auth.SecureAuthManager
import com.devsusana.hometutorpro.domain.repository.RemoteDataSource
import com.devsusana.hometutorpro.domain.repository.RemoteDocument
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class DataSynchronizerTest {

    private val studentDao: StudentDao = mockk(relaxed = true)
    private val scheduleDao: ScheduleDao = mockk(relaxed = true)
    private val scheduleExceptionDao: ScheduleExceptionDao = mockk(relaxed = true)
    private val remoteDataSource: RemoteDataSource = mockk(relaxed = true)
    private val conflictResolver: ConflictResolver = mockk(relaxed = true)
    private val auth: FirebaseAuth = mockk()
    private val syncMetadataDao: SyncMetadataDao = mockk(relaxed = true)
    private val secureAuthManager: SecureAuthManager = mockk(relaxed = true)
    private val firebaseUser: FirebaseUser = mockk()

    private lateinit var dataSynchronizer: DataSynchronizer

    private val professorId = "prof123"

    @Before
    fun setup() {
        every { auth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns professorId
        
        dataSynchronizer = DataSynchronizer(
            studentDao,
            scheduleDao,
            scheduleExceptionDao,
            remoteDataSource,
            conflictResolver,
            auth,
            syncMetadataDao,
            secureAuthManager
        )
    }

    @Test
    fun `performSync should upload pending changes and download updates`() = runTest {
        // Given
        val pendingStudent = StudentEntity(
            id = 1,
            professorId = professorId,
            name = "John Doe",
            age = 20,
            address = "123 Main St",
            parentPhones = "555-1234",
            studentPhone = "555-5678",
            studentEmail = "john@example.com",
            subjects = "Math",
            course = "Grade 10",
            pricePerHour = 25.0,
            educationalAttention = "Standard",
            lastPaymentDate = null,
            lastModifiedTimestamp = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING_UPLOAD
        )
        
        coEvery { studentDao.getStudentsBySyncStatus(professorId, SyncStatus.PENDING_UPLOAD) } returns listOf(pendingStudent)
        coEvery { studentDao.getStudentsBySyncStatus(professorId, SyncStatus.PENDING_DELETE) } returns emptyList()
        coEvery { scheduleDao.getSchedulesBySyncStatus(any(), any()) } returns emptyList()
        coEvery { scheduleExceptionDao.getExceptionsBySyncStatus(any(), any()) } returns emptyList()
        
        coEvery { remoteDataSource.uploadDocument(any(), any(), any(), any()) } returns "cloud_id_123"
        coEvery { remoteDataSource.downloadCollection(any(), any()) } returns emptyList()
        
        every { studentDao.getAllStudents(professorId) } returns flowOf(emptyList())

        // When
        dataSynchronizer.performSync()

        // Then
        coVerify { 
            remoteDataSource.uploadDocument(
                path = "professors/$professorId/students",
                data = any(),
                idempotencyKey = "John Doe",
                idempotencyField = "name"
            )
        }
        coVerify { studentDao.updateStudent(any()) }
        coVerify { syncMetadataDao.setLastSyncTimestamp(any()) }
    }

    @Test
    fun `performSync should handle deletion of students`() = runTest {
        // Given
        val studentToDelete = StudentEntity(
            id = 2,
            professorId = professorId,
            name = "To Delete",
            age = 0, address = "", parentPhones = "", studentPhone = "", studentEmail = "",
            subjects = "", course = "", pricePerHour = 0.0, educationalAttention = "",
            lastPaymentDate = null,
            cloudId = "cloud_id_delete",
            syncStatus = SyncStatus.PENDING_DELETE
        )
        
        coEvery { studentDao.getStudentsBySyncStatus(professorId, SyncStatus.PENDING_UPLOAD) } returns emptyList()
        coEvery { studentDao.getStudentsBySyncStatus(professorId, SyncStatus.PENDING_DELETE) } returns listOf(studentToDelete)
        coEvery { scheduleDao.getSchedulesBySyncStatus(any(), any()) } returns emptyList()
        coEvery { scheduleExceptionDao.getExceptionsBySyncStatus(any(), any()) } returns emptyList()
        
        coEvery { remoteDataSource.downloadCollection(any(), any()) } returns emptyList()
        every { studentDao.getAllStudents(professorId) } returns flowOf(emptyList())

        // When
        dataSynchronizer.performSync()

        // Then
        coVerify { remoteDataSource.deleteStudentData(professorId, "cloud_id_delete") }
        coVerify { studentDao.deleteStudent(any()) }
    }
    @Test
    fun `performSync should delete local students during incremental sync if missing from full remote set`() = runTest {
        // Given
        val lastSyncTimestamp = 1000L
        coEvery { syncMetadataDao.getLastSyncTimestamp() } returns lastSyncTimestamp
        
        val localStudent = StudentEntity(
            id = 1, professorId = professorId, name = "Local Student", cloudId = "cloud_1",
            syncStatus = SyncStatus.SYNCED, age = 0, address = "", parentPhones = "", studentPhone = "", studentEmail = "",
            subjects = "", course = "", pricePerHour = 0.0, educationalAttention = "", lastPaymentDate = null
        )
        
        every { studentDao.getAllStudents(professorId) } returns flowOf(listOf(localStudent))
        // Full fetch returns empty — student was deleted on another device
        coEvery { remoteDataSource.downloadCollection("professors/$professorId/students", 0L) } returns emptyList()
        // Incremental fetch also returns empty
        coEvery { remoteDataSource.downloadCollection("professors/$professorId/students", lastSyncTimestamp) } returns emptyList()
        
        // Mock other parts of sync to avoid NPEs
        coEvery { studentDao.getStudentsBySyncStatus(any(), any()) } returns emptyList()
        coEvery { scheduleDao.getSchedulesBySyncStatus(any(), any()) } returns emptyList()
        coEvery { scheduleExceptionDao.getExceptionsBySyncStatus(any(), any()) } returns emptyList()

        // When
        dataSynchronizer.performSync()

        // Then — ghost-data reconciliation should delete even during incremental sync
        coVerify(exactly = 1) { studentDao.deleteStudent(localStudent) }
    }

    @Test
    fun `performSync should NOT delete local students with pending uploads even if missing from remote`() = runTest {
        // Given
        val lastSyncTimestamp = 1000L
        coEvery { syncMetadataDao.getLastSyncTimestamp() } returns lastSyncTimestamp
        
        val localStudent = StudentEntity(
            id = 1, professorId = professorId, name = "Pending Student", cloudId = "cloud_1",
            syncStatus = SyncStatus.PENDING_UPLOAD, age = 0, address = "", parentPhones = "", studentPhone = "", studentEmail = "",
            subjects = "", course = "", pricePerHour = 0.0, educationalAttention = "", lastPaymentDate = null
        )
        
        every { studentDao.getAllStudents(professorId) } returns flowOf(listOf(localStudent))
        coEvery { remoteDataSource.downloadCollection("professors/$professorId/students", 0L) } returns emptyList()
        coEvery { remoteDataSource.downloadCollection("professors/$professorId/students", lastSyncTimestamp) } returns emptyList()
        
        coEvery { studentDao.getStudentsBySyncStatus(any(), any()) } returns emptyList()
        coEvery { scheduleDao.getSchedulesBySyncStatus(any(), any()) } returns emptyList()
        coEvery { scheduleExceptionDao.getExceptionsBySyncStatus(any(), any()) } returns emptyList()

        // When
        dataSynchronizer.performSync()

        // Then — students with PENDING_UPLOAD should never be ghost-deleted
        coVerify(exactly = 0) { studentDao.deleteStudent(localStudent) }
    }

    @Test
    fun `performSync should delete local students during full sync if missing from remote`() = runTest {
        // Given
        val lastSyncTimestamp = 0L
        coEvery { syncMetadataDao.getLastSyncTimestamp() } returns lastSyncTimestamp
        
        val localStudent = StudentEntity(
            id = 1, professorId = professorId, name = "Local Student", cloudId = "cloud_1",
            syncStatus = SyncStatus.SYNCED, age = 0, address = "", parentPhones = "", studentPhone = "", studentEmail = "",
            subjects = "", course = "", pricePerHour = 0.0, educationalAttention = "", lastPaymentDate = null
        )
        
        every { studentDao.getAllStudents(professorId) } returns flowOf(listOf(localStudent))
        coEvery { remoteDataSource.downloadCollection(any(), 0L) } returns emptyList() // Missing from full fetch
        
        // Mock other parts of sync
        coEvery { studentDao.getStudentsBySyncStatus(any(), any()) } returns emptyList()
        coEvery { scheduleDao.getSchedulesBySyncStatus(any(), any()) } returns emptyList()
        coEvery { scheduleExceptionDao.getExceptionsBySyncStatus(any(), any()) } returns emptyList()

        // When
        dataSynchronizer.performSync()

        // Then
        coVerify(exactly = 1) { studentDao.deleteStudent(localStudent) }
    }

    @Test
    fun `cleanFirestoreDuplicates should group case-insensitively and deduplicate`() = runTest {
        // Given — two Firestore documents with "Ana" and "ana" (different cases)
        val olderDoc = RemoteDocument(
            id = "doc_older",
            data = mapOf("name" to "ana", "lastModified" to 1000L)
        )
        val newerDoc = RemoteDocument(
            id = "doc_newer",
            data = mapOf("name" to "Ana", "lastModified" to 2000L)
        )
        
        coEvery {
            remoteDataSource.downloadCollection("professors/$professorId/students", 0L)
        } returns listOf(olderDoc, newerDoc)
        
        // Mock other parts of sync
        coEvery { studentDao.getStudentsBySyncStatus(any(), any()) } returns emptyList()
        coEvery { scheduleDao.getSchedulesBySyncStatus(any(), any()) } returns emptyList()
        coEvery { scheduleExceptionDao.getExceptionsBySyncStatus(any(), any()) } returns emptyList()
        every { studentDao.getAllStudents(professorId) } returns flowOf(emptyList())
        // Force cleanup by setting lastCleanupTimestamp to 0
        coEvery { syncMetadataDao.getLastCleanupTimestamp() } returns 0L

        // When
        dataSynchronizer.performSync()

        // Then — the older doc should be deleted, the newer one kept
        coVerify(exactly = 1) { remoteDataSource.deleteDocument("professors/$professorId/students/doc_older") }
        coVerify(exactly = 0) { remoteDataSource.deleteDocument("professors/$professorId/students/doc_newer") }
    }
}
