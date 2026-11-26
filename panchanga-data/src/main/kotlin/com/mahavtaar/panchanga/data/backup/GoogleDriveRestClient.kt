package com.mahavtaar.panchanga.data.backup

import android.accounts.Account
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed interface DriveAuthState {
    data class Authorized(val accessToken: String) : DriveAuthState
    data class NeedsResolution(val pendingIntent: PendingIntent) : DriveAuthState
}

data class DriveFileEntry(
    val id: String,
    val name: String,
    val modifiedTime: String? = null
)

data class DriveReplaceResult(
    val newFileId: String,
    val deletedOldFileIds: List<String>
)

@Serializable
private data class DriveFilesListResponse(
    val files: List<DriveFileMetadataDto> = emptyList()
)

@Serializable
private data class DriveFileMetadataDto(
    val id: String = "",
    val name: String = "",
    val modifiedTime: String? = null
)

@Serializable
private data class DriveCreateMetadataDto(
    val name: String,
    val mimeType: String,
    val parents: List<String>? = null
)

class DriveUnauthorizedException(message: String) : IllegalStateException(message)

/**
 * Thrown when Google's OAuth consent screen rejects authorization (e.g. HTTP 403 access_denied
 * because the Cloud project's OAuth consent screen is still in Testing mode without the user
 * listed as a Test User, or not yet published to Production).
 */
class DriveOAuthConsentBlockedException(message: String) : IllegalStateException(message)

interface DriveHttpExecutor {
    suspend fun execute(
        method: String,
        url: String,
        headers: Map<String, String>,
        bodyBytes: ByteArray? = null
    ): Pair<Int, String>
}

@Singleton
class DefaultDriveHttpExecutor @Inject constructor() : DriveHttpExecutor {
    override suspend fun execute(
        method: String,
        url: String,
        headers: Map<String, String>,
        bodyBytes: ByteArray?
    ): Pair<Int, String> = withContext(Dispatchers.IO) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 20_000
            doInput = true
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
            if (bodyBytes != null) {
                doOutput = true
                setFixedLengthStreamingMode(bodyBytes.size)
            }
        }
        try {
            if (bodyBytes != null) {
                conn.outputStream.use { out ->
                    out.write(bodyBytes)
                    out.flush()
                }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.let {
                BufferedReader(InputStreamReader(it, StandardCharsets.UTF_8)).use { r -> r.readText() }
            }.orEmpty()
            code to body
        } finally {
            conn.disconnect()
        }
    }
}

/**
 * Personal user-owned Google Drive REST v3 JSON sync helper using Play Services
 * [AuthorizationClient] with non-sensitive least-privilege scopes:
 * - `drive.file`: visible app-created backup JSON inside the signed-in user's own Drive.
 * - `drive.appdata`: private hidden mirror inside the signed-in user's own Drive `appDataFolder`
 *   so accidental deletion of the visible file never causes data loss.
 *
 * Strictly binds requests to the signed-in user's Google [Account] email so both regular users
 * and admins always store and sync their data solely inside their own personal Google Drive.
 */
