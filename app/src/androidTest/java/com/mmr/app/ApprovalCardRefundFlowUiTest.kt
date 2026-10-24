package com.mmr.app

import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.app.testing.MmrHiltUiTest
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.testing.MmrUiTestFixtures
import com.mmr.app.testing.assertTransactionRowShowsExpense
import com.mmr.app.testing.traceClickTag
import com.mmr.app.testing.traceClickText
import com.mmr.app.testing.tracePressBack
import com.mmr.app.testing.traceWaitUntil
import com.mmr.core.domain.repository.ApprovalRepository
import com.mmr.core.model.enums.SmsApprovalState
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class ApprovalCardRefundFlowUiTest : MmrHiltUiTest() {
    @Inject
    lateinit var approvalRepository: ApprovalRepository

    @Test
    fun applyRefund_reducesPurchaseAndClearsPendingRefund() {
        val fixture =
            runBlocking {
                MmrUiTestFixtures.seedCardRefundApplyScenario(
                    database = database,
                    approvalRepository = approvalRepository,
                    purchaseAmount = 500.0,
                    refundAmount = 150.0,
                )
            }

        traceActivityRecreate()

        composeRule.traceClickTag(MmrTestTags.homeTopBarApprovals)
        composeRule.traceWaitUntil("refund_card_visible", timeoutMillis = 10_000) {
            composeRule
                .onAllNodesWithTag(MmrTestTags.approvalCard(fixture.refundApprovalId))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule.traceClickTag(MmrTestTags.approvalQuickApprove(fixture.refundApprovalId))
        composeRule.traceWaitUntil("apply_refund_dialog", timeoutMillis = 15_000) {
            composeRule
                .onAllNodesWithText("Apply refund", substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule.traceWaitUntil("purchase_in_picker", timeoutMillis = 15_000) {
            composeRule
                .onAllNodesWithTag(MmrTestTags.transactionRow(fixture.purchaseTransactionId))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.traceClickTag(MmrTestTags.transactionRow(fixture.purchaseTransactionId))
        composeRule.traceWaitUntil("refund_amount_step", timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Refund amount").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.traceClickText("Apply refund")

        composeRule.traceWaitUntil("pending_refund_cleared", timeoutMillis = 15_000) {
            composeRule
                .onAllNodesWithTag(MmrTestTags.approvalCard(fixture.refundApprovalId))
                .fetchSemanticsNodes()
                .isEmpty()
        }

        val pending =
            runBlocking {
                approvalRepository.getApprovalsByState(SmsApprovalState.PENDING).first()
            }
        assertEquals(0, pending.size)

        composeRule.tracePressBack()
        composeRule.assertTransactionRowShowsExpense(
            fixture.purchaseTransactionId,
            fixture.netAmountAfterRefund,
        )
    }
}
