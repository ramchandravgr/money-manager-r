package com.mmr.app

import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.app.testing.MmrHiltUiTest
import com.mmr.app.testing.MmrTestTags
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class CategoryAddFlowUiTest : MmrHiltUiTest() {
    @Test
    fun addCategory_appearsInList() {
        val categoryName = "UiTest cat ${System.nanoTime()}"

        composeRule.onNodeWithTag(MmrTestTags.navSettings).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(MmrTestTags.settingsNavCategories).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("+").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("New category").assertExists()

        composeRule.onNodeWithText("Name").performTextInput(categoryName)
        composeRule.onNodeWithTag(MmrTestTags.appBarSave).performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(categoryName).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(categoryName).assertExists()
    }
}
