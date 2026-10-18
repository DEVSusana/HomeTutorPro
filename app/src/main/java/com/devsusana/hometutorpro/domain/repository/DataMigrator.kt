package com.devsusana.hometutorpro.domain.repository

/**
 * Contract for migrating locally persisted data to cloud storage.
 */
interface DataMigrator {
    suspend fun migrateStudents()
    suspend fun migrateResources()
}
