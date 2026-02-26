package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.migration.MigrationProgress
import kotlinx.coroutines.flow.Flow

/**
 * Use case contract for MigrateDataFromLocal operations.
 */
interface IMigrateDataFromLocalUseCase {
    /**
     * Executes the use case.
     */
    operator fun invoke(): Flow<MigrationProgress>
}
