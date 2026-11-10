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
import androidx.compose.ui.unit.sp
import com.rrparallelstudio.miapp.core.platform.MmrPlayStoreIntents
import com.rrparallelstudio.miapp.feature.settings.model.SmsBackupRestoreGuideCatalog
import com.rrparallelstudio.miapp.testing.MmrTestTags
import com.rrparallelstudio.miapp.ui.components.MmrHelperText
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsPrimaryActionSlot
import com.rrparallelstudio.miapp.ui.components.MmrPrimaryActionButton
import com.rrparallelstudio.miapp.ui.theme.MmrBrandColors

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
                text = "SMS backup setup",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MmrBrandColors.modalSheetTitleText(),
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
                }
            }
            MmrHelperText(
                text = SmsBackupRestoreGuideCatalog.footnote,
                color = MmrBrandColors.modalSheetBodyText(),
            )
            MmrPrimaryActionButton(
                text = "Open Play Store — ${SmsBackupRestoreGuideCatalog.PLAY_STORE_APP_NAME}",
                onClick = {
                    MmrPlayStoreIntents.openAppListing(context, SmsBackupRestoreGuideCatalog.PLAY_STORE_PACKAGE)
                },
                modifier =
                    Modifier.fillMaxWidth().testTag(MmrTestTags.settingsTransactionScanSbrGuidePlayStore),
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
