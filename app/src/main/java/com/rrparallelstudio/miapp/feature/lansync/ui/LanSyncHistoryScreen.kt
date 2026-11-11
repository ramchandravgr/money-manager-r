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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rrparallelstudio.miapp.core.lansync.LanSyncOverrideLine
import com.rrparallelstudio.miapp.feature.lansync.viewmodel.LanSyncHistoryViewModel
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsSection
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsSectionBodyText
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
        title = "Sync history",
        onNavigateBack = onNavigateBack,
        layout = MmrSettingsScreenLayout.InsetGrouped,
    ) {
        if (sessions.isEmpty()) {
            MmrIosSettingsSection(title = null) {
                MmrIosSettingsSectionBodyText(
                    text = "Nothing has synced yet. After Sync now, replacements from the other phone show up here.",
                )
            }
        }
        sessions.forEach { session ->
            MmrIosSettingsSection(
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
                MmrIosSettingsSectionBodyText(
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
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .border(1.dp, MmrBrandColors.Header.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .background(MmrBrandColors.Header.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                .padding(12.dp),
    ) {
        Text(
            text = "REPLACED YOUR ${line.kind.uppercase(Locale.getDefault())}",
            color = MmrBrandColors.Header,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = line.title,
            color = MmrBrandColors.TextStrong,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = "On this phone: ${line.yours}",
            color = MmrBrandColors.TextMedium,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = "From the other phone: ${line.theirs}",
            color = MmrBrandColors.TextStrong,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
