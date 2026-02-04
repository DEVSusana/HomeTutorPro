package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.data.migration.LocalDataMigrator
import com.devsusana.hometutorpro.domain.migration.MigrationProgress
import com.devsusana.hometutorpro.domain.usecases.IMigrateDataFromLocalUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Use case to execute the data migration process.
 * Emits progress updates as it proceeds through migration steps.
 */
class MigrateDataFromLocalUseCase @Inject constructor(
    private val localDataMigrator: LocalDataMigrator
) : IMigrateDataFromLocalUseCase {
    override operator fun invoke(): Flow<MigrationProgress> = flow {
        emit(MigrationProgress.Started)
        
        try {
            // 1. Migrate students (and their schedules/exceptions)
            emit(MigrationProgress.MigratingStudents)
            localDataMigrator.migrateStudents()
            
            // 2. Migrate resources
            emit(MigrationProgress.MigratingResources)
            localDataMigrator.migrateResources()
            
            // 3. Cleanup or final steps
            emit(MigrationProgress.CleaningUp)
            // localDataMigrator.cleanup() // If needed
            
            emit(MigrationProgress.Completed)
        } catch (e: Exception) {
            emit(MigrationProgress.Error(e.message ?: "Unknown error during migration"))
        }
    }
}
