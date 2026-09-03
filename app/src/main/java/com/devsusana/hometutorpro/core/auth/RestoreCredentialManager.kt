package com.devsusana.hometutorpro.core.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CreateRestoreCredentialRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetRestoreCredentialOption
import androidx.credentials.RestoreCredential
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User display info conforming to WebAuthn schema required by Android Restore Credentials API.
 */
@Serializable
data class RestoreUser(
    val id: String,
    val name: String,
    val displayName: String
)

/**
 * Data payload stored and transferred during device-to-device migration or cloud restore.
 */
@Serializable
data class RestorePayload(
    val user: RestoreUser,
    val token: String = ""
) {
    val userId: String get() = user.id
    val email: String get() = user.name
    val displayName: String get() = user.displayName
}

/**
 * Interface contract for managing Android Zero-Tap Restore Credentials.
 */
interface IRestoreCredentialManager {
    suspend fun saveRestoreCredential(userId: String, email: String?, displayName: String?, token: String = ""): Boolean
    suspend fun getRestoreCredential(): RestorePayload?
    suspend fun clearRestoreCredential(): Boolean
}

/**
 * Implementation of [IRestoreCredentialManager] utilizing AndroidX CredentialManager's
 * Restore Credentials API to achieve Zero-Tap Sign-In on new or migrated devices.
 */
@Singleton
class RestoreCredentialManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val credentialManager: CredentialManager
) : IRestoreCredentialManager {

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val TAG = "RestoreCredentialMgr"
    }

    /**
     * Saves a restore credential linked to user's Google Cloud Backup or device transfer.
     */
    override suspend fun saveRestoreCredential(
        userId: String,
        email: String?,
        displayName: String?,
        token: String
    ): Boolean {
        return try {
            val userEmail = email ?: ""
            val userName = displayName ?: ""
            val payload = RestorePayload(
                user = RestoreUser(
                    id = userId,
                    name = userEmail,
                    displayName = userName
                ),
                token = token
            )
            val payloadJson = json.encodeToString(payload)

            val request = CreateRestoreCredentialRequest(
                requestJson = payloadJson,
                isCloudBackupEnabled = true
            )

            credentialManager.createCredential(context, request)
            Log.d(TAG, "Restore credential created successfully for user $userEmail")
            true
        } catch (e: CreateCredentialException) {
            Log.w(TAG, "Failed to create restore credential: ${e.message}", e)
            false
        } catch (e: Exception) {
            Log.w(TAG, "Unexpected error creating restore credential: ${e.message}", e)
            false
        }
    }

    /**
     * Attempts silent retrieval of restore credential on device startup.
     */
    override suspend fun getRestoreCredential(): RestorePayload? {
        return try {
            val restoreOption = GetRestoreCredentialOption(requestJson = "{}")
            val getRequest = GetCredentialRequest.Builder()
                .addCredentialOption(restoreOption)
                .build()

            val response = credentialManager.getCredential(context, getRequest)
            val credential = response.credential

            if (credential is RestoreCredential) {
                val authJson = credential.authenticationResponseJson
                if (authJson.isNotBlank()) {
                    val payload = json.decodeFromString<RestorePayload>(authJson)
                    Log.d(TAG, "Restore credential retrieved successfully for ${payload.email}")
                    payload
                } else {
                    null
                }
            } else {
                Log.d(TAG, "No RestoreCredential returned by CredentialManager")
                null
            }
        } catch (e: GetCredentialException) {
            Log.d(TAG, "No restore credential available or error fetching: ${e.message}")
            null
        } catch (e: Exception) {
            Log.w(TAG, "Unexpected error retrieving restore credential: ${e.message}", e)
            null
        }
    }

    /**
     * Clears restore credentials upon user sign out to prevent unauthorized access.
     */
    override suspend fun clearRestoreCredential(): Boolean {
        return try {
            val request = ClearCredentialStateRequest()
            credentialManager.clearCredentialState(request)
            Log.d(TAG, "Restore credentials cleared successfully")
            true
        } catch (e: ClearCredentialException) {
            Log.w(TAG, "Failed to clear restore credential: ${e.message}", e)
            false
        } catch (e: Exception) {
            Log.w(TAG, "Unexpected error clearing restore credential: ${e.message}", e)
            false
        }
    }
}
