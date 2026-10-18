package com.devsusana.hometutorpro.presentation.viewmodels

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [PermissionViewModel].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PermissionViewModelTest {

    private lateinit var viewModel: PermissionViewModel
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        viewModel = PermissionViewModel()
    }

    @Test
    fun `initial state of showAlarmDialog is false`() {
        assertFalse(viewModel.showAlarmDialog.value)
    }

    @Test
    fun `dismissAlarmDialog sets showAlarmDialog to false`() {
        viewModel.dismissAlarmDialog()
        assertFalse(viewModel.showAlarmDialog.value)
    }

    @Test
    fun `checkPermissions triggers callback when notification permission is not granted on Tiramisu+`() {
        var requested = false
        viewModel.checkPermissions(context) {
            requested = true
        }
        assertTrue(requested)
    }

    @Test
    fun `onNotificationPermissionResult handles result and allows dismissal`() {
        viewModel.onNotificationPermissionResult(context, isGranted = true)
        viewModel.dismissAlarmDialog()
        assertFalse(viewModel.showAlarmDialog.value)
    }
}
