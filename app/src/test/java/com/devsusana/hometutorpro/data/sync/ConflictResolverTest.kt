package com.devsusana.hometutorpro.data.sync

import com.devsusana.hometutorpro.data.local.dao.StudentDao
import com.devsusana.hometutorpro.data.local.entities.StudentEntity
import com.devsusana.hometutorpro.data.local.entities.SyncStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ConflictResolverTest {

    private lateinit var studentDao: StudentDao
    private lateinit var conflictResolver: ConflictResolver

    @Before
    fun setup() {
        studentDao = mockk(relaxed = true)
        conflictResolver = ConflictResolver(studentDao)
    }

    @Test
    fun `resolveConflict when local is newer should keep local and set status to PENDING_UPLOAD`() = runTest {
        val local = StudentEntity(
            id = 1L,
            professorId = "prof1",
            name = "Local Student",
            age = 15,
            course = "Math",
            subjects = "Algebra",
            parentPhones = "123",
            studentPhone = "456",
            studentEmail = "local@test.com",
            address = "Street 1",
            pricePerHour = 20.0,
            educationalAttention = "None",
            lastPaymentDate = null,
            lastModifiedTimestamp = 2000L,
            syncStatus = SyncStatus.SYNCED
        )
        val remote = StudentEntity(
            id = 0L,
            professorId = "prof1",
            name = "Remote Student",
            age = 15,
            course = "Math",
            subjects = "Algebra",
            parentPhones = "123",
            studentPhone = "456",
            studentEmail = "remote@test.com",
            address = "Street 1",
            pricePerHour = 20.0,
            educationalAttention = "None",
            lastPaymentDate = null,
            lastModifiedTimestamp = 1000L,
            syncStatus = SyncStatus.SYNCED
        )

        val slot = slot<StudentEntity>()
        coEvery { studentDao.updateStudent(capture(slot)) } returns Unit

        conflictResolver.resolveConflict(local, remote)

        coVerify(exactly = 1) { studentDao.updateStudent(any()) }
        assertEquals(1L, slot.captured.id)
        assertEquals("Local Student", slot.captured.name)
        assertEquals(SyncStatus.PENDING_UPLOAD, slot.captured.syncStatus)
    }

    @Test
    fun `resolveConflict when remote is newer should use remote data keeping local id and set status to SYNCED`() = runTest {
        val local = StudentEntity(
            id = 1L,
            professorId = "prof1",
            name = "Local Student",
            age = 15,
            course = "Math",
            subjects = "Algebra",
            parentPhones = "123",
            studentPhone = "456",
            studentEmail = "local@test.com",
            address = "Street 1",
            pricePerHour = 20.0,
            educationalAttention = "None",
            lastPaymentDate = null,
            lastModifiedTimestamp = 1000L,
            syncStatus = SyncStatus.SYNCED
        )
        val remote = StudentEntity(
            id = 999L,
            professorId = "prof1",
            name = "Remote Student",
            age = 16,
            course = "Physics",
            subjects = "Mechanics",
            parentPhones = "456",
            studentPhone = "789",
            studentEmail = "remote@test.com",
            address = "Street 2",
            pricePerHour = 30.0,
            educationalAttention = "Special",
            lastPaymentDate = null,
            lastModifiedTimestamp = 2000L,
            syncStatus = SyncStatus.PENDING_UPLOAD
        )

        val slot = slot<StudentEntity>()
        coEvery { studentDao.updateStudent(capture(slot)) } returns Unit

        conflictResolver.resolveConflict(local, remote)

        coVerify(exactly = 1) { studentDao.updateStudent(any()) }
        assertEquals(1L, slot.captured.id)
        assertEquals("Remote Student", slot.captured.name)
        assertEquals(16, slot.captured.age)
        assertEquals("Physics", slot.captured.course)
        assertEquals(30.0, slot.captured.pricePerHour, 0.001)
        assertEquals(SyncStatus.SYNCED, slot.captured.syncStatus)
    }

    @Test
    fun `resolveConflict when timestamps are equal should let remote win and keep local id`() = runTest {
        val local = StudentEntity(
            id = 1L,
            professorId = "prof1",
            name = "Local Student",
            age = 15,
            course = "Math",
            subjects = "Algebra",
            parentPhones = "123",
            studentPhone = "456",
            studentEmail = "local@test.com",
            address = "Street 1",
            pricePerHour = 20.0,
            educationalAttention = "None",
            lastPaymentDate = null,
            lastModifiedTimestamp = 1500L,
            syncStatus = SyncStatus.SYNCED
        )
        val remote = StudentEntity(
            id = 0L,
            professorId = "prof1",
            name = "Remote Student",
            age = 15,
            course = "Math",
            subjects = "Algebra",
            parentPhones = "123",
            studentPhone = "456",
            studentEmail = "remote@test.com",
            address = "Street 1",
            pricePerHour = 20.0,
            educationalAttention = "None",
            lastPaymentDate = null,
            lastModifiedTimestamp = 1500L,
            syncStatus = SyncStatus.SYNCED
        )

        val slot = slot<StudentEntity>()
        coEvery { studentDao.updateStudent(capture(slot)) } returns Unit

        conflictResolver.resolveConflict(local, remote)

        coVerify(exactly = 1) { studentDao.updateStudent(any()) }
        assertEquals(1L, slot.captured.id)
        assertEquals("Remote Student", slot.captured.name)
        assertEquals(SyncStatus.SYNCED, slot.captured.syncStatus)
    }
}
