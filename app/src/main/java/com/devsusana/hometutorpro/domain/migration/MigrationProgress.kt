package com.devsusana.hometutorpro.domain.migration

/**
 * Represents the progress state of the data migration process.
 */
sealed class MigrationProgress {
    object Idle : MigrationProgress()
    object Started : MigrationProgress()
    object RegisteringUser : MigrationProgress()
    object MigratingStudents : MigrationProgress()
    object MigratingSchedules : MigrationProgress()
    object MigratingResources : MigrationProgress()
    object CleaningUp : MigrationProgress()
    object Completed : MigrationProgress()
    data class Error(val message: String) : MigrationProgress()
}
