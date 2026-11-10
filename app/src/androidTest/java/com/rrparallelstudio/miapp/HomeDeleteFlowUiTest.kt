package com.rrparallelstudio.miapp

import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rrparallelstudio.miapp.testing.MmrHiltUiTest
import com.rrparallelstudio.miapp.testing.MmrTestTags
import com.rrparallelstudio.miapp.testing.MmrUiTestFixtures
import com.rrparallelstudio.miapp.testing.traceClickTag
import com.rrparallelstudio.miapp.testing.traceClickText
import com.rrparallelstudio.miapp.testing.traceLongClickTag
import com.rrparallelstudio.miapp.testing.traceWaitUntil
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
