@file:Suppress("DEPRECATION")

package com.devsusana.hometutorpro.data.local

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.devsusana.hometutorpro.core.utils.SafeLogger
import net.zetetic.database.sqlcipher.SQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File
import java.io.IOException
import java.util.UUID

/**
 * Helper for encrypted database support.
 */
object SupportFactoryHelper {
    private const val TAG = "SupportFactoryHelper"
    private const val PASSPHRASE_KEY = "db_passphrase"
    private const val PREFS_NAME = "db_security_prefs"
    private const val DATABASE_NAME = "hometutorpro.db"

    fun createFactory(context: Context): SupportSQLiteOpenHelper.Factory {
        System.loadLibrary("sqlcipher")

        val passphraseString = getOrCreatePassphrase(context)
        verifyOrResetCorruptedDb(context, passphraseString)
        migrateIfNecessary(context, passphraseString)
        return SupportOpenHelperFactory(passphraseString.toByteArray())
    }

    private fun verifyOrResetCorruptedDb(context: Context, passphraseString: String) {
        val dbFile = context.getDatabasePath(DATABASE_NAME)
        if (!dbFile.exists()) return

        var isValid = false
        try {
            val factory = SupportOpenHelperFactory(passphraseString.toByteArray())
            val config = SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(DATABASE_NAME)
                .callback(object : SupportSQLiteOpenHelper.Callback(11) {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {}
                    override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                    override fun onDowngrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                        // No-op during helper verification to avoid false corruption detection
                    }
                })
                .build()
            factory.create(config).use { helper ->
                helper.readableDatabase.query("SELECT count(*) FROM sqlite_master;").use { cursor ->
                    cursor.moveToFirst()
                }
                isValid = true
            }
        } catch (_: Exception) {
            isValid = false
        }

        if (!isValid) {
            try {
                SQLiteDatabase.openDatabase(
                    dbFile.absolutePath,
                    null,
                    SQLiteDatabase.OPEN_READONLY
                ).use { db ->
                    db.rawQuery("SELECT count(*) FROM sqlite_master;", null).use { cursor ->
                        cursor.moveToFirst()
                    }
                    isValid = true
                }
            } catch (_: Exception) {
                isValid = false
            }
        }

        if (!isValid) {
            SafeLogger.e(TAG, "Database file exists but cannot be decrypted with current passphrase. Resetting database file...")
            listOf("", "-journal", "-shm", "-wal").forEach { suffix ->
                val file = context.getDatabasePath("$DATABASE_NAME$suffix")
                if (file.exists()) file.delete()
            }
        }
    }

    private fun migrateIfNecessary(context: Context, passphraseString: String) {
        val dbFile = context.getDatabasePath(DATABASE_NAME)
        if (!dbFile.exists()) return

        var isUnencrypted = false
        try {
            SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            ).use { db ->
                db.version
                isUnencrypted = true
            }
        } catch (e: Exception) {
            // Likely already encrypted or corrupted
        }

        if (isUnencrypted) {
            encryptDatabase(context, dbFile, passphraseString)
        }
    }

    private fun encryptDatabase(context: Context, dbFile: File, passphraseString: String) {
        val encryptedFile = context.getDatabasePath("$DATABASE_NAME.tmp")
        if (encryptedFile.exists()) encryptedFile.delete()

        dbFile.parentFile?.mkdirs()
        encryptedFile.createNewFile()

        try {
            SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READWRITE
            ).use { unencryptedDb ->
                val escapedPassword = passphraseString.replace("'", "''")
                unencryptedDb.rawExecSQL("ATTACH DATABASE '${encryptedFile.absolutePath}' AS encrypted KEY '$escapedPassword';")
                unencryptedDb.rawExecSQL("SELECT sqlcipher_export('encrypted');")
                unencryptedDb.rawExecSQL("DETACH DATABASE encrypted;")
            }

            listOf("-journal", "-shm", "-wal").forEach { suffix ->
                val file = context.getDatabasePath("$DATABASE_NAME$suffix")
                if (file.exists()) file.delete()
            }

            if (dbFile.delete()) {
                if (!encryptedFile.renameTo(dbFile)) {
                    throw IOException("Failed to rename encrypted database to ${dbFile.absolutePath}")
                }
            } else {
                throw IOException("Failed to delete old unencrypted database at ${dbFile.absolutePath}")
            }
        } catch (e: Exception) {
            if (encryptedFile.exists()) encryptedFile.delete()
            throw e
        }
    }

    private fun getOrCreatePassphrase(context: Context): String {
        val sharedPreferences = getEncryptedPreferences(context)
        var passphrase = try {
            sharedPreferences.getString(PASSPHRASE_KEY, null)
        } catch (e: Exception) {
            SafeLogger.e(TAG, "Failed reading passphrase from EncryptedSharedPreferences: ${e.message}. Recreating...", e)
            null
        }

        if (passphrase == null) {
            passphrase = UUID.randomUUID().toString()
            try {
                sharedPreferences.edit().putString(PASSPHRASE_KEY, passphrase).apply()
            } catch (e: Exception) {
                SafeLogger.e(TAG, "Failed saving passphrase to EncryptedSharedPreferences: ${e.message}", e)
            }
        }
        return passphrase
    }

    private fun getEncryptedPreferences(context: Context): SharedPreferences {
        return try {
            createEncryptedPrefs(context)
        } catch (e: Exception) {
            SafeLogger.e(TAG, "EncryptedSharedPreferences creation failed (${e.message}). Resetting corrupted preferences...", e)
            deletePreferencesFile(context, PREFS_NAME)
            try {
                createEncryptedPrefs(context)
            } catch (e2: Exception) {
                SafeLogger.e(TAG, "Fallback to standard preferences after Keystore failure: ${e2.message}", e2)
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            }
        }
    }

    private fun createEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun deletePreferencesFile(context: Context, prefsName: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.deleteSharedPreferences(prefsName)
            } else {
                context.getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit().clear().commit()
                val prefsFile = File(context.applicationInfo.dataDir, "shared_prefs/$prefsName.xml")
                if (prefsFile.exists()) {
                    prefsFile.delete()
                }
            }
        } catch (e: Exception) {
            SafeLogger.e(TAG, "Failed to delete preferences file $prefsName: ${e.message}", e)
        }
    }
}
