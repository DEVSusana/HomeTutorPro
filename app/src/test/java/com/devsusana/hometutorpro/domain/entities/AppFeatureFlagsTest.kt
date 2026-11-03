package com.devsusana.hometutorpro.domain.entities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppFeatureFlagsTest {

    @Test
    fun defaultValues_areSensibleAndSafe() {
        val flags = AppFeatureFlags()

        assertTrue(flags.isWidgetActionsEnabled)
        assertTrue(flags.isSueLocalAiEnabled)
        assertEquals("", flags.sueModelUrl)
        assertEquals("", flags.maintenanceMessage)
    }

    @Test
    fun customValues_areRetained() {
        val flags = AppFeatureFlags(
            isWidgetActionsEnabled = false,
            isSueLocalAiEnabled = false,
            sueModelUrl = "https://example.com/custom_model.task",
            maintenanceMessage = "Mantenimiento programado"
        )

        assertEquals(false, flags.isWidgetActionsEnabled)
        assertEquals(false, flags.isSueLocalAiEnabled)
        assertEquals("https://example.com/custom_model.task", flags.sueModelUrl)
        assertEquals("Mantenimiento programado", flags.maintenanceMessage)
    }
}
