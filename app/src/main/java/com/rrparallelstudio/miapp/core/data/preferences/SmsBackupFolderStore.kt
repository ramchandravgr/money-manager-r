package com.rrparallelstudio.miapp.core.data.preferences

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persisted SAF tree URI for SMS Backup & Restore XML exports (Play-safe intake).
 * Device-local; not included in ledger backup JSON.
 */
@Singleton
class SmsBackupFolderStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        private val _folderTreeUri = MutableStateFlow(readUri())
        val folderTreeUri: StateFlow<Uri?> = _folderTreeUri.asStateFlow()

        private val _folderDisplayName = MutableStateFlow(readDisplayName())
        val folderDisplayName: StateFlow<String?> = _folderDisplayName.asStateFlow()

        fun getFolderTreeUri(): Uri? = _folderTreeUri.value

        fun getFolderDisplayName(): String? = _folderDisplayName.value?.takeIf { it.isNotBlank() }

        fun setFolderTree(treeUri: Uri) {
            val label = treeUri.toDisplayFolderLabel()
            prefs
                .edit()
                .putString(KEY_TREE_URI, treeUri.toString())
                .putString(KEY_DISPLAY_NAME, label)
                .apply()
            _folderTreeUri.value = treeUri
            _folderDisplayName.value = label
        }

        fun clearFolder() {
            prefs
                .edit()
                .remove(KEY_TREE_URI)
                .remove(KEY_DISPLAY_NAME)
                .apply()
            _folderTreeUri.value = null
            _folderDisplayName.value = null
        }

        fun isConfigured(): Boolean = _folderTreeUri.value != null

        fun getLastImportSourceUri(): String? = prefs.getString(KEY_LAST_IMPORT_URI, null)

        fun getLastImportSourceModified(): Long = prefs.getLong(KEY_LAST_IMPORT_MODIFIED, 0L)

        fun recordSuccessfulImport(
            sourceUri: String,
            lastModified: Long,
            fileName: String?,
        ) {
            prefs
                .edit()
                .putString(KEY_LAST_IMPORT_URI, sourceUri)
                .putLong(KEY_LAST_IMPORT_MODIFIED, lastModified)
                .putString(KEY_LAST_IMPORT_FILE, fileName)
                .putLong(KEY_LAST_IMPORT_AT, System.currentTimeMillis())
                .apply()
        }

        private fun readUri(): Uri? =
            prefs.getString(KEY_TREE_URI, null)?.let { runCatching { Uri.parse(it) }.getOrNull() }

        private fun readDisplayName(): String? = prefs.getString(KEY_DISPLAY_NAME, null)

        private fun Uri.toDisplayFolderLabel(): String {
            val segment = lastPathSegment ?: return "Selected folder"
            val decoded =
                runCatching {
                    java.net.URLDecoder.decode(segment, Charsets.UTF_8.name())
                }.getOrDefault(segment)
            val afterColon = decoded.substringAfter(':', decoded)
            return afterColon.ifBlank { "Selected folder" }
        }

        private companion object {
            const val PREFS_NAME = "mmr_sms_backup_folder"
            const val KEY_TREE_URI = "tree_uri"
            const val KEY_DISPLAY_NAME = "display_name"
            const val KEY_LAST_IMPORT_URI = "last_import_uri"
            const val KEY_LAST_IMPORT_MODIFIED = "last_import_modified"
            const val KEY_LAST_IMPORT_FILE = "last_import_file"
            const val KEY_LAST_IMPORT_AT = "last_import_at"
        }
    }
