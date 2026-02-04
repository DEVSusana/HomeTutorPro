package com.devsusana.hometutorpro.data.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Unit tests for SyncScheduler.
 * Uses MockK to mock WorkManager and avoid database creation/leaks.
 */
@RunWith(AndroidJUnit4::class)
class SyncSchedulerTest {

    private lateinit var context: Context
    private lateinit var syncScheduler: SyncScheduler
    private lateinit var mockWorkManager: WorkManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        mockWorkManager = mockk(relaxed = true)
        syncScheduler = SyncScheduler(context, mockWorkManager)
    }

    @After
    fun tearDown() {
        // No static unmocking needed
    }

    @Test
    fun scheduleSyncNow_enqueuesImmediateSyncWork() = runTest {
        // When: Schedule immediate sync
        syncScheduler.scheduleSyncNow()

        // Then: Work should be enqueued
        verify { 
            mockWorkManager.enqueueUniqueWork(
                "immediate_sync",
                ExistingWorkPolicy.REPLACE,
                any<OneTimeWorkRequest>()
            )
        }
    }

    @Test
    fun schedulePeriodicSync_enqueuesPeriodicSyncWork() = runTest {
        // When: Schedule periodic sync
        syncScheduler.schedulePeriodicSync()

        // Then: Periodic work should be enqueued
        verify { 
            mockWorkManager.enqueueUniquePeriodicWork(
                "periodic_sync",
                ExistingPeriodicWorkPolicy.KEEP,
                any<PeriodicWorkRequest>()
            )
        }
    }

    @Test
    fun cancelAllSync_cancelsAllSyncWork() = runTest {
        // When: Cancel all sync
        syncScheduler.cancelAllSync()

        // Then: All sync work should be cancelled
        verify { mockWorkManager.cancelUniqueWork("immediate_sync") }
        verify { mockWorkManager.cancelUniqueWork("periodic_sync") }
    }

    @Test
    fun scheduleSyncNow_multipleTimes_replacesExistingWork() = runTest {
        // When: Schedule immediate sync multiple times
        syncScheduler.scheduleSyncNow()
        syncScheduler.scheduleSyncNow()
        syncScheduler.scheduleSyncNow()

        // Then: Should call enqueueUniqueWork multiple times with REPLACE
        verify(exactly = 3) { 
            mockWorkManager.enqueueUniqueWork(
                "immediate_sync",
                ExistingWorkPolicy.REPLACE,
                any<OneTimeWorkRequest>()
            )
        }
    }

    @Test
    fun schedulePeriodicSync_multipleTimes_keepsExistingWork() = runTest {
        // When: Schedule periodic sync multiple times
        syncScheduler.schedulePeriodicSync()
        syncScheduler.schedulePeriodicSync()
        syncScheduler.schedulePeriodicSync()

        // Then: Should call enqueueUniquePeriodicWork multiple times with KEEP
        verify(exactly = 3) { 
            mockWorkManager.enqueueUniquePeriodicWork(
                "periodic_sync",
                ExistingPeriodicWorkPolicy.KEEP,
                any<PeriodicWorkRequest>()
            )
        }
    }
}
