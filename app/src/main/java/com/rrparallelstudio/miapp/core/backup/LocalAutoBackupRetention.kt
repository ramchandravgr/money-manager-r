package com.rrparallelstudio.miapp.core.backup

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalAutoBackupRetention
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun pruneRetainedAutoBackups() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                pruneViaMediaStore()
            } else {
                prunePreApi29()
            }
        }

        @Suppress("DEPRECATION")
        private fun prunePreApi29() {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val files =
                dir
                    .listFiles()
                    ?.filter { it.name.startsWith(LocalAutoBackupContracts.FILE_NAME_PREFIX) }
                    ?.sortedByDescending { it.lastModified() }
                    ?: return
            files.drop(LocalAutoBackupContracts.MAX_RETAINED_FILES).forEach { it.delete() }
        }

        private fun pruneViaMediaStore() {
            val resolver = context.contentResolver
            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val projection =
                arrayOf(MediaStore.Downloads._ID, MediaStore.Downloads.DISPLAY_NAME, MediaStore.Downloads.DATE_MODIFIED)
            val selection = "${MediaStore.Downloads.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("${LocalAutoBackupContracts.FILE_NAME_PREFIX}%")
            val sort = "${MediaStore.Downloads.DATE_MODIFIED} DESC"
            val ids = mutableListOf<Long>()
            resolver.query(collection, projection, selection, selectionArgs, sort)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                while (cursor.moveToNext()) {
                    ids += cursor.getLong(idColumn)
                }
            }
            ids.drop(LocalAutoBackupContracts.MAX_RETAINED_FILES).forEach { id ->
                val uri = ContentUris.withAppendedId(collection, id)
                resolver.delete(uri, null, null)
            }
        }
    }
