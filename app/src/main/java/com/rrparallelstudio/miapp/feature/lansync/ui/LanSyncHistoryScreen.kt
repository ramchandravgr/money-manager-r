package com.rrparallelstudio.miapp.feature.lansync.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rrparallelstudio.miapp.core.lansync.LanSyncOverrideLine
import com.rrparallelstudio.miapp.feature.lansync.viewmodel.LanSyncHistoryViewModel
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroup
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupNote
import com.rrparallelstudio.miapp.ui.components.MmrSettingsScreenLayout
import com.rrparallelstudio.miapp.ui.components.MmrSettingsScreenScaffold
import com.rrparallelstudio.miapp.ui.theme.MmrBrandColors
import java.util.Locale

@Composable
fun LanSyncHistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: LanSyncHistoryViewModel = hiltViewModel(),
) {
    val sessions by viewModel.sessions.collectAsState()
    MmrSettingsScreenScaffold(
        title = "Sync History",
        onNavigateBack = onNavigateBack,
        layout = MmrSettingsScreenLayout.InsetGrouped,
    ) {
        if (sessions.isEmpty()) {
            MmrSettingsGroup(title = "Recent", isFirstInScreen = true) {
                MmrSettingsGroupNote(
                    text = "Nothing has synced yet. After Sync Now, replacements from the other phone show up here.",
                    showDividerBelow = false,
                )
            }
        }
        sessions.forEach { session ->
            MmrSettingsGroup(
                title = session.peerName,
                footer =
                    buildString {
                        append(session.counts)
                        session.footerNote?.let { note ->
                            append("\n\n")
                            append(note)
                        }
                    },
            ) {
                MmrSettingsGroupNote(
                    text =
                        buildString {
                            append(session.whenLabel)
                            if (!session.success) append(" · Did not complete")
                        },
                )
                session.overrides.forEach { line ->
                    OverrideCard(line)
                }
            }
        }
    }
}

@Composable
private fun OverrideCard(line: LanSyncOverrideLine) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .border(1.dp, MmrBrandColors.Header.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .background(MmrBrandColors.Header.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                .padding(12.dp),
    ) {
        Text(
            text = "REPLACED YOUR ${line.kind.uppercase(Locale.getDefault())}",
            style =
                com.rrparallelstudio.miapp.ui.theme
                    .mmrLanSyncHistoryKickerStyle(MmrBrandColors.Header),
        )
        Text(
            text = line.title,
            style =
                com.rrparallelstudio.miapp.ui.theme.mmrSettingsInsetRowPrimaryStyle(MmrBrandColors.TextStrong).copy(
                    fontWeight = FontWeight.SemiBold,
                ),
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = "On this phone: ${line.yours}",
            style =
                com.rrparallelstudio.miapp.ui.theme
                    .mmrSettingsRowSecondaryStyle(MmrBrandColors.TextMedium),
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = "From the other phone: ${line.theirs}",
            style =
                com.rrparallelstudio.miapp.ui.theme.mmrSettingsRowSecondaryStyle(MmrBrandColors.TextStrong).copy(
                    fontWeight = FontWeight.SemiBold,
                ),
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
