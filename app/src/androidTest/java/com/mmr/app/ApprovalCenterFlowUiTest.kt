package com.mmr.app

import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.app.testing.MmrHiltUiTest
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.testing.MmrUiTestFixtures
import com.mmr.app.testing.assertTransactionRowShowsExpense
import com.mmr.core.domain.repository.ApprovalRepository
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class ApprovalCenterFlowUiTest : MmrHiltUiTest() {
    @Inject
    lateinit var approvalRepository: ApprovalRepository

    @Test
    fun quickApprove_postsTransactionOnHome() {
        val merchant = "UiTest Approval ${System.currentTimeMillis()}"
        val fixture =
            runBlocking {
                MmrUiTestFixtures.seedPendingApproval(
                    database = database,
                    approvalRepository = approvalRepository,
                    merchant = merchant,
                    amount = 315.0,
                )
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

        Espresso.pressBack()
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(merchant).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(merchant).assertExists()
        val transactionId =
            runBlocking {
                database
                    .transactionDao()
                    .getApprovedTransactionsDetailed()
                    .first()
                    .single { it.transaction.note?.contains(merchant) == true }
                    .transaction.id
            }
        composeRule.assertTransactionRowShowsExpense(transactionId, 315.0)
    }
}
