package com.devsusana.hometutorpro.presentation.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EditProfileUiEventTest {

    @Test
    fun `test edit profile ui events creation and values`() {
        val nameEvent = EditProfileUiEvent.NameChanged("Susana")
        assertEquals("Susana", nameEvent.name)

        val emailEvent = EditProfileUiEvent.EmailChanged("susana@example.com")
        assertEquals("susana@example.com", emailEvent.email)

        val startTimeEvent = EditProfileUiEvent.WorkingStartTimeChanged("08:00")
        assertEquals("08:00", startTimeEvent.time)

        val endTimeEvent = EditProfileUiEvent.WorkingEndTimeChanged("22:00")
        assertEquals("22:00", endTimeEvent.time)

        org.junit.Assert.assertNotNull(EditProfileUiEvent.SaveProfile)
        org.junit.Assert.assertNotNull(EditProfileUiEvent.DismissFeedback)
    }
}
