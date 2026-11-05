package com.mmr.core.export

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PeriodExcelDownloadsSaver
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun saveToDownloads(
            source: File,
            displayName: String,
        ): String {
            val safeName = displayName.replace(Regex("[/\\\\?*\\[\\]:]"), "-")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                return saveViaMediaStore(source, safeName)
            }
            return savePublicDownloadsPreApi29(source, safeName)
        }

        @RequiresApi(Build.VERSION_CODES.Q)
        private fun saveViaMediaStore(
            source: File,
            displayName: String,
        ): String {
            val resolver = context.contentResolver
            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val pending =
                ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                    put(
                        MediaStore.Downloads.MIME_TYPE,
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    )
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
            val itemUri =
                resolver.insert(collection, pending)
                    ?: throw IllegalStateException("Could not create Downloads entry.")
            try {
                resolver.openOutputStream(itemUri)?.use { out ->
                    source.inputStream().use { input -> input.copyTo(out) }
                } ?: throw IllegalStateException("Could not open Downloads output stream.")
                val ready =
                    ContentValues().apply {
                        put(MediaStore.Downloads.IS_PENDING, 0)
                    }
                resolver.update(itemUri, ready, null, null)
            } catch (e: Exception) {
                resolver.delete(itemUri, null, null)
                throw e
            }
            return displayName
        }

        @Suppress("DEPRECATION")
        private fun savePublicDownloadsPreApi29(
            source: File,
            displayName: String,
        ): String {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!dir.exists() && !dir.mkdirs()) {
                throw IllegalStateException("Downloads folder is not available.")
            }
            val dest = File(dir, displayName)
            source.inputStream().use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            return dest.name
        }
    }
