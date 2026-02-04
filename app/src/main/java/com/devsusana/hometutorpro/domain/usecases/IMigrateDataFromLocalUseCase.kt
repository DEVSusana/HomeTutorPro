package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.migration.MigrationProgress
import kotlinx.coroutines.flow.Flow

interface IMigrateDataFromLocalUseCase {
    operator fun invoke(): Flow<MigrationProgress>
}
