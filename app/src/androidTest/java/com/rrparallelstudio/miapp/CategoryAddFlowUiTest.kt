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
