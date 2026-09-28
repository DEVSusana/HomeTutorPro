package com.devsusana.hometutorpro.presentation.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditProfileStateTest {

    @Test
    fun `default values are correct`() {
        val state = EditProfileState()

        assertEquals("", state.name)
        assertEquals("", state.email)
        assertEquals("", state.originalEmail)
        assertFalse(state.isLoading)
        assertEquals("08:00", state.workingStartTime)
        assertEquals("23:00", state.workingEndTime)
        assertEquals("", state.notes)
        assertNull(state.successMessage)
        assertNull(state.errorMessage)
    }

    @Test
    fun `custom values and copy work as expected`() {
        val state = EditProfileState(
            name = "Susana",
            email = "susana@example.com",
            originalEmail = "old@example.com",
            isLoading = true,
            workingStartTime = "09:00",
            workingEndTime = "21:00",
            notes = "Special notes",
            successMessage = "Profile updated",
            errorMessage = "Update failed"
        )

        assertEquals("Susana", state.name)
        assertEquals("susana@example.com", state.email)
        assertEquals("old@example.com", state.originalEmail)
        assertTrue(state.isLoading)
        assertEquals("09:00", state.workingStartTime)
        assertEquals("21:00", state.workingEndTime)
        assertEquals("Special notes", state.notes)
        assertEquals("Profile updated", state.successMessage)
        assertEquals("Update failed", state.errorMessage)

        val copied = state.copy(isLoading = false, name = "Susana Cordoba")
        assertFalse(copied.isLoading)
        assertEquals("Susana Cordoba", copied.name)
        assertEquals("susana@example.com", copied.email)
    }
}
