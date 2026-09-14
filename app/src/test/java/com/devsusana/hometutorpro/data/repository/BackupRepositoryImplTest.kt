package com.devsusana.hometutorpro.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.devsusana.hometutorpro.data.local.AppDatabase
import com.devsusana.hometutorpro.data.local.dao.ResourceDao
import com.devsusana.hometutorpro.data.local.dao.ScheduleDao
import com.devsusana.hometutorpro.data.local.dao.ScheduleExceptionDao
import com.devsusana.hometutorpro.data.local.dao.StudentDao
import com.devsusana.hometutorpro.data.local.entities.StudentEntity
import com.devsusana.hometutorpro.data.models.AppBackup
import com.devsusana.hometutorpro.data.security.BackupCryptoHelper
import com.devsusana.hometutorpro.data.security.SecureAuthManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BackupRepositoryImplTest {

    private lateinit var database: AppDatabase
    private lateinit var studentDao: StudentDao
    private lateinit var scheduleDao: ScheduleDao
    private lateinit var scheduleExceptionDao: ScheduleExceptionDao
    private lateinit var resourceDao: ResourceDao
    private lateinit var auth: FirebaseAuth
    private lateinit var firebaseUser: FirebaseUser
    private lateinit var secureAuthManager: SecureAuthManager

    private lateinit var repository: BackupRepositoryImpl

    private val testUid = "prof_uid_12345"
    private val otherUid = "prof_uid_99999"

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.i(any(), any()) } returns 0

        database = mockk(relaxed = true)
        studentDao = mockk(relaxed = true)
        scheduleDao = mockk(relaxed = true)
        scheduleExceptionDao = mockk(relaxed = true)
        resourceDao = mockk(relaxed = true)
        auth = mockk(relaxed = true)
        firebaseUser = mockk(relaxed = true)
        secureAuthManager = mockk(relaxed = true)

        every { database.studentDao() } returns studentDao
        every { database.scheduleDao() } returns scheduleDao
        every { database.scheduleExceptionDao() } returns scheduleExceptionDao
        every { database.resourceDao() } returns resourceDao

        every { auth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns testUid
        every { secureAuthManager.getUserId() } returns testUid

        // Mock withTransaction to execute the block directly
        mockkStatic("androidx.room.RoomDatabaseKt")
        val transactionLambda = slot<suspend () -> Any>()
        coEvery { database.withTransaction(capture(transactionLambda)) } coAnswers {
            transactionLambda.captured.invoke()
        }

        repository = BackupRepositoryImpl(
            database = database,
            auth = auth,
            secureAuthManager = secureAuthManager
        )
    }

    private fun createTestStudent(
        id: Long = 1L,
        professorId: String = testUid,
        name: String = "Test Student",
        address: String = "Calle Test 123"
    ): StudentEntity {
        return StudentEntity(
            id = id,
            professorId = professorId,
            name = name,
            age = 15,
            address = address,
            parentPhones = "123456789",
            studentPhone = "987654321",
            studentEmail = "test@example.com",
            subjects = "Matemáticas",
            course = "4º ESO",
            pricePerHour = 25.0,
            pendingBalance = 50.0,
            educationalAttention = "None",
            lastPaymentDate = null,
            color = 0xFF0000
        )
    }

    @Test
    fun createBackup_producesEncryptedJson_withoutPlainTextData() = runTest {
        val sensitiveStudentName = "Juan Perez Garcia"
        val mockStudent = createTestStudent(
            name = sensitiveStudentName,
            address = "Calle Secreta 123"
        )
        coEvery { studentDao.getAllStudentsOnce(testUid) } returns listOf(mockStudent)

        val backupOutput = repository.createBackup()

        assertNotNull(backupOutput)
        assertTrue("Output should contain cipherTextBase64", backupOutput.contains("cipherTextBase64"))
        assertTrue("Output should contain algorithm info", backupOutput.contains("AES-256-GCM-PBKDF2"))
        assertFalse("Output must NOT contain plain text student name", backupOutput.contains(sensitiveStudentName))
        assertFalse("Output must NOT contain plain text address", backupOutput.contains("Calle Secreta"))
    }

    @Test
    fun restoreBackup_successfullyDecryptsAndInserts_whenSameUid() = runTest {
        val mockStudent = createTestStudent(name = "Maria Lopez")
        coEvery { studentDao.getAllStudentsOnce(testUid) } returns listOf(mockStudent)

        val encryptedJson = repository.createBackup()

        val result = repository.restoreBackup(encryptedJson)

        assertTrue("Restore should succeed", result.isSuccess)
        coVerify { studentDao.insertStudent(match { it.name == "Maria Lopez" && it.professorId == testUid }) }
    }

    @Test
    fun restoreBackup_failsWithSecurityException_whenDifferentUid() = runTest {
        val mockStudent = createTestStudent(name = "Carlos Santana")
        coEvery { studentDao.getAllStudentsOnce(testUid) } returns listOf(mockStudent)

        val encryptedJson = repository.createBackup()

        // Switch active user to otherUid
        every { firebaseUser.uid } returns otherUid
        every { secureAuthManager.getUserId() } returns otherUid

        val result = repository.restoreBackup(encryptedJson)

        assertTrue("Restore should fail for different user UID", result.isFailure)
        assertTrue("Exception should be SecurityException", result.exceptionOrNull() is SecurityException)
        coVerify(exactly = 0) { studentDao.insertStudent(any()) }
    }

    @Test
    fun restoreBackup_supportsLegacyUnencryptedV1Backup() = runTest {
        val legacyBackup = AppBackup(
            version = 1,
            students = listOf(createTestStudent(id = 99L, professorId = "old_prof", name = "Alumno Legado")),
            schedules = emptyList(),
            exceptions = emptyList(),
            resources = emptyList()
        )
        val legacyJson = Json.encodeToString(legacyBackup)

        val result = repository.restoreBackup(legacyJson)

        assertTrue("Legacy v1 restore should succeed", result.isSuccess)
        coVerify { studentDao.insertStudent(match { it.name == "Alumno Legado" && it.professorId == testUid }) }
    }

    @Test
    fun backupCryptoHelper_encryptAndDecryptRoundtrip() {
        val plainText = """{"testKey":"secretValue123"}"""
        val secretKey = "user_secret_uid_abc"

        val encrypted = BackupCryptoHelper.encrypt(plainText, secretKey)
        assertNotNull(encrypted.cipherTextBase64)
        assertNotNull(encrypted.ivBase64)
        assertNotNull(encrypted.saltBase64)

        val decrypted = BackupCryptoHelper.decrypt(encrypted, secretKey)
        assertEquals(plainText, decrypted)
    }

    @Test(expected = SecurityException::class)
    fun backupCryptoHelper_decryptWithWrongKey_throwsSecurityException() {
        val plainText = """{"testKey":"secretValue123"}"""
        val encrypted = BackupCryptoHelper.encrypt(plainText, "correct_key")

        BackupCryptoHelper.decrypt(encrypted, "wrong_key")
    }
}
