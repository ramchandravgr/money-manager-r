package com.rrparallelstudio.miapp

import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rrparallelstudio.miapp.core.domain.repository.ApprovalRepository
import com.rrparallelstudio.miapp.testing.MmrHiltUiTest
import com.rrparallelstudio.miapp.testing.MmrTestTags
import com.rrparallelstudio.miapp.testing.MmrUiTestFixtures
import com.rrparallelstudio.miapp.testing.traceClickTag
import com.rrparallelstudio.miapp.testing.traceWaitUntil
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
    fun linkAction_showsGlobalLedgerPicker() {
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
        composeRule.traceWaitUntil("wait_until", timeoutMillis = 10_000) {
            composeRule
                .onAllNodesWithTag(
                    MmrTestTags.transactionRow(fixture.transactionId),
                ).fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithText("Link to existing transaction").assertExists()
        composeRule.onNodeWithTag(MmrTestTags.transactionRow(fixture.transactionId)).assertExists()
    }
}
