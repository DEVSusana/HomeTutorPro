package com.devsusana.hometutorpro.core.auth

import android.content.Context
import android.util.Base64
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
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject
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
        private const val CHALLENGE_STRING = "HomeTutorProRestoreCredentialAuth"
    }

    private fun getBase64Url(input: ByteArray): String {
        return Base64.encodeToString(input, Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING)
    }

    private fun buildCreateRestoreCredentialJson(
        userId: String,
        email: String,
        displayName: String
    ): String {
        val userRawData = "$userId|$email|$displayName"
        val base64UserId = getBase64Url(userRawData.toByteArray(Charsets.UTF_8))
        val base64Challenge = getBase64Url(CHALLENGE_STRING.toByteArray(Charsets.UTF_8))
        val rpId = context.packageName

        val jsonObject = JSONObject().apply {
            put("challenge", base64Challenge)
            put("rp", JSONObject().apply {
                put("id", rpId)
                put("name", "HomeTutorPro")
            })
            put("user", JSONObject().apply {
                put("id", base64UserId)
                put("name", email)
                put("displayName", displayName)
            })
            put("pubKeyCredParams", JSONArray().apply {
                put(JSONObject().apply {
                    put("type", "public-key")
                    put("alg", -7) // ES256
                })
                put(JSONObject().apply {
                    put("type", "public-key")
                    put("alg", -257) // RS256
                })
            })
            put("authenticatorSelection", JSONObject().apply {
                put("authenticatorAttachment", "platform")
                put("requireResidentKey", true)
                put("residentKey", "required")
                put("userVerification", "preferred")
            })
            put("timeout", 60000)
            put("attestation", "none")
        }
        return jsonObject.toString()
    }

    private fun buildGetRestoreCredentialJson(): String {
        val base64Challenge = getBase64Url(CHALLENGE_STRING.toByteArray(Charsets.UTF_8))
        val rpId = context.packageName

        val jsonObject = JSONObject().apply {
            put("challenge", base64Challenge)
            put("rpId", rpId)
            put("userVerification", "preferred")
            put("timeout", 60000)
        }
        return jsonObject.toString()
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
        val userEmail = email ?: ""
        val userName = displayName ?: ""
        val requestJson = buildCreateRestoreCredentialJson(userId, userEmail, userName)

        // Try with Cloud Backup first
        try {
            val request = CreateRestoreCredentialRequest(
                requestJson = requestJson,
                isCloudBackupEnabled = true
            )
            credentialManager.createCredential(context, request)
            Log.d(TAG, "Restore credential created successfully with cloud backup for $userEmail")
            return true
        } catch (e: Exception) {
            Log.w(TAG, "Failed creating restore credential with cloud backup: ${e.message}. Retrying with local D2D backup...")
        }

        // Fallback to local D2D backup if Cloud Backup fails (e.g. debug build or device without E2EE)
        return try {
            val fallbackRequest = CreateRestoreCredentialRequest(
                requestJson = requestJson,
                isCloudBackupEnabled = false
            )
            credentialManager.createCredential(context, fallbackRequest)
            Log.d(TAG, "Restore credential created successfully (local D2D backup) for $userEmail")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create fallback restore credential: ${e.message}", e)
            false
        }
    }

    /**
     * Attempts silent retrieval of restore credential on device startup.
     */
    override suspend fun getRestoreCredential(): RestorePayload? {
        // 1. Try official standard GetRestoreCredentialOption with "{}"
        try {
            val restoreOption = GetRestoreCredentialOption(requestJson = "{}")
            val getRequest = GetCredentialRequest.Builder()
                .addCredentialOption(restoreOption)
                .build()

            val response = credentialManager.getCredential(context, getRequest)
            val credential = response.credential

            if (credential is RestoreCredential) {
                val authJson = credential.authenticationResponseJson
                if (authJson.isNotBlank()) {
                    val payload = parseRestorePayload(authJson)
                    if (payload != null) return payload
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Standard restore option with '{}' returned: ${e.message}")
        }

        // 2. Fallback: try with explicit WebAuthn get json
        return try {
            val requestJson = buildGetRestoreCredentialJson()
            val restoreOption = GetRestoreCredentialOption(requestJson = requestJson)
            val getRequest = GetCredentialRequest.Builder()
                .addCredentialOption(restoreOption)
                .build()

            val response = credentialManager.getCredential(context, getRequest)
            val credential = response.credential

            if (credential is RestoreCredential) {
                val authJson = credential.authenticationResponseJson
                if (authJson.isNotBlank()) {
                    parseRestorePayload(authJson)
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

    private fun parseRestorePayload(authJson: String): RestorePayload? {
        return try {
            val jsonObject = JSONObject(authJson)
            // 1. Check WebAuthn response structure
            val responseObj = jsonObject.optJSONObject("response")
            val userHandleBase64 = responseObj?.optString("userHandle")
            if (!userHandleBase64.isNullOrBlank()) {
                val decoded = String(Base64.decode(userHandleBase64, Base64.NO_WRAP or Base64.URL_SAFE), Charsets.UTF_8)
                val parts = decoded.split("|")
                if (parts.isNotEmpty()) {
                    val uid = parts[0]
                    val email = parts.getOrNull(1) ?: ""
                    val name = parts.getOrNull(2) ?: ""
                    Log.d(TAG, "Restore credential decoded from userHandle: $email")
                    return RestorePayload(
                        user = RestoreUser(id = uid, name = email, displayName = name)
                    )
                }
            }

            // 2. Direct RestorePayload format fallback
            val payload = json.decodeFromString<RestorePayload>(authJson)
            Log.d(TAG, "Restore credential parsed from raw payload: ${payload.email}")
            payload
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing restore payload: ${e.message}")
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
