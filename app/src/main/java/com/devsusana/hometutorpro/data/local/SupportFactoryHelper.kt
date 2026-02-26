@file:Suppress("DEPRECATION")

package com.devsusana.hometutorpro.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.sqlite.db.SupportSQLiteOpenHelper
import net.zetetic.database.sqlcipher.SQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File
import java.io.IOException
import java.util.UUID

/**
 * Helper for encrypted database support.
 */
object SupportFactoryHelper {
    private const val PASSPHRASE_KEY = "db_passphrase"
    private const val PREFS_NAME = "db_security_prefs"
    private const val DATABASE_NAME = "hometutorpro.db"

    fun createFactory(context: Context): SupportSQLiteOpenHelper.Factory {
        System.loadLibrary("sqlcipher")

        val passphraseString = getOrCreatePassphrase(context)
        migrateIfNecessary(context, passphraseString)
        return SupportOpenHelperFactory(passphraseString.toByteArray())
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

    @Suppress("DEPRECATION")
    private fun getOrCreatePassphrase(context: Context): String {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
            
        val sharedPreferences = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        var passphrase = sharedPreferences.getString(PASSPHRASE_KEY, null)
        if (passphrase == null) {
            passphrase = UUID.randomUUID().toString()
            sharedPreferences.edit().putString(PASSPHRASE_KEY, passphrase).apply()
        }
        return passphrase
    }
}
