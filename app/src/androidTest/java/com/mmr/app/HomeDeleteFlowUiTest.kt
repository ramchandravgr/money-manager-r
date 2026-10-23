package com.mmr.app

import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.app.testing.MmrHiltUiTest
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.testing.MmrUiTestFixtures
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

        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()

        composeRule
            .onNodeWithTag(MmrTestTags.transactionRow(fixture.transactionId))
            .performTouchInput { longClick() }
        composeRule.onNodeWithTag(MmrTestTags.homeSelectionDelete).performClick()
        composeRule.onNodeWithText("Delete").performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                composeRule.onNodeWithText(note).assertDoesNotExist()
                true
            }.getOrDefault(false)
        }
    }
}
