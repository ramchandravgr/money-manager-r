package com.rrparallelstudio.miapp

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rrparallelstudio.miapp.testing.MmrHiltUiTest
import com.rrparallelstudio.miapp.testing.MmrTestTags
import com.rrparallelstudio.miapp.testing.traceClickTag
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Play builds use SMS backup XML refresh (no READ_SMS); paste remains.
 */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class ApprovalInboxScanUiTest : MmrHiltUiTest() {
    @Test
    fun approvalCenter_showsBackupRefresh_andPasteEmptyState() {
        composeRule.traceClickTag(MmrTestTags.homeTopBarApprovals)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(MmrTestTags.approvalScanSms).assertExists()
        composeRule.onNodeWithTag(MmrTestTags.approvalFabManualSms).assertExists()
        composeRule.onNodeWithText("Transaction scan", substring = true).assertExists()
    }
}
