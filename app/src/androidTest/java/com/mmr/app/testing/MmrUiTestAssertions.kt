package com.mmr.app.testing

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo

fun SemanticsNodeInteraction.assertShowsExpenseAmount(amount: Double) {
    assert(hasText(MmrUiTestFormat.expenseAmountFormatted(amount), substring = true))
}

fun ComposeTestRule.waitForTransactionRow(
    transactionId: Long,
    timeoutMillis: Long = 10_000L,
) {
    val tag = MmrTestTags.transactionRow(transactionId)
    waitUntil(timeoutMillis) {
        onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    }
    onNodeWithTag(tag).performScrollTo()
}

fun ComposeTestRule.assertTransactionRowShowsExpense(
    transactionId: Long,
    amount: Double,
) {
    waitForTransactionRow(transactionId)
    onNodeWithTag(MmrTestTags.transactionRow(transactionId)).assertShowsExpenseAmount(amount)
}

/** Search results share row formatting but may be off-screen in a nested list. */
fun ComposeTestRule.assertLedgerLineShowsExpense(
    note: String,
    amount: Double,
    timeoutMillis: Long = 15_000L,
) {
    val amountLabel = MmrUiTestFormat.expenseAmountFormatted(amount)
    waitUntil(timeoutMillis) {
        onAllNodes(
            hasText(note, substring = true) and hasText(amountLabel, substring = true),
        ).fetchSemanticsNodes().isNotEmpty()
    }
    onNode(hasText(note, substring = true) and hasText(amountLabel, substring = true)).assertExists()
}
