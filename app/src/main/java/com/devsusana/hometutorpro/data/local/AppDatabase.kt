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
    version = 6,
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

    companion object {
        private const val DATABASE_NAME = "hometutorpro.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        private val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Add isCompleted and completedDate columns to schedules table
                database.execSQL("ALTER TABLE schedules ADD COLUMN isCompleted INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE schedules ADD COLUMN completedDate INTEGER")
            }
        }
        
        private val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Add notes column to students table
                database.execSQL("ALTER TABLE students ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val factory = SupportFactoryHelper.createFactory(context)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .openHelperFactory(factory)
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
