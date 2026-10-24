package com.mmr.app

import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.app.testing.MmrHiltUiTest
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.testing.traceClickTag
import com.mmr.app.testing.traceTextInputTag
import com.mmr.app.testing.traceWaitUntil
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class CategoryAddFlowUiTest : MmrHiltUiTest() {
    @Test
    fun addCategory_appearsInList() {
        val categoryName = "UiTest cat ${System.nanoTime()}"
        waitForMainNavReady()

        composeRule.traceClickTag(MmrTestTags.navSettings)
        composeRule.waitForIdle()
        composeRule.traceClickTag(MmrTestTags.settingsNavCategories)
        composeRule.waitForIdle()

        composeRule.traceClickTag(MmrTestTags.settingsListAddCategories)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("New category").assertExists()

        composeRule.traceTextInputTag(MmrTestTags.categoryFieldName, categoryName)
        composeRule.traceClickTag(MmrTestTags.appBarSave)

        composeRule.traceWaitUntil("wait_until", timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(categoryName).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(categoryName).assertExists()
    }
}
