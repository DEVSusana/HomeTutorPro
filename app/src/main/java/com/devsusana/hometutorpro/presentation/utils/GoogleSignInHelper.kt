package com.devsusana.hometutorpro.presentation.utils

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
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
        if (resId == 0) {
            Log.e("GoogleSignInHelper", "Default web client ID resource 'default_web_client_id' not found in resources")
            onError("Web client ID missing")
            return
        }
        val webClientId = context.getString(resId)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = context
                )
                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    onSuccess(googleIdTokenCredential.idToken)
                } else {
                    Log.e("GoogleSignInHelper", "Unsupported credential type: ${credential.type}")
                    onError("Unsupported credential type")
                }
            } catch (e: GetCredentialException) {
                Log.e("GoogleSignInHelper", "Credential Manager error: ${e.type}", e)
                onError(e.message)
            } catch (e: Exception) {
                Log.e("GoogleSignInHelper", "Sign in error", e)
                onError(e.message)
            }
        }
    }
}
