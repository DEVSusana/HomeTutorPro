package com.devsusana.hometutorpro.data.local

import androidx.room.*
import androidx.room.TypeConverters
import com.devsusana.hometutorpro.data.local.converters.TypeConverters as AppTypeConverters
import com.devsusana.hometutorpro.data.local.dao.*
import com.devsusana.hometutorpro.data.local.entities.*
/**
 * Room Database for the application.
 *
 * Defines the schema with [StudentEntity], [ScheduleEntity], [ScheduleExceptionEntity],
 * [ResourceEntity], [SharedResourceEntity], and [SyncMetadataEntity].
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

    /** @return [StudentDao] interface for interacting with the students table. */
    abstract fun studentDao(): StudentDao

    /** @return [ScheduleDao] interface for interacting with the schedules table. */
    abstract fun scheduleDao(): ScheduleDao

    /** @return [ScheduleExceptionDao] interface for interacting with the schedule exceptions table. */
    abstract fun scheduleExceptionDao(): ScheduleExceptionDao

    /** @return [ResourceDao] interface for interacting with the resources table. */
    abstract fun resourceDao(): ResourceDao

    /** @return [SharedResourceDao] interface for interacting with the shared resources table. */
    abstract fun sharedResourceDao(): SharedResourceDao

    /** @return [SyncMetadataDao] interface for interacting with the sync metadata table. */
    abstract fun syncMetadataDao(): SyncMetadataDao
}
