package com.mmr.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.mmr.app.testing.BackupScreenHostTestBridge
import com.mmr.app.ui.theme.MoneyManagerRTheme
import com.mmr.feature.backup.ui.BackupScreen
import dagger.hilt.android.AndroidEntryPoint

/** Debug-only host for instrumented backup import UI (no SAF picker). */
@AndroidEntryPoint
class BackupScreenHostActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val importUri =
            intent.getParcelableExtra(IMPORT_URI_EXTRA, Uri::class.java)
                ?: BackupScreenHostTestBridge.pendingImportUri
        setContent {
            MoneyManagerRTheme {
                BackupScreen(
                    onNavigateBack = { finish() },
                    testImportUri = importUri,
                )
            }
        }
    }

    companion object {
        const val IMPORT_URI_EXTRA = "import_uri"
    }
}
