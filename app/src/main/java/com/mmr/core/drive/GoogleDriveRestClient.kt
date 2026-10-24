package com.mmr.core.drive

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class GoogleDriveFileRef(
    val id: String,
    val name: String,
    val modifiedTimeMillis: Long,
)

@Singleton
class GoogleDriveRestClient
    @Inject
    constructor() {
        private val http =
            OkHttpClient
                .Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .build()

        private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
        private val zipMediaType = "application/zip".toMediaType()

        fun findFolderByName(
            accessToken: String,
            parentId: String,
            name: String,
        ): GoogleDriveFileRef? {
            val q =
                "name = '${escapeQuery(name)}' and '$parentId' in parents and " +
                    "mimeType = 'application/vnd.google-apps.folder' and trashed = false"
            val files = listFiles(accessToken, q)
            return files.firstOrNull()
        }

        fun createFolder(
            accessToken: String,
            parentId: String,
            name: String,
        ): GoogleDriveFileRef {
            val body =
                JSONObject()
                    .put("name", name)
                    .put("mimeType", "application/vnd.google-apps.folder")
                    .put("parents", JSONArray().put(parentId))
                    .toString()
                    .toRequestBody(jsonMediaType)
            val request =
                Request
                    .Builder()
                    .url("$DRIVE_API/files")
                    .header("Authorization", bearer(accessToken))
                    .post(body)
                    .build()
            val response = executeJson(request)
            return GoogleDriveFileRef(
                id = response.getString("id"),
                name = response.getString("name"),
                modifiedTimeMillis = parseDriveTime(response.optString("modifiedTime")),
            )
        }

        fun listFilesInFolder(
            accessToken: String,
            folderId: String,
        ): List<GoogleDriveFileRef> {
            val q = "'$folderId' in parents and trashed = false"
            return listFiles(accessToken, q)
        }

        fun renameFile(
            accessToken: String,
            fileId: String,
            newName: String,
        ) {
            val body =
                JSONObject()
                    .put("name", newName)
                    .toString()
                    .toRequestBody(jsonMediaType)
            val request =
                Request
                    .Builder()
                    .url("$DRIVE_API/files/$fileId")
                    .header("Authorization", bearer(accessToken))
                    .patch(body)
                    .build()
            executeRaw(request)
        }

        fun deleteFile(
            accessToken: String,
            fileId: String,
        ) {
            val request =
                Request
                    .Builder()
                    .url("$DRIVE_API/files/$fileId")
                    .header("Authorization", bearer(accessToken))
                    .delete()
                    .build()
            executeRaw(request)
        }

        fun uploadFile(
            accessToken: String,
            parentFolderId: String,
            fileName: String,
            bytes: ByteArray,
        ): GoogleDriveFileRef {
            val metadata =
                JSONObject()
                    .put("name", fileName)
                    .put("parents", JSONArray().put(parentFolderId))
            val multipart =
                buildMultipartBody(
                    metadata = metadata,
                    mimeType = "application/zip",
                    fileBytes = bytes,
                )
            val request =
                Request
                    .Builder()
                    .url("$UPLOAD_API/files?uploadType=multipart")
                    .header("Authorization", bearer(accessToken))
                    .post(multipart)
                    .build()
            val response = executeJson(request)
            return GoogleDriveFileRef(
                id = response.getString("id"),
                name = response.getString("name"),
                modifiedTimeMillis = parseDriveTime(response.optString("modifiedTime")),
            )
        }

        fun downloadFile(
            accessToken: String,
            fileId: String,
        ): ByteArray {
            val request =
                Request
                    .Builder()
                    .url("$DRIVE_API/files/$fileId?alt=media")
                    .header("Authorization", bearer(accessToken))
                    .get()
                    .build()
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Drive download failed: ${response.code}")
                }
                return response.body?.bytes() ?: throw IOException("Empty Drive download body")
            }
        }

        private fun listFiles(
            accessToken: String,
            query: String,
        ): List<GoogleDriveFileRef> {
            val encoded = URLEncoder.encode(query, Charsets.UTF_8.name())
            val url =
                "$DRIVE_API/files?q=$encoded&spaces=drive&fields=files(id,name,modifiedTime)&pageSize=100"
            val request =
                Request
                    .Builder()
                    .url(url)
                    .header("Authorization", bearer(accessToken))
                    .get()
                    .build()
            val json = executeJson(request)
            val array = json.optJSONArray("files") ?: JSONArray()
            return buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    add(
                        GoogleDriveFileRef(
                            id = item.getString("id"),
                            name = item.getString("name"),
                            modifiedTimeMillis = parseDriveTime(item.optString("modifiedTime")),
                        ),
                    )
                }
            }
        }

        private fun executeJson(request: Request): JSONObject {
            val body = executeRaw(request)
            return JSONObject(body)
        }

        private fun executeRaw(request: Request): String {
            http.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IOException("Drive API ${response.code}: $body")
                }
                return body
            }
        }

        private fun bearer(accessToken: String): String = "Bearer $accessToken"

        private fun escapeQuery(value: String): String = value.replace("'", "\\'")

        private fun parseDriveTime(raw: String?): Long {
            if (raw.isNullOrBlank()) return 0L
            return runCatching {
                java.time.Instant
                    .parse(raw)
                    .toEpochMilli()
            }.getOrDefault(0L)
        }

        private fun buildMultipartBody(
            metadata: JSONObject,
            mimeType: String,
            fileBytes: ByteArray,
        ): okhttp3.RequestBody {
            val boundary = "mmr_drive_${System.currentTimeMillis()}"
            val mediaType = "multipart/related; boundary=$boundary".toMediaType()
            val part1 =
                "--$boundary\r\n" +
                    "Content-Type: application/json; charset=UTF-8\r\n\r\n" +
                    "${metadata}\r\n"
            val part2Header =
                "--$boundary\r\n" +
                    "Content-Type: $mimeType\r\n\r\n"
            val end = "\r\n--$boundary--\r\n"
            val bytes =
                part1.toByteArray(Charsets.UTF_8) +
                    part2Header.toByteArray(Charsets.UTF_8) +
                    fileBytes +
                    end.toByteArray(Charsets.UTF_8)
            return bytes.toRequestBody(mediaType)
        }

        fun fetchAuthenticatedUserEmail(accessToken: String): String? {
            val request =
                Request
                    .Builder()
                    .url("https://www.googleapis.com/oauth2/v3/userinfo")
                    .header("Authorization", "Bearer $accessToken")
                    .get()
                    .build()
            return try {
                http.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return null
                    val body = response.body?.string() ?: return null
                    JSONObject(body).optString("email").takeIf { it.isNotBlank() }
                }
            } catch (_: IOException) {
                null
            }
        }

        private companion object {
            const val DRIVE_API = "https://www.googleapis.com/drive/v3"
            const val UPLOAD_API = "https://www.googleapis.com/upload/drive/v3"
        }
    }
