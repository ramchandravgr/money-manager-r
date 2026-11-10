package com.rrparallelstudio.miapp

import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rrparallelstudio.miapp.core.domain.repository.ApprovalRepository
import com.rrparallelstudio.miapp.testing.MmrHiltUiTest
import com.rrparallelstudio.miapp.testing.MmrTestTags
import com.rrparallelstudio.miapp.testing.MmrUiTestFixtures
import com.rrparallelstudio.miapp.testing.assertTransactionRowShowsExpense
import com.rrparallelstudio.miapp.testing.traceClickTag
import com.rrparallelstudio.miapp.testing.tracePressBack
import com.rrparallelstudio.miapp.testing.traceWaitUntil
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

        traceActivityRecreate()

        composeRule.traceClickTag(MmrTestTags.homeTopBarApprovals)
        composeRule.traceWaitUntil("wait_until", timeoutMillis = 10_000) {
            composeRule
                .onAllNodesWithTag(MmrTestTags.approvalCard(fixture.approvalId))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule.traceClickTag(MmrTestTags.approvalQuickApprove(fixture.approvalId))

        composeRule.tracePressBack()

        composeRule.traceWaitUntil("wait_until", timeoutMillis = 10_000) {
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
