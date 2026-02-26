package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.data.migration.LocalDataMigrator
import com.devsusana.hometutorpro.domain.migration.MigrationProgress
import com.devsusana.hometutorpro.domain.usecases.implementations.MigrateDataFromLocalUseCase
import com.devsusana.hometutorpro.domain.usecases.implementations.RescueOrphanedDataUseCase
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MigrateDataFromLocalUseCaseTest {

    private val localDataMigrator: LocalDataMigrator = mockk()
    private val rescueOrphanedDataUseCase: RescueOrphanedDataUseCase = mockk()
    private val useCase = MigrateDataFromLocalUseCase(localDataMigrator, rescueOrphanedDataUseCase)

    @Test
    fun `invoke should emit progress and complete when migration succeeds`() = runTest {
        coEvery { rescueOrphanedDataUseCase.invoke() } returns Unit
        coEvery { localDataMigrator.migrateStudents() } returns Unit
        coEvery { localDataMigrator.migrateResources() } returns Unit

        val emissions = useCase().toList()

        assertEquals(
            listOf(
                MigrationProgress.Started,
                MigrationProgress.MigratingStudents,
                MigrationProgress.MigratingResources,
                MigrationProgress.CleaningUp,
                MigrationProgress.Completed
            ),
            emissions
        )

        coVerifyOrder {
            rescueOrphanedDataUseCase.invoke()
            localDataMigrator.migrateStudents()
            localDataMigrator.migrateResources()
        }
    }

    @Test
    fun `invoke should emit error when migration fails`() = runTest {
        coEvery { rescueOrphanedDataUseCase.invoke() } returns Unit
        coEvery { localDataMigrator.migrateStudents() } throws IllegalStateException("boom")

        val emissions = useCase().toList()

        assertEquals(3, emissions.size)
        assertTrue(emissions[0] is MigrationProgress.Started)
        assertTrue(emissions[1] is MigrationProgress.MigratingStudents)
        val error = emissions[2] as MigrationProgress.Error
        assertEquals("boom", error.message)
    }
}
