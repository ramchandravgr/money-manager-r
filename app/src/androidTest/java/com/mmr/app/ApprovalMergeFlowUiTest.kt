package com.mmr.app

import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.app.testing.MmrHiltUiTest
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.testing.MmrUiTestFixtures
import com.mmr.core.domain.repository.ApprovalRepository
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class ApprovalMergeFlowUiTest : MmrHiltUiTest() {
    @Inject
    lateinit var approvalRepository: ApprovalRepository

    @Test
    fun quickApprove_withLedgerMatch_showsMergeDialog() {
        val fixture =
            runBlocking {
                MmrUiTestFixtures.seedSmsMergeScenario(database, approvalRepository)
            }

        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(MmrTestTags.homeTopBarApprovals).performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule
                .onAllNodesWithTag(MmrTestTags.approvalCard(fixture.approvalId))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule.onNodeWithTag(MmrTestTags.approvalQuickApprove(fixture.approvalId)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Merge with existing transaction?").assertExists()
        composeRule.onNodeWithText("Transaction #${fixture.transactionId}", substring = true).assertExists()
    }
}
