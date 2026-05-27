package com.devsusana.hometutorpro.data.local

import androidx.room.*
import androidx.room.TypeConverters
import com.devsusana.hometutorpro.data.local.converters.TypeConverters as AppTypeConverters
import com.devsusana.hometutorpro.data.local.dao.*
import com.devsusana.hometutorpro.data.local.entities.*
/**
 * Room Database for Free flavor.
 * Provides local storage for all app data.
 */
@Database(
    entities = [
        StudentEntity::class,
        ScheduleEntity::class,
        ScheduleExceptionEntity::class,
        ResourceEntity::class,
        SharedResourceEntity::class,
        SyncMetadataEntity::class
    ],
    version = 9,
    exportSchema = false
)
@TypeConverters(AppTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun studentDao(): StudentDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun scheduleExceptionDao(): ScheduleExceptionDao
    abstract fun resourceDao(): ResourceDao
    abstract fun sharedResourceDao(): SharedResourceDao
    abstract fun syncMetadataDao(): SyncMetadataDao
    abstract fun agentContextDao(): AgentContextDao
}
