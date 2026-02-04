package com.devsusana.hometutorpro.data.mappers

import com.devsusana.hometutorpro.data.mappers.toData
import com.devsusana.hometutorpro.data.mappers.toDomain
import com.devsusana.hometutorpro.data.models.UserDataModel
import com.devsusana.hometutorpro.domain.entities.User
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for User mapper functions.
 * 
 * Tests bidirectional conversion between UserDataModel and User domain entity.
 */
class UserMapperTest {

    @Test
    fun `toDomain should convert UserDataModel to User correctly`() {
        // Given
        val dataModel = UserDataModel(
            uid = "user123",
            email = "john@example.com",
            displayName = "John Doe"
        )

        // When
        val domain = dataModel.toDomain()

        // Then
        assertEquals(dataModel.uid, domain.uid)
        assertEquals(dataModel.email, domain.email)
        assertEquals(dataModel.displayName, domain.displayName)
    }

    @Test
    fun `toData should convert User to UserDataModel correctly`() {
        // Given
        val domain = User(
            uid = "user456",
            email = "jane@example.com",
            displayName = "Jane Smith"
        )

        // When
        val dataModel = domain.toData()

        // Then
        assertEquals(domain.uid, dataModel.uid)
        assertEquals(domain.email, dataModel.email)
        assertEquals(domain.displayName, dataModel.displayName)
    }

    @Test
    fun `round trip conversion should preserve all data`() {
        // Given
        val original = User(
            uid = "user789",
            email = "alice@example.com",
            displayName = "Alice Johnson"
        )

        // When
        val roundTrip = original.toData().toDomain()

        // Then
        assertEquals(original.uid, roundTrip.uid)
        assertEquals(original.email, roundTrip.email)
        assertEquals(original.displayName, roundTrip.displayName)
    }

    @Test
    fun `toDomain should handle empty strings correctly`() {
        // Given
        val dataModel = UserDataModel(
            uid = "",
            email = "",
            displayName = ""
        )

        // When
        val domain = dataModel.toDomain()

        // Then
        assertEquals("", domain.uid)
        assertEquals("", domain.email)
        assertEquals("", domain.displayName)
    }

    @Test
    fun `toData should preserve special characters in email`() {
        // Given
        val domain = User(
            uid = "user999",
            email = "test+tag@example.co.uk",
            displayName = "Test User"
        )

        // When
        val dataModel = domain.toData()

        // Then
        assertEquals("test+tag@example.co.uk", dataModel.email)
    }

    @Test
    fun `toDomain should handle long display names`() {
        // Given
        val longName = "A".repeat(100)
        val dataModel = UserDataModel(
            uid = "user000",
            email = "test@example.com",
            displayName = longName
        )

        // When
        val domain = dataModel.toDomain()

        // Then
        assertEquals(longName, domain.displayName)
    }
}
