package com.rrparallelstudio.miapp

import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rrparallelstudio.miapp.testing.MmrHiltUiTest
import com.rrparallelstudio.miapp.testing.MmrTestTags
import com.rrparallelstudio.miapp.testing.traceClickTag
import com.rrparallelstudio.miapp.testing.traceTextInputTag
import com.rrparallelstudio.miapp.testing.traceWaitUntil
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class AccountAddFlowUiTest : MmrHiltUiTest() {
    @Test
    fun addAccount_viaAppBarAdd_appearsInList() {
        val accountName = "UiTest acct ${System.nanoTime()}"
        waitForMainNavReady()

        composeRule.traceClickTag(MmrTestTags.navSettings)
        composeRule.waitForIdle()
        composeRule.traceClickTag(MmrTestTags.settingsNavAccounts)
        composeRule.waitForIdle()

        composeRule.traceClickTag(MmrTestTags.settingsListAddAccounts)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("New account").assertExists()

        composeRule.traceTextInputTag(MmrTestTags.accountFieldName, accountName)
        composeRule.traceClickTag(MmrTestTags.appBarSave)

        composeRule.traceWaitUntil("wait_until", timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(accountName).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(accountName).assertExists()
    }
}
