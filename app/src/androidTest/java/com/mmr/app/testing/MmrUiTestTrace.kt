package com.mmr.app.testing

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.espresso.Espresso

/** Pre- and post-action PNG capture for a full UI trace. */
fun ComposeTestRule.traceAction(
    label: String,
    block: ComposeTestRule.() -> Unit,
) {
    MmrUiScreenshotTrace.capture(this, phase = "pre", actionLabel = label)
    block()
    waitForIdle()
    MmrUiScreenshotTrace.capture(this, phase = "post", actionLabel = label)
}

fun ComposeTestRule.traceClickTag(
    tag: String,
    label: String = "click_tag:$tag",
) {
    traceAction(label) {
        onNodeWithTag(tag).performClick()
    }
}

fun ComposeTestRule.traceClickText(
    text: String,
    substring: Boolean = false,
    label: String = "click_text:$text",
) {
    traceAction(label) {
        onNodeWithText(text, substring = substring).performClick()
    }
}

fun ComposeTestRule.traceClickContentDescription(
    description: String,
    label: String = "click_cd:$description",
) {
    traceAction(label) {
        onNodeWithContentDescription(description).performClick()
    }
}

fun ComposeTestRule.traceTextInputTag(
    tag: String,
    text: String,
    label: String = "input_tag:$tag",
) {
    traceAction(label) {
        onNodeWithTag(tag).performTextInput(text)
    }
}

fun ComposeTestRule.traceTextClearanceTag(
    tag: String,
    label: String = "clear_tag:$tag",
) {
    traceAction(label) {
        onNodeWithTag(tag).performTextClearance()
    }
}

fun ComposeTestRule.tracePressBack(label: String = "press_back") {
    traceAction(label) {
        Espresso.pressBack()
    }
}

fun ComposeTestRule.traceScrollToTagAndClick(
    tag: String,
    label: String = "scroll_click_tag:$tag",
) {
    traceAction(label) {
        onNodeWithTag(tag).performScrollTo().performClick()
    }
}

fun ComposeTestRule.traceInteraction(
    label: String,
    interaction: SemanticsNodeInteraction,
    click: Boolean = true,
) {
    traceAction(label) {
        if (click) {
            interaction.performClick()
        }
    }
}

fun ComposeTestRule.traceWaitUntil(
    label: String,
    timeoutMillis: Long = 10_000,
    condition: () -> Boolean,
) {
    traceAction("wait_until:$label") {
        waitUntil(timeoutMillis) { condition() }
    }
}

fun ComposeTestRule.traceWaitForIdle(label: String = "wait_idle") {
    traceAction(label) {
        waitForIdle()
    }
}

fun ComposeTestRule.traceMatcherClick(
    matcher: SemanticsMatcher,
    label: String,
) {
    traceAction(label) {
        onNode(matcher).performClick()
    }
}

fun ComposeTestRule.traceNodeClick(
    label: String,
    interaction: () -> SemanticsNodeInteraction,
) {
    traceAction(label) {
        interaction().performClick()
    }
}

fun ComposeTestRule.traceLongClickTag(
    tag: String,
    label: String = "long_click_tag:$tag",
) {
    traceAction(label) {
        onNodeWithTag(tag).performTouchInput { longClick() }
    }
}

fun ComposeTestRule.traceNodeTextInput(
    label: String,
    interaction: () -> SemanticsNodeInteraction,
    text: String,
) {
    traceAction(label) {
        interaction().performTextInput(text)
    }
}
