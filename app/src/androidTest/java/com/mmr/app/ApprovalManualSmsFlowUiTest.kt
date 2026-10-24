package com.mmr.app

import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.app.testing.MmrHiltUiTest
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.testing.traceClickTag
import com.mmr.app.testing.traceClickText
import com.mmr.app.testing.traceNodeTextInput
import com.mmr.app.testing.traceWaitUntil
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

        composeRule.traceClickTag(MmrTestTags.homeTopBarApprovals)
        composeRule.waitForIdle()
        composeRule.traceClickTag(MmrTestTags.approvalFabManualSms)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Add SMS manually").assertExists()

        composeRule.traceNodeTextInput("sms_message", { composeRule.onNodeWithText("SMS message") }, body)
        composeRule.traceClickText("Add to pending")

        composeRule.traceWaitUntil("wait_until", timeoutMillis = 15_000) {
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
