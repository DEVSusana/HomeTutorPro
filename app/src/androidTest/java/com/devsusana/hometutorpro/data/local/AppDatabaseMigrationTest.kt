package com.devsusana.hometutorpro.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented integration tests verifying [AppDatabase] integrity and schema instantiation.
 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    /** Room migration test helper. */
    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    /**
     * Verifies that the Room database schema instantiates correctly and all defined DAOs
     * are accessible and functional in an in-memory instance without crashes.
     */
    @Test
    fun databaseSchema_instantiatesCorrectly() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        try {
            // Verify DAOs are successfully instantiated
            assertNotNull(database.studentDao())
            assertNotNull(database.scheduleDao())
            assertNotNull(database.scheduleExceptionDao())
            assertNotNull(database.resourceDao())
            assertNotNull(database.sharedResourceDao())
            assertNotNull(database.syncMetadataDao())
        } finally {
            database.close()
        }
    }
}
