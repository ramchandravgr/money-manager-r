package com.mmr.app

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.app.testing.MmrHiltUiTest
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.testing.MmrUiTestFixtures
import com.mmr.core.domain.repository.LoanRepository
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class LoanRepaymentFlowUiTest : MmrHiltUiTest() {
    @Inject
    lateinit var loanRepository: LoanRepository

    @Test
    fun recordPartPayment_reducesOutstandingOnDetail() {
        val loanName = "Repay UI ${System.nanoTime()}"
        val loanId =
            runBlocking {
                MmrUiTestFixtures.seedFlexibleLoan(loanRepository, loanName, principal = 1_000.0)
            }

        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(MmrTestTags.navLoans).performClick()
        composeRule.onNodeWithTag(MmrTestTags.loanRow(loanId)).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(MmrTestTags.loanDetailFabRepayment).performClick()
        composeRule.onNodeWithText("Record payment").assertExists()
        composeRule.onNodeWithTag(MmrTestTags.loanRepaymentAmount).performTextInput("250")
        composeRule.onNodeWithTag(MmrTestTags.loanRepaymentConfirm).performClick()

        composeRule.waitUntil(timeoutMillis = 15_000) {
            runCatching {
                composeRule.onNodeWithText("750.00", substring = true).assertExists()
                true
            }.getOrDefault(false)
        }
    }
}
