package com.rrparallelstudio.miapp.testing

import android.net.Uri

/** Supplies import URI to [BackupScreenHostActivity] after [androidx.test.core.app.ActivityScenario.recreate]. */
object BackupScreenHostTestBridge {
    var pendingImportUri: Uri? = null
    var pendingImportDisplayName: String? = null

    fun clear() {
        pendingImportUri = null
        pendingImportDisplayName = null
    }
}
