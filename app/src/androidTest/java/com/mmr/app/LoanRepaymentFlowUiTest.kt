package com.mmr.app

import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.app.testing.MmrHiltUiTest
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.testing.MmrUiTestFixtures
import com.mmr.app.testing.traceClickTag
import com.mmr.app.testing.traceTextInputTag
import com.mmr.app.testing.traceWaitUntil
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

        traceActivityRecreate()

        composeRule.traceClickTag(MmrTestTags.navLoans)
        composeRule.traceClickTag(MmrTestTags.loanRow(loanId))
        composeRule.waitForIdle()

        composeRule.traceClickTag(MmrTestTags.loanDetailFabRepayment)
        composeRule.onNodeWithText("Record payment").assertExists()
        composeRule.traceTextInputTag(MmrTestTags.loanRepaymentAmount, "250")
        composeRule.traceClickTag(MmrTestTags.loanRepaymentConfirm)

        composeRule.traceWaitUntil("wait_until", timeoutMillis = 15_000) {
            runCatching {
                composeRule.onNodeWithText("750.00", substring = true).assertExists()
                true
            }.getOrDefault(false)
        }
    }
}
