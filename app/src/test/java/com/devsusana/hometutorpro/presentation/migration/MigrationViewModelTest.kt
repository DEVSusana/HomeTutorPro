package com.devsusana.hometutorpro.presentation.migration

import com.devsusana.hometutorpro.domain.migration.MigrationProgress
import com.devsusana.hometutorpro.domain.usecases.IMigrateDataFromLocalUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [MigrationViewModel].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MigrationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val migrateDataFromLocalUseCase: IMigrateDataFromLocalUseCase = mockk()
    private lateinit var viewModel: MigrationViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = MigrationViewModel(migrateDataFromLocalUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Idle`() {
        assertEquals(MigrationProgress.Idle, viewModel.migrationState.value)
    }

    @Test
    fun `startMigration collects progress updates from use case`() = runTest {
        val progresses = listOf(
            MigrationProgress.Started,
            MigrationProgress.MigratingStudents,
            MigrationProgress.Completed
        )
        every { migrateDataFromLocalUseCase() } returns flowOf(*progresses.toTypedArray())

        viewModel.startMigration()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(MigrationProgress.Completed, viewModel.migrationState.value)
        verify(exactly = 1) { migrateDataFromLocalUseCase() }
    }

    @Test
    fun `resetState resets migrationState to Idle`() = runTest {
        every { migrateDataFromLocalUseCase() } returns flowOf(MigrationProgress.Completed)
        viewModel.startMigration()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(MigrationProgress.Completed, viewModel.migrationState.value)

        viewModel.resetState()

        assertEquals(MigrationProgress.Idle, viewModel.migrationState.value)
    }
}
