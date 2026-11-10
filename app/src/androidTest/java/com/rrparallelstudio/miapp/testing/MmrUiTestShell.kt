package com.rrparallelstudio.miapp.testing

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.rrparallelstudio.miapp.core.data.preferences.AppDisplaySettingsStore
import com.rrparallelstudio.miapp.core.data.preferences.AppSecuritySettingsStore
import com.rrparallelstudio.miapp.core.data.preferences.FtueStore
import com.rrparallelstudio.miapp.core.data.preferences.LaunchPermissionStore
import com.rrparallelstudio.miapp.core.data.preferences.ReleaseNotesStore
import com.rrparallelstudio.miapp.security.MmrDeviceCredentialGate

/**
 * Brings [MainActivity] to an interactive main shell (past splash, lock, FTUE) for Compose UI tests.
 */
object MmrUiTestShell {
    fun stabilizeLaunchPrefs(
        ftueStore: FtueStore,
        launchPermissionStore: LaunchPermissionStore,
        releaseNotesStore: ReleaseNotesStore,
        displaySettingsStore: AppDisplaySettingsStore,
        appSecuritySettingsStore: AppSecuritySettingsStore,
        appVersionName: String,
    ) {
        MmrDeviceCredentialGate.bypassForInstrumentedTests = true
        ftueStore.markCompleted()
        launchPermissionStore.markLaunchPermissionFlowCompleted()
        releaseNotesStore.markAcknowledged(appVersionName)
        displaySettingsStore.applySnapshot(
            displaySettingsStore.snapshot().copy(hiddenMainNavIds = emptySet()),
        )
    }

    fun dismissReleaseNotesIfPresent(composeRule: ComposeTestRule) {
        val close =
            runCatching {
                composeRule.onAllNodesWithTag(MmrTestTags.releaseNotesClose).fetchSemanticsNodes()
            }.getOrDefault(emptyList())
        if (close.isNotEmpty()) {
            composeRule.traceAction("dismiss_release_notes") {
                composeRule.onNodeWithTag(MmrTestTags.releaseNotesClose).performClick()
                composeRule.waitForIdle()
            }
        }
    }

    private val mainNavTags =
        listOf(
            MmrTestTags.navHome,
            MmrTestTags.navStats,
            MmrTestTags.navBudgets,
            MmrTestTags.navScheduled,
            MmrTestTags.navSettings,
        )

    fun dismissFtueOverlayIfPresent(composeRule: ComposeTestRule) {
        val ftueNodes =
            runCatching {
                composeRule.onAllNodesWithTag(MmrTestTags.ftueScreen).fetchSemanticsNodes()
            }.getOrDefault(emptyList())
        if (ftueNodes.isNotEmpty()) {
            composeRule.traceAction("dismiss_ftue_skip") {
                composeRule.onNodeWithTag(MmrTestTags.ftueSkip).performClick()
                composeRule.waitForIdle()
            }
        }
    }

    fun dismissAppLockIfPresent(composeRule: ComposeTestRule) {
        val locked =
            runCatching {
                composeRule.onAllNodesWithText("Money Inbox is locked").fetchSemanticsNodes()
            }.getOrDefault(emptyList())
        if (locked.isNotEmpty()) {
            composeRule.traceAction("dismiss_app_lock") {
                composeRule.onNodeWithText("Unlock").performClick()
                composeRule.waitForIdle()
            }
        }
    }

    fun waitForAnyMainNavTab(
        composeRule: ComposeTestRule,
        timeoutMillis: Long = 45_000,
    ) {
        composeRule.traceWaitUntil("main_nav_any_visible", timeoutMillis = timeoutMillis) {
            if (
                mainNavTags.any { tag ->
                    runCatching {
                        composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
                    }.getOrDefault(false)
                }
            ) {
                true
            } else {
                dismissAppLockIfPresent(composeRule)
                false
            }
        }
        composeRule.waitForIdle()
    }
}
