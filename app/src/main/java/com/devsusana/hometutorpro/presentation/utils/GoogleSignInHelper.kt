package com.devsusana.hometutorpro.presentation.utils

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.devsusana.hometutorpro.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object GoogleSignInHelper {

    fun launchGoogleSignIn(
        context: Context,
        scope: CoroutineScope,
        onSuccess: (String) -> Unit,
        onError: (String?) -> Unit
    ) {
        val credentialManager = CredentialManager.create(context)
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

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        scope.launch {
            var retries = 1
            var success = false
            while (retries >= 0 && !success) {
                try {
                    val result = credentialManager.getCredential(
                        request = request,
                        context = context
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
                } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
                    Log.d("GoogleSignInHelper", "Google Sign In cancelled by user")
                    // Do not treat user cancellation as an error
                    break
                } catch (e: androidx.credentials.exceptions.NoCredentialException) {
                    Log.w("GoogleSignInHelper", "NoCredentialException caught. Retries left: $retries", e)
                    if (retries > 0) {
                        retries--
                        kotlinx.coroutines.delay(500) // Small delay to let Play Services warm up
                    } else {
                        onError(e.message)
                        break
                    }
                } catch (e: GetCredentialException) {
                    Log.e("GoogleSignInHelper", "Credential Manager error: ${e.type}", e)
                    onError(e.message)
                    break
                } catch (e: Exception) {
                    Log.e("GoogleSignInHelper", "Sign in error", e)
                    onError(e.message)
                    break
                }
            }
        }
    }
}
