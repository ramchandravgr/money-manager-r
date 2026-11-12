package com.rrparallelstudio.miapp.core.data.preferences

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Persisted SAF tree URI for scheduled and manual ledger backups (.mibxbac). */
@Singleton
class LocalBackupFolderStore
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

        fun isConfigured(): Boolean = _folderTreeUri.value != null

        private fun readUri(): Uri? =
            prefs.getString(KEY_TREE_URI, null)?.let { runCatching { Uri.parse(it) }.getOrNull() }

        private fun readDisplayName(): String? = prefs.getString(KEY_DISPLAY_NAME, null)

        private fun Uri.toDisplayFolderLabel(): String {
            val segment = lastPathSegment ?: return "Selected Folder"
            val decoded =
                runCatching {
                    java.net.URLDecoder.decode(segment, Charsets.UTF_8.name())
                }.getOrDefault(segment)
            val afterColon = decoded.substringAfter(':', decoded)
            return afterColon.ifBlank { "Selected Folder" }
        }

        private companion object {
            const val PREFS_NAME = "mmr_local_backup_folder"
            const val KEY_TREE_URI = "tree_uri"
            const val KEY_DISPLAY_NAME = "display_name"
        }
    }
