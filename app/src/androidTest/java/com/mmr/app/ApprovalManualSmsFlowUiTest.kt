package com.mmr.app

import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.app.testing.MmrHiltUiTest
import com.mmr.app.testing.MmrTestTags
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class ApprovalManualSmsFlowUiTest : MmrHiltUiTest() {
    @Test
    fun manualSms_addsPendingApprovalMessage() {
        val body =
            """
            You've spent Rs.1,250.00 on HDFC Bank Credit Card xx1234 at Amazon on 12-10-2025.
            """.trimIndent()

        composeRule.onNodeWithTag(MmrTestTags.homeTopBarApprovals).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(MmrTestTags.approvalFabManualSms).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Add SMS manually").assertExists()

        composeRule.onNodeWithText("SMS message").performTextInput(body)
        composeRule.onNodeWithText("Add to pending").performClick()

        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule
                .onAllNodesWithText("Manual SMS added to pending list.", substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty() ||
                composeRule
                    .onAllNodesWithText("Amazon", substring = true)
                    .fetchSemanticsNodes()
                    .isNotEmpty()
        }
    }
}
