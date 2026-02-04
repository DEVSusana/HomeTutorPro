package com.devsusana.hometutorpro.data.mappers

import com.devsusana.hometutorpro.data.mappers.toData
import com.devsusana.hometutorpro.data.mappers.toDomain
import com.devsusana.hometutorpro.data.models.ResourceDataModel
import com.devsusana.hometutorpro.domain.entities.Resource
import org.junit.Assert.*
import org.junit.Test
import java.util.Date

/**
 * Unit tests for Resource mapper functions.
 * 
 * Tests bidirectional conversion between ResourceDataModel and Resource domain entity.
 * Handles Date to Long timestamp conversion.
 */
class ResourceMapperTest {

    @Test
    fun `toDomain should convert ResourceDataModel to Resource correctly`() {
        // Given
        val uploadTimestamp = 1234567890L
        val dataModel = ResourceDataModel(
            id = "resource123",
            professorId = "prof456",
            name = "Math Worksheet.pdf",
            url = "https://storage.example.com/files/worksheet.pdf",
            type = "application/pdf",
            uploadDate = uploadTimestamp
        )

        // When
        val domain = dataModel.toDomain()

        // Then
        assertEquals(dataModel.id, domain.id)
        assertEquals(dataModel.professorId, domain.professorId)
        assertEquals(dataModel.name, domain.name)
        assertEquals(dataModel.url, domain.url)
        assertEquals(dataModel.type, domain.type)
        assertEquals(Date(uploadTimestamp), domain.uploadDate)
    }

    @Test
    fun `toData should convert Resource to ResourceDataModel correctly`() {
        // Given
        val uploadDate = Date(9876543210L)
        val domain = Resource(
            id = "resource789",
            professorId = "prof101",
            name = "Physics Notes.docx",
            url = "https://storage.example.com/files/notes.docx",
            type = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            uploadDate = uploadDate
        )

        // When
        val dataModel = domain.toData()

        // Then
        assertEquals(domain.id, dataModel.id)
        assertEquals(domain.professorId, dataModel.professorId)
        assertEquals(domain.name, dataModel.name)
        assertEquals(domain.url, dataModel.url)
        assertEquals(domain.type, dataModel.type)
        assertEquals(uploadDate.time, dataModel.uploadDate)
    }

    @Test
    fun `round trip conversion should preserve all data`() {
        // Given
        val original = Resource(
            id = "resource999",
            professorId = "prof999",
            name = "Chemistry Lab.xlsx",
            url = "https://storage.example.com/files/lab.xlsx",
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            uploadDate = Date(1111111111L)
        )

        // When
        val roundTrip = original.toData().toDomain()

        // Then
        assertEquals(original.id, roundTrip.id)
        assertEquals(original.professorId, roundTrip.professorId)
        assertEquals(original.name, roundTrip.name)
        assertEquals(original.url, roundTrip.url)
        assertEquals(original.type, roundTrip.type)
        assertEquals(original.uploadDate, roundTrip.uploadDate)
    }

    @Test
    fun `toDomain should handle different file types`() {
        // Test various MIME types
        val fileTypes = listOf(
            "application/pdf",
            "image/jpeg",
            "application/vnd.ms-excel",
            "application/vnd.ms-powerpoint",
            "video/mp4"
        )

        fileTypes.forEach { mimeType ->
            // Given
            val dataModel = ResourceDataModel(
                id = "test",
                professorId = "prof",
                name = "file.ext",
                url = "https://example.com/file",
                type = mimeType,
                uploadDate = 0L
            )

            // When
            val domain = dataModel.toDomain()

            // Then
            assertEquals(mimeType, domain.type)
        }
    }

    @Test
    fun `toData should preserve long URLs`() {
        // Given
        val longUrl = "https://storage.googleapis.com/bucket-name/very/long/path/to/file/with/many/segments/document.pdf?token=verylongtoken123456789"
        val domain = Resource(
            id = "resource000",
            professorId = "prof000",
            name = "Document.pdf",
            url = longUrl,
            type = "application/pdf",
            uploadDate = Date(0L)
        )

        // When
        val dataModel = domain.toData()

        // Then
        assertEquals(longUrl, dataModel.url)
    }

    @Test
    fun `toDomain should correctly convert timestamp to Date`() {
        // Given
        val timestamp = System.currentTimeMillis()
        val dataModel = ResourceDataModel(
            id = "test",
            professorId = "prof",
            name = "test.pdf",
            url = "https://example.com/test.pdf",
            type = "application/pdf",
            uploadDate = timestamp
        )

        // When
        val domain = dataModel.toDomain()

        // Then
        assertEquals(timestamp, domain.uploadDate.time)
    }
}
