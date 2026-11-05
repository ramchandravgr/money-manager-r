package com.mmr.app

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.app.testing.MmrHiltUiTest
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.testing.traceClickTag
import com.mmr.app.testing.traceClickText
import com.mmr.app.testing.traceMatcherClick
import com.mmr.app.testing.traceTextInputTag
import com.mmr.app.testing.traceWaitUntil
import com.mmr.core.data.catalog.MmrBuiltInAccounts
import com.mmr.core.database.entity.AccountEntity
import com.mmr.core.model.enums.AccountType
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class TransferTransactionFlowUiTest : MmrHiltUiTest() {
    @Before
    fun seedSecondAccount() {
        runBlocking {
            database.accountDao().insert(
                AccountEntity(name = "Main Bank Account", type = AccountType.BANK),
            )
        }
    }

    @Test
    fun addTransfer_betweenSavingsAndBank_appearsOnHome() {
        val note = "UiTest transfer ${System.currentTimeMillis()}"
        composeRule.traceClickTag(MmrTestTags.homeFabAdd)
        composeRule.waitForIdle()

        composeRule.traceClickTag(MmrTestTags.transactionTypeTransfer)
        composeRule.traceTextInputTag(MmrTestTags.transactionAmount, "75")
        composeRule.traceTextInputTag(MmrTestTags.transactionNote, note)

        composeRule.traceClickText("From account")
        composeRule.waitForIdle()
        composeRule.traceMatcherClick(
            hasText(MmrBuiltInAccounts.DEFAULT_ACCOUNT_NAME) and hasClickAction() and isEnabled(),
            "pick_savings_account",
        )

        composeRule.traceClickText("To")
        composeRule.waitForIdle()
        composeRule.traceMatcherClick(
            hasText("Main Bank Account") and hasClickAction() and isEnabled(),
            "pick_main_bank_account",
        )

        composeRule.traceClickTag(MmrTestTags.appBarSave)

        composeRule.traceWaitUntil("wait_until", timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(note).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(note).assertExists()
    }
}
