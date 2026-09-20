package com.devsusana.hometutorpro.presentation.settings

import com.devsusana.hometutorpro.domain.entities.AppThemeMode
import com.devsusana.hometutorpro.domain.entities.SueDeviceCompatibility
import com.devsusana.hometutorpro.domain.entities.SueModelStatus
import com.devsusana.hometutorpro.domain.entities.SueUnsupportedReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsStateTest {

    @Test
    fun `default values are correct`() {
        val state = SettingsState()

        assertEquals("es", state.language)
        assertEquals(AppThemeMode.SYSTEM, state.themeMode)
        assertTrue(state.classEndNotificationsEnabled)
        assertFalse(state.isDebugPremium)
        assertFalse(state.isSueEnabled)
        assertFalse(state.isSueFabVisible)
        assertTrue(state.sueDeviceCompatibility.isSupported)
        assertEquals(SueModelStatus.NotDownloaded, state.sueModelStatus)
        assertFalse(state.showDeleteModelConfirmDialog)
        assertFalse(state.isBackupLoading)
        assertNull(state.backupMessage)
        assertFalse(state.isBackupSuccess)
        assertNull(state.deleteAccountError)
        assertFalse(state.showChangePasswordDialog)
        assertFalse(state.isChangingPassword)
        assertFalse(state.changePasswordSuccess)
        assertNull(state.changePasswordError)
    }

    @Test
    fun `custom values and copy work as expected`() {
        val downloadedStatus = SueModelStatus.Downloaded(500_000_000L, "500 MB")
        val state = SettingsState(
            language = "en",
            themeMode = AppThemeMode.DARK,
            classEndNotificationsEnabled = false,
            isDebugPremium = true,
            isSueEnabled = true,
            isSueFabVisible = true,
            sueDeviceCompatibility = SueDeviceCompatibility(isSupported = false, reason = SueUnsupportedReason.INSUFFICIENT_RAM),
            sueModelStatus = downloadedStatus,
            showDeleteModelConfirmDialog = true,
            isBackupLoading = true,
            backupMessage = "Export complete",
            isBackupSuccess = true,
            deleteAccountError = 123,
            showChangePasswordDialog = true,
            isChangingPassword = true,
            changePasswordSuccess = true,
            changePasswordError = 456
        )

        assertEquals("en", state.language)
        assertEquals(AppThemeMode.DARK, state.themeMode)
        assertFalse(state.classEndNotificationsEnabled)
        assertTrue(state.isDebugPremium)
        assertTrue(state.isSueEnabled)
        assertTrue(state.isSueFabVisible)
        assertFalse(state.sueDeviceCompatibility.isSupported)
        assertEquals(SueUnsupportedReason.INSUFFICIENT_RAM, state.sueDeviceCompatibility.reason)
        assertEquals(downloadedStatus, state.sueModelStatus)
        assertTrue(state.showDeleteModelConfirmDialog)
        assertTrue(state.isBackupLoading)
        assertEquals("Export complete", state.backupMessage)
        assertTrue(state.isBackupSuccess)
        assertEquals(123, state.deleteAccountError)
        assertTrue(state.showChangePasswordDialog)
        assertTrue(state.isChangingPassword)
        assertTrue(state.changePasswordSuccess)
        assertEquals(456, state.changePasswordError)

        val updated = state.copy(language = "es", isDebugPremium = false)
        assertEquals("es", updated.language)
        assertFalse(updated.isDebugPremium)
        assertEquals(AppThemeMode.DARK, updated.themeMode)
    }
}
