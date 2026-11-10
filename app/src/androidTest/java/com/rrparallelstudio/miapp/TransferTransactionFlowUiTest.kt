package com.rrparallelstudio.miapp

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rrparallelstudio.miapp.core.data.catalog.MmrBuiltInAccounts
import com.rrparallelstudio.miapp.core.database.entity.AccountEntity
import com.rrparallelstudio.miapp.core.model.enums.AccountType
import com.rrparallelstudio.miapp.testing.MmrHiltUiTest
import com.rrparallelstudio.miapp.testing.MmrTestTags
import com.rrparallelstudio.miapp.testing.traceClickTag
import com.rrparallelstudio.miapp.testing.traceClickText
import com.rrparallelstudio.miapp.testing.traceMatcherClick
import com.rrparallelstudio.miapp.testing.traceTextInputTag
import com.rrparallelstudio.miapp.testing.traceWaitUntil
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
