package com.rrparallelstudio.miapp.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rrparallelstudio.miapp.core.platform.MmrPlayStoreIntents
import com.rrparallelstudio.miapp.feature.settings.model.SmsBackupRestoreGuideCatalog
import com.rrparallelstudio.miapp.testing.MmrTestTags
import com.rrparallelstudio.miapp.ui.components.MmrHelperText
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsPrimaryActionSlot
import com.rrparallelstudio.miapp.ui.components.MmrPrimaryActionButton
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroup
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupNavRow
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupNote
import com.rrparallelstudio.miapp.ui.components.MmrSettingsScreenLayout
import com.rrparallelstudio.miapp.ui.components.MmrSettingsScreenScaffold
import com.rrparallelstudio.miapp.ui.theme.MmrBrandColors
import com.rrparallelstudio.miapp.ui.theme.mmrBottomSheetTitleStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsBackupRestoreSetupGuideSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onChooseBackupFolder: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
        containerColor = MmrBrandColors.modalChromeSurface(),
        contentColor = MmrBrandColors.modalSheetTitleText(),
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
                    .testTag(MmrTestTags.settingsTransactionScanSbrGuideSheet),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Setup Guide",
                style = mmrBottomSheetTitleStyle(MmrBrandColors.modalSheetTitleText()),
            )
            MmrHelperText(
                text = SmsBackupRestoreGuideCatalog.intro,
                color = MmrBrandColors.modalSheetBodyText(),
            )
            SmsBackupRestoreGuideCatalog.steps.forEachIndexed { index, step ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "${index + 1}. ${step.title}",
                        fontWeight = FontWeight.SemiBold,
                        color = MmrBrandColors.modalSheetTitleText(),
                    )
                    MmrHelperText(
                        text = step.body,
                        color = MmrBrandColors.modalSheetBodyText(),
                    )
                    if (index == 0) {
                        MmrPrimaryActionButton(
                            text = "Open Play Store — ${SmsBackupRestoreGuideCatalog.PLAY_STORE_APP_NAME}",
                            onClick = {
                                MmrPlayStoreIntents.openAppListing(
                                    context,
                                    SmsBackupRestoreGuideCatalog.PLAY_STORE_PACKAGE,
                                )
                            },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .testTag(MmrTestTags.settingsTransactionScanSbrGuidePlayStore),
                        )
                    }
                }
            }
            MmrHelperText(
                text = SmsBackupRestoreGuideCatalog.footnote,
                color = MmrBrandColors.modalSheetBodyText(),
            )
            if (onChooseBackupFolder != null) {
                MmrPrimaryActionButton(
                    text = "Choose backup folder",
                    onClick = {
                        onDismiss()
                        onChooseBackupFolder()
                    },
                    modifier =
                        Modifier.fillMaxWidth().testTag(MmrTestTags.settingsTransactionScanSbrGuideChooseFolder),
                )
            }
            MmrIosSettingsPrimaryActionSlot(
                text = "Done",
                onClick = onDismiss,
                showDividerBelow = false,
            )
        }
    }
}

@Composable
fun SmsBackupSetupGuideScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    MmrSettingsScreenScaffold(
        title = "Setup Guide",
        onNavigateBack = onNavigateBack,
        layout = MmrSettingsScreenLayout.InsetGrouped,
    ) {
        MmrSettingsGroup(
            title = "SMS Backup & Restore",
            isFirstInScreen = true,
            footer = SmsBackupRestoreGuideCatalog.footnote,
        ) {
            MmrSettingsGroupNote(text = SmsBackupRestoreGuideCatalog.intro)
            SmsBackupRestoreGuideCatalog.steps.forEachIndexed { index, step ->
                val last = index == SmsBackupRestoreGuideCatalog.steps.lastIndex
                if (index == 0) {
                    MmrSettingsGroupNavRow(
                        label = "${index + 1}. ${step.title}",
                        value = "Play Store",
                        onClick = {
                            MmrPlayStoreIntents.openAppListing(
                                context,
                                SmsBackupRestoreGuideCatalog.PLAY_STORE_PACKAGE,
                            )
                        },
                        testTag = MmrTestTags.settingsTransactionScanSbrGuidePlayStore,
                    )
                } else {
                    MmrSettingsGroupNote(text = "${index + 1}. ${step.title}", showDividerBelow = false)
                }
                MmrSettingsGroupNote(text = step.body, showDividerBelow = !last)
            }
        }
    }
}
