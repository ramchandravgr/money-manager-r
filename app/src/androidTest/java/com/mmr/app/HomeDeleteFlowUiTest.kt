package com.mmr.app

import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.app.testing.MmrHiltUiTest
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.testing.MmrUiTestFixtures
import com.mmr.app.testing.traceClickTag
import com.mmr.app.testing.traceClickText
import com.mmr.app.testing.traceLongClickTag
import com.mmr.app.testing.traceWaitUntil
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class HomeDeleteFlowUiTest : MmrHiltUiTest() {
    @Test
    fun deleteSelectedExpense_removesFromHomeList() {
        val note = "Delete me ${System.nanoTime()}"
        val fixture =
            runBlocking {
                MmrUiTestFixtures.seedSingleExpense(database, note = note, amount = 15.0)
            }

        traceActivityRecreate()

        composeRule.traceLongClickTag(MmrTestTags.transactionRow(fixture.transactionId))
        composeRule.traceClickTag(MmrTestTags.homeSelectionDelete)
        composeRule.traceClickText("Delete")

        composeRule.traceWaitUntil("wait_until", timeoutMillis = 10_000) {
            runCatching {
                composeRule.onNodeWithText(note).assertDoesNotExist()
                true
            }.getOrDefault(false)
        }
    }
}
