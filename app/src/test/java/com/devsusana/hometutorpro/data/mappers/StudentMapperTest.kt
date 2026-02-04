package com.devsusana.hometutorpro.data.mappers

import com.devsusana.hometutorpro.data.mappers.toData
import com.devsusana.hometutorpro.data.mappers.toDomain
import com.devsusana.hometutorpro.data.models.StudentDataModel
import com.devsusana.hometutorpro.domain.entities.Student
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Student mapper functions.
 * 
 * Tests bidirectional conversion between StudentDataModel and Student domain entity:
 * - Data model to domain entity conversion
 * - Domain entity to data model conversion
 * - Round-trip conversion preserves data
 */
class StudentMapperTest {

    @Test
    fun `toDomain should convert StudentDataModel to Student correctly`() {
        // Given
        val dataModel = StudentDataModel(
            id = "student123",
            professorId = "prof456",
            name = "John Doe",
            age = 15,
            address = "123 Main St",
            parentPhones = "555-1234",
            subjects = "Math, Physics",
            course = "3º ESO",
            pricePerHour = 25.0,
            pendingBalance = 50.0,
            educationalAttention = "None",
            lastPaymentDate = 1234567890L,
            color = -16777216 // Black
        )

        // When
        val domain = dataModel.toDomain()

        // Then
        assertEquals(dataModel.id, domain.id)
        assertEquals(dataModel.professorId, domain.professorId)
        assertEquals(dataModel.name, domain.name)
        assertEquals(dataModel.age, domain.age)
        assertEquals(dataModel.address, domain.address)
        assertEquals(dataModel.parentPhones, domain.parentPhones)
        assertEquals(dataModel.subjects, domain.subjects)
        assertEquals(dataModel.course, domain.course)
        assertEquals(dataModel.pricePerHour, domain.pricePerHour, 0.001)
        assertEquals(dataModel.pendingBalance, domain.pendingBalance, 0.001)
        assertEquals(dataModel.educationalAttention, domain.educationalAttention)
        assertEquals(dataModel.lastPaymentDate, domain.lastPaymentDate)
        assertEquals(dataModel.color, domain.color)
    }

    @Test
    fun `toData should convert Student to StudentDataModel correctly`() {
        // Given
        val domain = Student(
            id = "student123",
            professorId = "prof456",
            name = "Jane Smith",
            age = 16,
            address = "456 Oak Ave",
            parentPhones = "555-5678",
            subjects = "Chemistry, Biology",
            course = "4º ESO",
            pricePerHour = 30.0,
            pendingBalance = 60.0,
            educationalAttention = "Dyslexia",
            lastPaymentDate = 9876543210L,
            color = -65536 // Red
        )

        // When
        val dataModel = domain.toData()

        // Then
        assertEquals(domain.id, dataModel.id)
        assertEquals(domain.professorId, dataModel.professorId)
        assertEquals(domain.name, dataModel.name)
        assertEquals(domain.age, dataModel.age)
        assertEquals(domain.address, dataModel.address)
        assertEquals(domain.parentPhones, dataModel.parentPhones)
        assertEquals(domain.subjects, dataModel.subjects)
        assertEquals(domain.course, dataModel.course)
        assertEquals(domain.pricePerHour, dataModel.pricePerHour, 0.001)
        assertEquals(domain.pendingBalance, dataModel.pendingBalance, 0.001)
        assertEquals(domain.educationalAttention, dataModel.educationalAttention)
        assertEquals(domain.lastPaymentDate, dataModel.lastPaymentDate)
        assertEquals(domain.color, dataModel.color)
    }

    @Test
    fun `round trip conversion should preserve all data`() {
        // Given
        val original = Student(
            id = "student789",
            professorId = "prof101",
            name = "Alice Johnson",
            age = 14,
            address = "789 Pine Rd",
            parentPhones = "555-9999",
            subjects = "English, History",
            course = "2º ESO",
            pricePerHour = 20.0,
            pendingBalance = 40.0,
            educationalAttention = "",
            lastPaymentDate = null,
            color = -16711936 // Green
        )

        // When
        val roundTrip = original.toData().toDomain()

        // Then
        assertEquals(original.id, roundTrip.id)
        assertEquals(original.professorId, roundTrip.professorId)
        assertEquals(original.name, roundTrip.name)
        assertEquals(original.age, roundTrip.age)
        assertEquals(original.address, roundTrip.address)
        assertEquals(original.parentPhones, roundTrip.parentPhones)
        assertEquals(original.subjects, roundTrip.subjects)
        assertEquals(original.course, roundTrip.course)
        assertEquals(original.pricePerHour, roundTrip.pricePerHour, 0.001)
        assertEquals(original.pendingBalance, roundTrip.pendingBalance, 0.001)
        assertEquals(original.educationalAttention, roundTrip.educationalAttention)
        assertEquals(original.lastPaymentDate, roundTrip.lastPaymentDate)
        assertEquals(original.color, roundTrip.color)
    }

    @Test
    fun `toDomain should handle null lastPaymentDate and color`() {
        // Given
        val dataModel = StudentDataModel(
            id = "student999",
            professorId = "prof999",
            name = "Test Student",
            age = 15,
            address = "Test Address",
            parentPhones = "555-0000",
            subjects = "Math",
            course = "3º ESO",
            pricePerHour = 25.0,
            pendingBalance = 0.0,
            educationalAttention = "",
            lastPaymentDate = null,
            color = null
        )

        // When
        val domain = dataModel.toDomain()

        // Then
        assertNull(domain.lastPaymentDate)
        assertNull(domain.color)
    }

    @Test
    fun `toData should handle empty strings and nulls correctly`() {
        // Given
        val domain = Student(
            id = "student000",
            professorId = "prof000",
            name = "Empty Fields Student",
            age = 0,
            address = "",
            parentPhones = "",
            subjects = "",
            course = "",
            pricePerHour = 0.0,
            pendingBalance = 0.0,
            educationalAttention = "",
            lastPaymentDate = null,
            color = null
        )

        // When
        val dataModel = domain.toData()

        // Then
        assertEquals("", dataModel.address)
        assertEquals("", dataModel.parentPhones)
        assertEquals("", dataModel.subjects)
        assertEquals("", dataModel.course)
        assertEquals("", dataModel.educationalAttention)
        assertEquals(0.0, dataModel.pricePerHour, 0.001)
        assertEquals(0.0, dataModel.pendingBalance, 0.001)
        assertNull(dataModel.color)
    }
}
