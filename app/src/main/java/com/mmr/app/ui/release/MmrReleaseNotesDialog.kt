package com.mmr.app.ui.release

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mmr.app.release.MmrReleaseNotesCatalog
import com.mmr.app.ui.components.MmrContentDialog
import com.mmr.app.ui.theme.MmrBrandColors

@Composable
fun MmrReleaseNotesDialog(
    appVersionName: String,
    onDismiss: () -> Unit,
) {
    val current = MmrReleaseNotesCatalog.notesForVersion(appVersionName)
    val history = MmrReleaseNotesCatalog.priorMajorMinorHistory(appVersionName)

    MmrContentDialog(
        title = "What's new",
        message = "Version $appVersionName",
        onDismiss = onDismiss,
        dismissLabel = "Close",
        showDismissButton = true,
        confirmLabel = null,
        onConfirm = null,
    ) {
        if (current != null && current.changesNewestFirst.isNotEmpty()) {
            Text(
                text = "Latest in this version",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MmrBrandColors.TextStrong,
                modifier = Modifier.padding(top = 4.dp),
            )
            BulletedChanges(current.changesNewestFirst)
        } else {
            Text(
                text = "Welcome to Money Inbox. See Settings for backup, SMS scan range, and privacy options.",
                style = MaterialTheme.typography.bodySmall,
                color = MmrBrandColors.TextMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        if (history.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            Text(
                text = "Version history",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MmrBrandColors.TextStrong,
            )
            history.forEach { past ->
                Column(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = past.version,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MmrBrandColors.TextStrong,
                    )
                    BulletedChanges(past.changesNewestFirst)
                }
            }
        }
    }
}

@Composable
private fun BulletedChanges(items: List<String>) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items.forEach { line ->
            Text(
                text = "• $line",
                style = MaterialTheme.typography.bodySmall,
                color = MmrBrandColors.TextMedium,
            )
        }
    }
}
