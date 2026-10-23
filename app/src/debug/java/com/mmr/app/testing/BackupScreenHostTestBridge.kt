package com.mmr.app.testing

import android.net.Uri

/** Supplies import URI to [BackupScreenHostActivity] after [androidx.test.core.app.ActivityScenario.recreate]. */
object BackupScreenHostTestBridge {
    var pendingImportUri: Uri? = null

    fun clear() {
        pendingImportUri = null
    }
}
