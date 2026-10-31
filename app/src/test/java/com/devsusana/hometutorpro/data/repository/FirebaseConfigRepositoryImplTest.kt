package com.devsusana.hometutorpro.data.repository

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FirebaseConfigRepositoryImplTest {

    private lateinit var context: Context
    private lateinit var remoteConfig: FirebaseRemoteConfig
    private lateinit var mockActivateTask: Task<Boolean>

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        remoteConfig = mockk(relaxed = true)
        mockActivateTask = mockk(relaxed = true)

        every { remoteConfig.activate() } returns mockActivateTask
        every { remoteConfig.setConfigSettingsAsync(any()) } returns mockk(relaxed = true)
        every { remoteConfig.setDefaultsAsync(any<Int>()) } returns mockk(relaxed = true)
    }

    @Test
    fun isWidgetActionsEnabled_readsFromRemoteConfig() {
        every { remoteConfig.getBoolean(FirebaseConfigRepositoryImpl.KEY_ENABLE_WIDGET_ACTIONS) } returns false

        val repository = FirebaseConfigRepositoryImpl(context, remoteConfig)

        assertFalse(repository.isWidgetActionsEnabled())
    }

    @Test
    fun isSueLocalAiEnabled_readsFromRemoteConfig() {
        every { remoteConfig.getBoolean(FirebaseConfigRepositoryImpl.KEY_ENABLE_SUE_LOCAL_AI) } returns true

        val repository = FirebaseConfigRepositoryImpl(context, remoteConfig)

        assertTrue(repository.isSueLocalAiEnabled())
    }

    @Test
    fun getSueModelUrl_readsFromRemoteConfig() {
        val expectedUrl = "https://custom.url/model.task"
        every { remoteConfig.getString(FirebaseConfigRepositoryImpl.KEY_SUE_MODEL_URL) } returns expectedUrl

        val repository = FirebaseConfigRepositoryImpl(context, remoteConfig)

        assertEquals(expectedUrl, repository.getSueModelUrl())
    }

    @Test
    fun getMaintenanceMessage_readsFromRemoteConfig() {
        val expectedMsg = "Mantenimiento en curso"
        every { remoteConfig.getString(FirebaseConfigRepositoryImpl.KEY_MAINTENANCE_MESSAGE) } returns expectedMsg

        val repository = FirebaseConfigRepositoryImpl(context, remoteConfig)

        assertEquals(expectedMsg, repository.getMaintenanceMessage())
    }
}
