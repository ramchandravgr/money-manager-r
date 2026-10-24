package com.mmr.app.testing

import androidx.compose.ui.test.junit4.ComposeTestRule
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class MmrUiTestTraceRule(
    private val composeRule: ComposeTestRule,
) : TestWatcher() {
    override fun starting(description: Description) {
        MmrUiScreenshotTrace.startTest(
            className = description.className,
            methodName = description.methodName,
        )
    }

    override fun finished(description: Description) {
        composeRule.traceAction("test_finished") { }
        MmrUiScreenshotTrace.finishTest()
    }
}
