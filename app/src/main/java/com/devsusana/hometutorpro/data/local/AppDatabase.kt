package com.devsusana.hometutorpro.data.local

import androidx.room.*
import androidx.room.TypeConverters
import com.devsusana.hometutorpro.data.local.converters.TypeConverters as AppTypeConverters
import com.devsusana.hometutorpro.data.local.dao.*
import com.devsusana.hometutorpro.data.local.entities.*
import android.content.Context

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

    companion object {
        private const val DATABASE_NAME = "hometutorpro.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val factory = SupportFactoryHelper.createFactory(context)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .openHelperFactory(factory)
                    .addMigrations(
                        com.devsusana.hometutorpro.data.local.migrations.DatabaseMigrations.MIGRATION_4_5,
                        com.devsusana.hometutorpro.data.local.migrations.DatabaseMigrations.MIGRATION_5_6,
                        com.devsusana.hometutorpro.data.local.migrations.DatabaseMigrations.MIGRATION_6_7,
                        com.devsusana.hometutorpro.data.local.migrations.DatabaseMigrations.MIGRATION_7_8,
                        com.devsusana.hometutorpro.data.local.migrations.DatabaseMigrations.MIGRATION_8_9
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
