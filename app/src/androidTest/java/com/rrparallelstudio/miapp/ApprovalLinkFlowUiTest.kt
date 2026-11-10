package com.rrparallelstudio.miapp

import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rrparallelstudio.miapp.core.domain.repository.ApprovalRepository
import com.rrparallelstudio.miapp.testing.MmrHiltUiTest
import com.rrparallelstudio.miapp.testing.MmrTestTags
import com.rrparallelstudio.miapp.testing.MmrUiTestFixtures
import com.rrparallelstudio.miapp.testing.traceClickTag
import com.rrparallelstudio.miapp.testing.traceWaitUntil
import com.rrparallelstudio.miapp.testing.waitForTransactionRow
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class ApprovalLinkFlowUiTest : MmrHiltUiTest() {
    @Inject
    lateinit var approvalRepository: ApprovalRepository

    @Test
    fun linkAction_showsLinkDialogWithLedgerRows() {
        val fixture =
            runBlocking {
                MmrUiTestFixtures.seedSmsMergeScenario(database, approvalRepository)
            }

        traceActivityRecreate()

        composeRule.traceClickTag(MmrTestTags.homeTopBarApprovals)
        composeRule.traceWaitUntil("wait_until", timeoutMillis = 10_000) {
            composeRule
                .onAllNodesWithTag(MmrTestTags.approvalCard(fixture.approvalId))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule.traceClickTag(MmrTestTags.approvalLink(fixture.approvalId))
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Link to existing transaction").assertExists()
        composeRule.waitForTransactionRow(fixture.transactionId)
    }
}
