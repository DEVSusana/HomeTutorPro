package com.devsusana.hometutorpro.presentation.utils

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.devsusana.hometutorpro.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object GoogleSignInHelper {

    fun launchGoogleSignIn(
        context: Context,
        scope: CoroutineScope,
        onSuccess: (String) -> Unit,
        onError: (String?) -> Unit
    ) {
        val activityContext = context.findActivity() ?: context
        val credentialManager = CredentialManager.create(activityContext)
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        val webClientId = if (resId != 0) {
            try {
                context.getString(resId)
            } catch (e: Exception) {
                "133704532651-u1otk4ii5nudebajefff8a01ombu4e75.apps.googleusercontent.com"
            }
        } else {
            "133704532651-u1otk4ii5nudebajefff8a01ombu4e75.apps.googleusercontent.com"
        }

        if (webClientId.isBlank()) {
            Log.e("GoogleSignInHelper", "Default web client ID is blank")
            onError("Web client ID missing")
            return
        }

        // Primary: GetSignInWithGoogleOption specifically designed for explicit button clicks
        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(
            serverClientId = webClientId
        ).build()

        val primaryRequest = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()

        // Fallback: GetGoogleIdOption in case device Play Services requires standard GoogleIdOption
        val fallbackGoogleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()

        val fallbackRequest = GetCredentialRequest.Builder()
            .addCredentialOption(fallbackGoogleIdOption)
            .build()

        scope.launch {
            var retries = 2
            var currentRequest = primaryRequest
            var success = false

            while (retries >= 0 && !success) {
                try {
                    val result = credentialManager.getCredential(
                        request = currentRequest,
                        context = activityContext
                    )
                    val credential = result.credential
                    if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        onSuccess(googleIdTokenCredential.idToken)
                        success = true
                    } else {
                        Log.e("GoogleSignInHelper", "Unsupported credential type: ${credential.type}")
                        onError("Unsupported credential type")
                        break
                    }
                } catch (e: GetCredentialCancellationException) {
                    Log.d("GoogleSignInHelper", "Google Sign In cancelled by user")
                    // Do not treat user cancellation as an error
                    break
                } catch (e: NoCredentialException) {
                    Log.w("GoogleSignInHelper", "NoCredentialException caught. Retries left: $retries", e)
                    if (retries > 0) {
                        retries--
                        currentRequest = fallbackRequest
                        delay(350)
                    } else {
                        onError(e.message)
                        break
                    }
                } catch (e: GetCredentialException) {
                    Log.w("GoogleSignInHelper", "Credential Manager error (${e.type}). Retries left: $retries", e)
                    if (retries > 0) {
                        retries--
                        currentRequest = fallbackRequest
                        delay(350)
                    } else {
                        Log.e("GoogleSignInHelper", "Credential Manager failed: ${e.type}", e)
                        onError(e.message)
                        break
                    }
                } catch (e: Exception) {
                    Log.e("GoogleSignInHelper", "Sign in error", e)
                    onError(e.message)
                    break
                }
            }
        }
    }
}
