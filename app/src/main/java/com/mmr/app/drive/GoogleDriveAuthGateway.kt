package com.mmr.app.drive

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import com.mmr.core.data.preferences.GoogleDriveBackupPreferencesStore
import com.mmr.core.drive.GoogleDriveBackupContracts
import com.mmr.core.drive.GoogleDriveRestClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class GoogleDriveAuthorizedSession(
    val email: String,
    val accessToken: String,
)

sealed class GoogleDriveAuthorizeStep {
    data class NeedsUi(
        val pendingIntent: PendingIntent,
    ) : GoogleDriveAuthorizeStep()

    data class Complete(
        val email: String,
        val accessToken: String,
    ) : GoogleDriveAuthorizeStep()
}

@Singleton
class GoogleDriveAuthGateway
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val webClientIdProvider: GoogleDriveWebClientIdProvider,
        private val preferencesStore: GoogleDriveBackupPreferencesStore,
        private val driveRestClient: GoogleDriveRestClient,
    ) {
        fun isConfigured(): Boolean = webClientIdProvider.isConfigured

        suspend fun authorize(): GoogleDriveAuthorizeStep =
            withContext(Dispatchers.IO) {
                requireConfigured()
                val result = Tasks.await(authorizationClient().authorize(buildAuthorizationRequest()))
                result.toStep()
            }

        suspend fun completeAuthorize(data: Intent?): Result<GoogleDriveAuthorizedSession> =
            withContext(Dispatchers.IO) {
                requireConfigured()
                if (data == null) {
                    return@withContext Result.failure(
                        IllegalStateException("Google Drive authorization was cancelled."),
                    )
                }
                runCatching {
                    val result = authorizationClient().getAuthorizationResultFromIntent(data)
                    if (result.hasResolution()) {
                        throw IllegalStateException("Google Drive authorization was not completed.")
                    }
                    val token =
                        result.accessToken?.takeIf { it.isNotBlank() }
                            ?: throw IllegalStateException("Google did not return a Drive access token.")
                    val email = persistAuthorizationResult(result, token)
                    GoogleDriveAuthorizedSession(email, token)
                }
            }

        suspend fun fetchAccessToken(): String? =
            withContext(Dispatchers.IO) {
                if (!preferencesStore.isLinked()) return@withContext null
                if (!webClientIdProvider.isConfigured) return@withContext null
                val result = Tasks.await(authorizationClient().authorize(buildAuthorizationRequest()))
                if (result.hasResolution()) return@withContext null
                result.accessToken?.takeIf { it.isNotBlank() }
            }

        suspend fun disconnect() {
            withContext(Dispatchers.IO) {
                preferencesStore.clearLink()
            }
        }

        private fun authorizationClient() = Identity.getAuthorizationClient(context)

        private fun buildAuthorizationRequest(): AuthorizationRequest {
            val clientId = webClientIdProvider.clientId
            return AuthorizationRequest
                .builder()
                .setRequestedScopes(
                    listOf(
                        driveScope(),
                        Scope(GoogleDriveBackupContracts.USERINFO_EMAIL_SCOPE),
                    ),
                ).requestOfflineAccess(clientId)
                .build()
        }

        private fun AuthorizationResult.toStep(): GoogleDriveAuthorizeStep {
            if (hasResolution()) {
                val pending =
                    pendingIntent
                        ?: throw IllegalStateException("Google Drive authorization needs user interaction.")
                return GoogleDriveAuthorizeStep.NeedsUi(pending)
            }
            val token =
                accessToken?.takeIf { it.isNotBlank() }
                    ?: throw IllegalStateException("Google did not return a Drive access token.")
            val email = persistAuthorizationResult(this, token)
            return GoogleDriveAuthorizeStep.Complete(email, token)
        }

        private fun persistAuthorizationResult(
            result: AuthorizationResult,
            accessToken: String,
        ): String {
            val email =
                result.toGoogleSignInAccount()?.email?.takeIf { it.isNotBlank() }
                    ?: driveRestClient.fetchAuthenticatedUserEmail(accessToken)
                    ?: throw IllegalStateException("Could not read your Google account email. Try again.")
            preferencesStore.setLinkedEmail(email)
            return email
        }

        private fun driveScope(): Scope = Scope(GoogleDriveBackupContracts.DRIVE_FILE_SCOPE)

        private fun requireConfigured() {
            if (!webClientIdProvider.isConfigured) {
                throw IllegalStateException(
                    "Google Drive backup is not configured. Add GOOGLE_WEB_CLIENT_ID to local.properties.",
                )
            }
        }
    }