@Singleton
class GoogleDriveRestClient @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val httpExecutor: DriveHttpExecutor
) {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    companion object {
        val DRIVE_SCOPES = listOf(
            Scope("https://www.googleapis.com/auth/drive.file"),
            Scope("https://www.googleapis.com/auth/drive.appdata")
        )
        private const val DRIVE_FILES_URL = "https://www.googleapis.com/drive/v3/files"
        private const val DRIVE_UPLOAD_URL =
            "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id,name,modifiedTime"
        private const val GOOGLE_ACCOUNT_TYPE = "com.google"
    }

    suspend fun authorizeForDrive(
        activity: Activity? = null,
        accountEmail: String? = null
    ): DriveAuthState = withContext(Dispatchers.IO) {
        val builder = AuthorizationRequest.builder()
            .setRequestedScopes(DRIVE_SCOPES)
        accountEmail?.trim()?.takeIf { it.contains("@") }?.let { email ->
            builder.setAccount(Account(email, GOOGLE_ACCOUNT_TYPE))
        }
        val request = builder.build()
        val client = if (activity != null) {
            Identity.getAuthorizationClient(activity)
        } else {
            Identity.getAuthorizationClient(context)
        }
        val result = try {
            client.authorize(request).await()
        } catch (e: ApiException) {
            throw mapApiException(e)
        }
        if (result.hasResolution()) {
            val pi = result.pendingIntent
                ?: throw IllegalStateException("Google Drive authorization requires user confirmation")
            DriveAuthState.NeedsResolution(pi)
        } else {
            val token = result.accessToken?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Google Drive access token unavailable")
            DriveAuthState.Authorized(token)
        }
    }

    fun extractTokenFromIntent(intent: Intent?): String? {
        if (intent == null) return null
        return runCatching {
            val res = Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(intent)
            res.accessToken?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    fun extractTokenOrThrow(intent: Intent?, resultCode: Int = Activity.RESULT_OK): String {
        if (intent == null) {
            if (resultCode == Activity.RESULT_CANCELED) {
                throw DriveOAuthConsentBlockedException(
                    "Google Drive authorization was dismissed or blocked (Error 403: access_denied). " +
                        "Publish the OAuth Consent Screen to Production (or add your email to Test Users), " +
                        "or use 'Link Backup File in Google Drive' below for instant auto-sync."
                )
            }
            throw IllegalStateException("Google Drive authorization cancelled")
        }
        val res = try {
            Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(intent)
        } catch (e: ApiException) {
            throw mapApiException(e)
        }
        return res.accessToken?.takeIf { it.isNotBlank() }
            ?: throw DriveOAuthConsentBlockedException(
                "Google Drive did not return an access token (Error 403: access_denied if app is in Testing mode). " +
                    "Use 'Link Backup File in Google Drive' below or publish the OAuth Consent Screen to Production."
            )
    }

    private fun mapApiException(e: ApiException): Throwable {
        val raw = e.localizedMessage.orEmpty()
        val isConsentBlock = e.statusCode == 10 || // DEVELOPER_ERROR
            e.statusCode == 16 || // CANCELED from consent error screen
            raw.contains("403", ignoreCase = true) ||
            raw.contains("access_denied", ignoreCase = true) ||
            raw.contains("verification", ignoreCase = true)
        return if (isConsentBlock) {
            DriveOAuthConsentBlockedException(
                "Google blocked Drive OAuth (Error 403: access_denied). " +
                    "In Google Cloud Console -> OAuth Consent Screen, set Publishing Status to 'In Production' " +
                    "(or add this email under Test Users), OR tap 'Link Backup File in Google Drive' for instant auto-sync."
            )
        } else {
            IllegalStateException("Google Drive authorization failed (${e.statusCode}): ${raw.ifBlank { "Please try again" }}")
        }
    }

    suspend fun findFilesByName(
        accessToken: String,
        fileName: String,
        inAppDataFolder: Boolean = false
    ): List<DriveFileEntry> = withContext(Dispatchers.IO) {
        val escapedName = fileName.replace("\\", "\\\\").replace("'", "\\'")
        val query = if (inAppDataFolder) {
            "name='$escapedName' and 'appDataFolder' in parents and trashed=false"
        } else {
            "name='$escapedName' and trashed=false"
        }
        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.name())
        val fields = URLEncoder.encode("files(id,name,modifiedTime)", StandardCharsets.UTF_8.name())
        val spacesParam = if (inAppDataFolder) "&spaces=appDataFolder" else ""
        val url = "$DRIVE_FILES_URL?q=$encodedQuery$spacesParam&orderBy=modifiedTime%20desc&fields=$fields"
        val (code, body) = httpExecutor.execute(
            method = "GET",
            url = url,
            headers = mapOf("Authorization" to "Bearer $accessToken")
        )
        if (code == 401 || code == 403) {
            throw DriveUnauthorizedException("Google Drive permission expired ($code)")
        }
        if (code !in 200..299) {
            throw IllegalStateException("Google Drive query failed ($code): ${body.take(180)}")
        }
        val parsed = json.decodeFromString<DriveFilesListResponse>(body)
        parsed.files
            .filter { it.id.isNotBlank() }
            .map { DriveFileEntry(id = it.id, name = it.name, modifiedTime = it.modifiedTime) }
    }

    /**
     * Uploads the newest JSON file to the signed-in user's own Google Drive (`drive.file`) and
     * deletes all previous Drive file copies with the same name so only the latest JSON remains.
     */
    suspend fun replaceJsonFileInDrive(
        accessToken: String,
        fileName: String,
        jsonContent: String
    ): DriveReplaceResult = replaceJsonInSpace(
        accessToken = accessToken,
        fileName = fileName,
        jsonContent = jsonContent,
        inAppDataFolder = false
    )

    /**
     * Mirrors the newest JSON backup into the signed-in user's private Google Drive `appDataFolder`
     * (`drive.appdata`) so accidental deletion of the visible file in Drive never causes data loss.
     */
    suspend fun replaceAppDataJsonInDrive(
        accessToken: String,
        fileName: String,
        jsonContent: String
    ): DriveReplaceResult = replaceJsonInSpace(
        accessToken = accessToken,
        fileName = fileName,
        jsonContent = jsonContent,
        inAppDataFolder = true
    )

    private suspend fun replaceJsonInSpace(
        accessToken: String,
        fileName: String,
        jsonContent: String,
        inAppDataFolder: Boolean
    ): DriveReplaceResult = withContext(Dispatchers.IO) {
        val existingFiles = findFilesByName(accessToken, fileName, inAppDataFolder = inAppDataFolder)
        val newMeta = json.encodeToString(
            DriveCreateMetadataDto.serializer(),
            DriveCreateMetadataDto(
                name = fileName,
                mimeType = "application/json",
                parents = if (inAppDataFolder) listOf("appDataFolder") else null
            )
        )
        val boundary = "mahavtaar_${UUID.randomUUID().toString().replace("-", "")}"
        val multipartBody = buildString {
            append("--").append(boundary).append("\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(newMeta).append("\r\n")
            append("--").append(boundary).append("\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(jsonContent).append("\r\n")
            append("--").append(boundary).append("--\r\n")
        }.toByteArray(StandardCharsets.UTF_8)

        val (uploadCode, uploadBody) = httpExecutor.execute(
            method = "POST",
            url = DRIVE_UPLOAD_URL,
            headers = mapOf(
                "Authorization" to "Bearer $accessToken",
                "Content-Type" to "multipart/related; boundary=$boundary"
            ),
            bodyBytes = multipartBody
        )
        if (uploadCode == 401 || uploadCode == 403) {
            throw DriveUnauthorizedException("Google Drive upload unauthorized ($uploadCode)")
        }
        if (uploadCode !in 200..299) {
            throw IllegalStateException("Google Drive upload failed ($uploadCode): ${uploadBody.take(180)}")
        }
        val uploadedDto = json.decodeFromString<DriveFileMetadataDto>(uploadBody)
        val newId = uploadedDto.id.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Google Drive upload returned empty file ID")

        val deletedOldIds = mutableListOf<String>()
        for (oldEntry in existingFiles) {
            if (oldEntry.id == newId) continue
            val (delCode, _) = httpExecutor.execute(
                method = "DELETE",
                url = "$DRIVE_FILES_URL/${oldEntry.id}",
                headers = mapOf("Authorization" to "Bearer $accessToken")
            )
            if (delCode in 200..299 || delCode == 404) {
                deletedOldIds += oldEntry.id
            }
        }
        DriveReplaceResult(
            newFileId = newId,
            deletedOldFileIds = deletedOldIds
        )
    }

    suspend fun downloadLatestJsonFromDrive(
        accessToken: String,
        fileName: String
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        tryDownloadLatestJsonFromDriveOrAppData(accessToken, fileName)
            ?: throw IllegalStateException("No JSON backup ($fileName) found in Google Drive")
    }

    /**
     * Looks up the latest user-owned JSON backup in visible Drive (`drive.file`) first, and if
     * absent falls back to the private `appDataFolder` (`drive.appdata`) safety mirror.
     * Returns `null` cleanly if no remote backup exists yet for this user.
     */
    suspend fun tryDownloadLatestJsonFromDriveOrAppData(
        accessToken: String,
        fileName: String
    ): Pair<String, String>? = withContext(Dispatchers.IO) {
        val visibleEntry = findFilesByName(accessToken, fileName, inAppDataFolder = false).firstOrNull()
        val entry = visibleEntry
            ?: runCatching {
                findFilesByName(accessToken, fileName, inAppDataFolder = true).firstOrNull()
            }.getOrNull()
            ?: return@withContext null

        val (code, body) = httpExecutor.execute(
            method = "GET",
            url = "$DRIVE_FILES_URL/${entry.id}?alt=media",
            headers = mapOf("Authorization" to "Bearer $accessToken")
        )
        if (code == 401 || code == 403) {
            throw DriveUnauthorizedException("Google Drive download unauthorized ($code)")
        }
        if (code !in 200..299) {
            throw IllegalStateException("Google Drive download failed ($code)")
        }
        entry.id to body
    }
}
