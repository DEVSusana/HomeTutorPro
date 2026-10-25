package com.devsusana.hometutorpro.data.mappers

import com.devsusana.hometutorpro.data.security.SecureAuthManager
import com.devsusana.hometutorpro.data.local.entities.*
import com.devsusana.hometutorpro.data.remote.RemoteDocument
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek

class FirestoreMappersTest {

    private val secureAuthManager: SecureAuthManager = mockk()

    @Test
    fun `StudentEntity toFirestoreMap should encrypt PII`() {
        // Given
        val entity = StudentEntity(
            professorId = "prof123",
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
            lastModifiedTimestamp = 123456789L
        )
        every { secureAuthManager.encryptPII("123 Main St") } returns "encrypted_address"
        every { secureAuthManager.encryptPII("555-1234") } returns "encrypted_parents"
        every { secureAuthManager.encryptPII("555-5678") } returns "encrypted_student"
        every { secureAuthManager.encryptPII("john@example.com") } returns "encrypted_email"

        // When
        val map = entity.toFirestoreMap(secureAuthManager)

        // Then
        assertEquals("John Doe", map["name"])
        assertEquals("encrypted_address", map["address"])
        assertEquals("encrypted_parents", map["parentPhones"])
        assertEquals("encrypted_student", map["studentPhone"])
        assertEquals("encrypted_email", map["studentEmail"])
        assertEquals(123456789L, map["lastModified"])
    }

    @Test
    fun `RemoteDocument toStudentEntity should decrypt PII`() {
        // Given
        val remoteDoc = RemoteDocument(
            id = "cloud123",
            data = mapOf(
                "name" to "John Doe",
                "address" to "encrypted_address",
                "age" to 20L,
                "pricePerHour" to 25.0,
                "lastModified" to 123456789L
            )
        )
        every { secureAuthManager.decryptPII("encrypted_address") } returns "123 Main St"
        every { secureAuthManager.decryptPII(null) } returns ""

        // When
        val entity = remoteDoc.toStudentEntity(secureAuthManager, "prof123")

        // Then
        assertEquals("John Doe", entity.name)
        assertEquals("123 Main St", entity.address)
        assertEquals(20, entity.age)
        assertEquals(25.0, entity.pricePerHour, 0.0)
        assertEquals("cloud123", entity.cloudId)
        assertEquals(SyncStatus.SYNCED, entity.syncStatus)
    }

    @Test
    fun `ScheduleEntity toFirestoreMap should map all fields`() {
        // Given
        val entity = ScheduleEntity(
            id = 1,
            professorId = "prof123",
            studentId = 10L,
            dayOfWeek = DayOfWeek.MONDAY,
            startTime = "10:00",
            endTime = "11:00",
            lastModifiedTimestamp = 123456789L
        )

        // When
        val map = entity.toFirestoreMap()

        // Then
        assertEquals(1L, map["localId"])
        assertEquals("MONDAY", map["dayOfWeek"])
        assertEquals("10:00", map["startTime"])
        assertEquals(123456789L, map["lastModified"])
    }

    @Test
    fun `RemoteDocument toScheduleEntity should map firebase fields`() {
        // Given
        val remoteDoc = RemoteDocument(
            id = "cloud_sched",
            data = mapOf(
                "dayOfWeek" to "WEDNESDAY",
                "startTime" to "14:00",
                "endTime" to "15:00",
                "isCompleted" to true,
                "lastModified" to 123456789L,
                "completedDate" to 987654321L
            )
        )

        // When
        val entity = remoteDoc.toScheduleEntity(10L, "prof123")

        // Then
        assertEquals(DayOfWeek.WEDNESDAY, entity.dayOfWeek)
        assertEquals("14:00", entity.startTime)
        assertEquals(true, entity.isCompleted)
        assertEquals(123456789L, entity.lastModifiedTimestamp)
        assertEquals(10L, entity.studentId)
        assertEquals("prof123", entity.professorId)
    }

    @Test
    fun `ScheduleExceptionEntity toFirestoreMap should map all fields`() {
        // Given
        val entity = ScheduleExceptionEntity(
            id = 1,
            professorId = "prof123",
            studentId = 10L,
            originalScheduleId = "sched1",
            exceptionDate = 1696204800000L,
            reason = "Holiday",
            type = "CANCELLED",
            newStartTime = "11:00",
            newEndTime = "12:00",
            newDayOfWeek = DayOfWeek.TUESDAY,
            lastModifiedTimestamp = 123456789L
        )

        // When
        val map = entity.toFirestoreMap()

        // Then
        assertEquals(1L, map["localId"])
        assertEquals("sched1", map["originalScheduleId"])
        assertEquals(1696204800000L, map["exceptionDate"])
        assertEquals("TUESDAY", map["newDayOfWeek"])
        assertEquals("CANCELLED", map["type"])
    }

    @Test
    fun `RemoteDocument toScheduleExceptionEntity should map firebase fields`() {
        // Given
        val remoteDoc = RemoteDocument(
            id = "cloud_exc",
            data = mapOf(
                "originalScheduleId" to "sched_rem",
                "exceptionDate" to 1696204800000L,
                "reason" to "Sick",
                "type" to "RESCHEDULED",
                "newStartTime" to "10:30",
                "newEndTime" to "11:30",
                "newDayOfWeek" to "FRIDAY",
                "lastModified" to 123456789L
            )
        )

        // When
        val entity = remoteDoc.toScheduleExceptionEntity(20L, "prof456")

        // Then
        assertEquals("cloud_exc", entity.cloudId)
        assertEquals("sched_rem", entity.originalScheduleId)
        assertEquals(1696204800000L, entity.exceptionDate)
        assertEquals(DayOfWeek.FRIDAY, entity.newDayOfWeek)
        assertEquals("RESCHEDULED", entity.type)
        assertEquals(20L, entity.studentId)
        assertEquals("prof456", entity.professorId)
    }
}
