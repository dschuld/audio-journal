package com.audiojournal.app.upload.drive

import android.app.PendingIntent
import android.content.Context
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/**
 * Wraps Google Play services' AuthorizationClient to obtain OAuth access
 * tokens for the Drive API. No client secret lives in the app: the OAuth
 * client is registered in Google Cloud Console against this app's package
 * name and signing certificate, and Play services brokers the tokens for the
 * Google account on the device.
 */
class DriveAuthManager(private val context: Context) {

    private val authorizationRequest = AuthorizationRequest.builder()
        .setRequestedScopes(listOf(Scope(DRIVE_SCOPE)))
        .build()

    /**
     * Access token if the user has already granted Drive access, or null if
     * consent is still needed (or Play services is unavailable).
     */
    suspend fun getAccessToken(): String? = try {
        val result = Identity.getAuthorizationClient(context)
            .authorize(authorizationRequest)
            .await()
        if (result.hasResolution()) null else result.accessToken
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    /**
     * PendingIntent that launches the Google consent UI, or null if access
     * is already granted (no UI needed).
     */
    suspend fun consentIntent(): PendingIntent? {
        val result = Identity.getAuthorizationClient(context)
            .authorize(authorizationRequest)
            .await()
        return if (result.hasResolution()) result.pendingIntent else null
    }

    companion object {
        // Full Drive scope so recordings can be placed into folders that
        // already exist in your Drive (e.g. the one your backend reads).
        // Switch to "https://www.googleapis.com/auth/drive.file" if you only
        // need folders created by this app.
        const val DRIVE_SCOPE = "https://www.googleapis.com/auth/drive"
    }
}
