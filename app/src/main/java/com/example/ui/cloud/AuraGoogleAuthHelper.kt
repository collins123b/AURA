package com.example.ui.cloud

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private const val AUTH_TAG = "AuraGoogleAuth"

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

object AuraGoogleAuthHelper {

    suspend fun attemptAutoSignIn(
        activityContext: Context,
        auth: FirebaseAuth
    ): Result<FirebaseUser?> {
        if (auth.currentUser != null) return Result.success(auth.currentUser)
        return try {
            val serverClientId = activityContext.getString(R.string.default_web_client_id)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(true)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val credentialManager = CredentialManager.create(activityContext)
            val response = credentialManager.getCredential(activityContext, request)
            exchangeGoogleCredential(response.credential, auth)
        } catch (e: CancellationException) {
            throw e
        } catch (_: NoCredentialException) {
            Result.success(null)
        } catch (_: GetCredentialCancellationException) {
            Log.w(AUTH_TAG, "Auto Google Sign-In dismissed by user.")
            Result.success(null)
        } catch (e: Exception) {
            Log.w(AUTH_TAG, "Auto Google Sign-In unavailable: ${e.message}")
            Result.success(null)
        }
    }

    suspend fun signInWithGoogleButton(
        activityContext: Context,
        auth: FirebaseAuth
    ): Result<FirebaseUser> {
        return try {
            val serverClientId = activityContext.getString(R.string.default_web_client_id)
            val signInOption = GetSignInWithGoogleOption.Builder(serverClientId).build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInOption)
                .build()
            val credentialManager = CredentialManager.create(activityContext)
            val response = credentialManager.getCredential(activityContext, request)
            val userResult = exchangeGoogleCredential(response.credential, auth)
            val signedInUser = userResult.getOrNull()
            if (signedInUser != null) {
                Result.success(signedInUser)
            } else {
                Result.failure(
                    userResult.exceptionOrNull()
                        ?: IllegalStateException("Google Sign-In did not return a Firebase user.")
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialCancellationException) {
            Log.w(AUTH_TAG, "Google Sign-In cancelled by user.")
            Result.failure(IllegalStateException("Sign-in cancelled."))
        } catch (e: Exception) {
            Log.e(AUTH_TAG, "Google Sign-In failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun signOutGoogle(context: Context, auth: FirebaseAuth) {
        auth.signOut()
        runCatching {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        }
    }

    private suspend fun exchangeGoogleCredential(
        credential: androidx.credentials.Credential,
        auth: FirebaseAuth
    ): Result<FirebaseUser?> {
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val tokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCred = GoogleAuthProvider.getCredential(tokenCredential.idToken, null)
            val authResult = auth.signInWithCredential(firebaseCred).await()
            return Result.success(authResult.user)
        }
        return Result.failure(IllegalArgumentException("Unsupported credential type: ${credential.type}"))
    }
}
