package com.devsusana.hometutorpro.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.devsusana.hometutorpro.data.local.AppDatabase
import com.devsusana.hometutorpro.data.local.entities.StudentEntity
import com.devsusana.hometutorpro.data.local.entities.SyncStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Instrumented test for StudentDao.
 * Verifies database operations for StudentEntity.
 */
@RunWith(AndroidJUnit4::class)
class StudentDaoTest {

    private lateinit var studentDao: StudentDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java
        ).build()
        studentDao = db.studentDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndGetStudent() = runBlocking {
        val student = createTestStudent(name = "Test Student")
        val id = studentDao.insertStudent(student)
        
        val loaded = studentDao.getStudentById(id).first()
        assertNotNull(loaded)
        assertEquals(student.name, loaded?.name)
        assertEquals(student.cloudId, loaded?.cloudId)
    }

    @Test
    fun updateStudent() = runBlocking {
        val student = createTestStudent(name = "Original Name")
        val id = studentDao.insertStudent(student)
        
        val loaded = studentDao.getStudentById(id).first()!!
        val updated = loaded.copy(name = "Updated Name")
        studentDao.updateStudent(updated)
        
        val reloaded = studentDao.getStudentById(id).first()
        assertEquals("Updated Name", reloaded?.name)
    }

    @Test
    fun deleteStudent() = runBlocking {
        val student = createTestStudent(name = "To Delete")
        val id = studentDao.insertStudent(student)
        
        val loaded = studentDao.getStudentById(id).first()!!
        studentDao.deleteStudent(loaded)
        
        val reloaded = studentDao.getStudentById(id).first()
        assertNull(reloaded)
    }

    @Test
    fun getStudentByCloudId() = runBlocking {
        val cloudId = "cloud_123"
        val student = createTestStudent(name = "Cloud Student", cloudId = cloudId)
        studentDao.insertStudent(student)
        
        val loaded = studentDao.getStudentByCloudId(cloudId)
        assertNotNull(loaded)
        assertEquals(cloudId, loaded?.cloudId)
    }

    @Test
    fun markForDeletion() = runBlocking {
        val student = createTestStudent(name = "Mark Delete")
        val id = studentDao.insertStudent(student)
        
        studentDao.markForDeletion(id)
        
        val loaded = studentDao.getStudentById(id).first()
        // Note: getStudentById filters out pendingDelete=1, so it should return null
        assertNull(loaded)
        
        // Verify it still exists in DB but marked
        // We need a raw query or a DAO method that doesn't filter to verify this fully,
        // but getStudentById returning null confirms the filter works.
    }

    private fun createTestStudent(
        name: String,
        cloudId: String? = null
    ): StudentEntity {
        return StudentEntity(
            name = name,
            cloudId = cloudId,
            age = 20,
            address = "Test Address",
            parentPhones = "123456789",
            studentPhone = "987654321",
            studentEmail = "test@example.com",
            subjects = "Math",
            course = "101",
            pricePerHour = 50.0,
            pendingBalance = 0.0,
            educationalAttention = "None",
            lastPaymentDate = null,
            color = null,
            syncStatus = SyncStatus.SYNCED
        )
    }
}
