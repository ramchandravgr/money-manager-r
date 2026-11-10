package com.rrparallelstudio.miapp

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.rrparallelstudio.miapp.feature.backup.ui.BackupScreen
import com.rrparallelstudio.miapp.testing.BackupScreenHostTestBridge
import com.rrparallelstudio.miapp.ui.theme.MoneyInboxTheme
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
            MoneyInboxTheme {
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
