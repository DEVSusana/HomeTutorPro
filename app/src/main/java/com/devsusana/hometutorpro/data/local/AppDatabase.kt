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

        private val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // 1. Handle missing columns in students (if any, safely)
                val cursor = database.query("PRAGMA table_info(students)")
                val columns = mutableListOf<String>()
                while (cursor.moveToNext()) {
                    val nameIndex = cursor.getColumnIndex("name")
                    if (nameIndex != -1) {
                        columns.add(cursor.getString(nameIndex))
                    }
                }
                cursor.close()

                if (!columns.contains("studentEmail")) {
                    database.execSQL("ALTER TABLE students ADD COLUMN studentEmail TEXT")
                }
                if (!columns.contains("color")) {
                    database.execSQL("ALTER TABLE students ADD COLUMN color INTEGER")
                }
                if (!columns.contains("isActive")) {
                    database.execSQL("ALTER TABLE students ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1")
                }
                if (!columns.contains("lastClassDate")) {
                    database.execSQL("ALTER TABLE students ADD COLUMN lastClassDate INTEGER")
                }

                // 2. Migrate schedules (TEXT -> INTEGER for dayOfWeek)
                database.execSQL("ALTER TABLE schedules RENAME TO schedules_old")
                database.execSQL("""
                    CREATE TABLE schedules (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        cloudId TEXT,
                        studentId INTEGER NOT NULL,
                        dayOfWeek INTEGER NOT NULL,
                        startTime TEXT NOT NULL,
                        endTime TEXT NOT NULL,
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        completedDate INTEGER,
                        syncStatus TEXT NOT NULL,
                        lastModifiedTimestamp INTEGER NOT NULL,
                        pendingDelete INTEGER NOT NULL,
                        FOREIGN KEY(studentId) REFERENCES students(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                
                database.execSQL("""
                    INSERT INTO schedules (id, cloudId, studentId, dayOfWeek, startTime, endTime, isCompleted, completedDate, syncStatus, lastModifiedTimestamp, pendingDelete)
                    SELECT id, cloudId, studentId, 
                    CASE dayOfWeek 
                        WHEN 'MONDAY' THEN 1 
                        WHEN 'TUESDAY' THEN 2 
                        WHEN 'WEDNESDAY' THEN 3 
                        WHEN 'THURSDAY' THEN 4 
                        WHEN 'FRIDAY' THEN 5 
                        WHEN 'SATURDAY' THEN 6 
                        WHEN 'SUNDAY' THEN 7 
                        ELSE 1 
                    END, 
                    startTime, endTime, isCompleted, completedDate, syncStatus, lastModifiedTimestamp, pendingDelete 
                    FROM schedules_old
                """.trimIndent())
                database.execSQL("DROP TABLE schedules_old")
                database.execSQL("CREATE INDEX index_schedules_studentId ON schedules(studentId)")
                database.execSQL("CREATE INDEX index_schedules_cloudId ON schedules(cloudId)")
                database.execSQL("CREATE INDEX index_schedules_syncStatus ON schedules(syncStatus)")

                // 3. Migrate schedule_exceptions (TEXT -> INTEGER for newDayOfWeek)
                database.execSQL("ALTER TABLE schedule_exceptions RENAME TO schedule_exceptions_old")
                database.execSQL("""
                    CREATE TABLE schedule_exceptions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        cloudId TEXT,
                        studentId INTEGER NOT NULL,
                        originalScheduleId TEXT NOT NULL,
                        exceptionDate INTEGER NOT NULL,
                        reason TEXT NOT NULL,
                        isCancelled INTEGER NOT NULL,
                        newStartTime TEXT,
                        newEndTime TEXT,
                        newDayOfWeek INTEGER,
                        syncStatus TEXT NOT NULL,
                        lastModifiedTimestamp INTEGER NOT NULL,
                        pendingDelete INTEGER NOT NULL,
                        FOREIGN KEY(studentId) REFERENCES students(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                
                database.execSQL("""
                    INSERT INTO schedule_exceptions (id, cloudId, studentId, originalScheduleId, exceptionDate, reason, isCancelled, newStartTime, newEndTime, newDayOfWeek, syncStatus, lastModifiedTimestamp, pendingDelete)
                    SELECT id, cloudId, studentId, originalScheduleId, exceptionDate, reason, isCancelled, newStartTime, newEndTime,
                    CASE newDayOfWeek 
                        WHEN 'MONDAY' THEN 1 
                        WHEN 'TUESDAY' THEN 2 
                        WHEN 'WEDNESDAY' THEN 3 
                        WHEN 'THURSDAY' THEN 4 
                        WHEN 'FRIDAY' THEN 5 
                        WHEN 'SATURDAY' THEN 6 
                        WHEN 'SUNDAY' THEN 7 
                        ELSE NULL 
                    END, 
                    syncStatus, lastModifiedTimestamp, pendingDelete 
                    FROM schedule_exceptions_old
                """.trimIndent())
                database.execSQL("DROP TABLE schedule_exceptions_old")
                database.execSQL("CREATE INDEX index_schedule_exceptions_studentId ON schedule_exceptions(studentId)")
                database.execSQL("CREATE INDEX index_schedule_exceptions_exceptionDate ON schedule_exceptions(exceptionDate)")
                database.execSQL("CREATE INDEX index_schedule_exceptions_cloudId ON schedule_exceptions(cloudId)")
                database.execSQL("CREATE INDEX index_schedule_exceptions_syncStatus ON schedule_exceptions(syncStatus)")
            }
        }

        private val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Add professorId to all relevant tables for multi-user isolation
                database.execSQL("ALTER TABLE students ADD COLUMN professorId TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE schedules ADD COLUMN professorId TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE schedule_exceptions ADD COLUMN professorId TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE resources ADD COLUMN professorId TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE shared_resources ADD COLUMN professorId TEXT NOT NULL DEFAULT ''")
                
                // Add indices for professorId for query performance
                database.execSQL("CREATE INDEX index_students_professorId ON students(professorId)")
                database.execSQL("CREATE INDEX index_schedules_professorId ON schedules(professorId)")
                database.execSQL("CREATE INDEX index_schedule_exceptions_professorId ON schedule_exceptions(professorId)")
                database.execSQL("CREATE INDEX index_resources_professorId ON resources(professorId)")
                database.execSQL("CREATE INDEX index_shared_resources_professorId ON shared_resources(professorId)")
            }
        }

        private val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // 1. Rename old table
                database.execSQL("ALTER TABLE schedule_exceptions RENAME TO schedule_exceptions_old")

                // 2. Create new table (type field instead of isCancelled, and unique index)
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `schedule_exceptions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `professorId` TEXT NOT NULL, 
                        `cloudId` TEXT, 
                        `studentId` INTEGER NOT NULL, 
                        `originalScheduleId` TEXT NOT NULL, 
                        `exceptionDate` INTEGER NOT NULL, 
                        `reason` TEXT NOT NULL, 
                        `type` TEXT NOT NULL, 
                        `newStartTime` TEXT, 
                        `newEndTime` TEXT, 
                        `newDayOfWeek` INTEGER, 
                        `syncStatus` TEXT NOT NULL, 
                        `lastModifiedTimestamp` INTEGER NOT NULL, 
                        `pendingDelete` INTEGER NOT NULL, 
                        FOREIGN KEY(`studentId`) REFERENCES `students`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                """.trimIndent())

                // 3. Insert data (deduplicating during insert using GROUP BY)
                // Map isCancelled to type: 1 -> CANCELLED, 0 -> RESCHEDULED (or EXTRA if originalScheduleId is 'EXTRA')
                database.execSQL("""
                    INSERT INTO schedule_exceptions (
                        id, professorId, cloudId, studentId, originalScheduleId, exceptionDate, 
                        reason, type, newStartTime, newEndTime, newDayOfWeek, 
                        syncStatus, lastModifiedTimestamp, pendingDelete
                    )
                    SELECT 
                        MAX(id), professorId, MAX(cloudId), studentId, originalScheduleId, exceptionDate, 
                        MAX(reason), 
                        CASE 
                            WHEN isCancelled = 1 THEN 'CANCELLED' 
                            WHEN originalScheduleId = 'EXTRA' THEN 'EXTRA' 
                            ELSE 'RESCHEDULED' 
                        END, 
                        MAX(newStartTime), MAX(newEndTime), MAX(newDayOfWeek), 
                        MAX(syncStatus), MAX(lastModifiedTimestamp), MAX(pendingDelete)
                    FROM schedule_exceptions_old
                    GROUP BY professorId, originalScheduleId, exceptionDate
                """.trimIndent())

                // 4. Drop old table
                database.execSQL("DROP TABLE schedule_exceptions_old")

                // 5. Re-create indices (including the new unique one)
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_schedule_exceptions_studentId` ON `schedule_exceptions` (`studentId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_schedule_exceptions_exceptionDate` ON `schedule_exceptions` (`exceptionDate`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_schedule_exceptions_cloudId` ON `schedule_exceptions` (`cloudId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_schedule_exceptions_syncStatus` ON `schedule_exceptions` (`syncStatus`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_schedule_exceptions_professorId` ON `schedule_exceptions` (`professorId`)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_schedule_exceptions_professorId_originalScheduleId_exceptionDate` ON `schedule_exceptions` (`professorId`, `originalScheduleId`, `exceptionDate`)")
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
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
