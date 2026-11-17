package com.devsusana.hometutorpro.data.repository

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
        every { mockActivateTask.isComplete } returns true
        every { mockActivateTask.isSuccessful } returns true
        every { mockActivateTask.result } returns true

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

    @Test
    fun fetchAndActivate_success_returnsTrueAndUpdatesState() = runTest {
        val successTask = mockk<Task<Boolean>>()
        every { successTask.isComplete } returns true
        every { successTask.isCanceled } returns false
        every { successTask.exception } returns null
        every { successTask.result } returns true

        every { remoteConfig.fetchAndActivate() } returns successTask
        every { remoteConfig.getBoolean(FirebaseConfigRepositoryImpl.KEY_ENABLE_WIDGET_ACTIONS) } returns true
        every { remoteConfig.getBoolean(FirebaseConfigRepositoryImpl.KEY_ENABLE_SUE_LOCAL_AI) } returns true

        val repository = FirebaseConfigRepositoryImpl(context, remoteConfig)

        val success = repository.fetchAndActivate()

        assertTrue(success)
        assertTrue(repository.featureFlags.value.isWidgetActionsEnabled)
        assertTrue(repository.featureFlags.value.isSueLocalAiEnabled)
    }

    @Test
    fun fetchAndActivate_failure_returnsFalse() = runTest {
        val failureTask = mockk<Task<Boolean>>(relaxed = true)
        every { failureTask.isComplete } returns true
        every { failureTask.isSuccessful } returns false
        every { failureTask.exception } returns RuntimeException("Network failure")

        every { remoteConfig.fetchAndActivate() } returns failureTask

        val repository = FirebaseConfigRepositoryImpl(context, remoteConfig)

        val success = repository.fetchAndActivate()

        assertFalse(success)
    }

    @Test
    fun configUpdateListener_handlesUpdateAndError() {
        val listenerSlot = slot<ConfigUpdateListener>()
        every { remoteConfig.addOnConfigUpdateListener(capture(listenerSlot)) } returns mockk()

        val repository = FirebaseConfigRepositoryImpl(context, remoteConfig)

        assertTrue(listenerSlot.isCaptured)

        // Trigger onUpdate
        val mockUpdate = mockk<ConfigUpdate>(relaxed = true)
        every { mockUpdate.updatedKeys } returns setOf(FirebaseConfigRepositoryImpl.KEY_ENABLE_SUE_LOCAL_AI)
        listenerSlot.captured.onUpdate(mockUpdate)

        // Trigger onError
        val mockException = mockk<FirebaseRemoteConfigException>(relaxed = true)
        listenerSlot.captured.onError(mockException)

        assertNotNull(repository.featureFlags.value)
    }
}
