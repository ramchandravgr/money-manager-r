package com.mmr.app

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mmr.app.testing.MmrHiltUiTest
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.testing.MmrUiTestFixtures
import com.mmr.app.testing.traceClickTag
import com.mmr.app.testing.traceWaitUntil
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Reads the **physical device** SMS inbox (USB debugging). Does not insert fake SMS.
 *
 * Before running: grant READ_SMS if prompted once manually; keep at least one parseable bank
 * transaction SMS in the app?s SMS scan lookback window (Settings ? configuration / scan months).
 */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class ApprovalInboxScanUiTest : MmrHiltUiTest() {
    @Test
    fun scanSms_readsDeviceInbox_findsTransactionMessages() {
        grantReadSmsForTestApp()
        runBlocking { MmrUiTestFixtures.clearApprovals(database) }

        composeRule.traceClickTag(MmrTestTags.homeTopBarApprovals)
        composeRule.waitForIdle()
        composeRule.traceClickTag(MmrTestTags.approvalScanSms)

        composeRule.traceWaitUntil("wait_until", timeoutMillis = 45_000) {
            composeRule
                .onAllNodesWithText("Scanning?", substring = true)
                .fetchSemanticsNodes()
                .isEmpty()
        }

        composeRule.traceWaitUntil("wait_until", timeoutMillis = 10_000) {
            scanResultBannerVisible()
        }

        composeRule.onNodeWithText("SMS permission is required", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("Error scanning SMS", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("No transaction SMS found", substring = true).assertDoesNotExist()
    }

    private fun scanResultBannerVisible(): Boolean {
        val markers =
            listOf(
                "Added ",
                "matched transaction",
                "No new transactions",
                "mandate notice",
                "statement notice",
                "IPO/SIP hold",
                "credit card payment",
                "duplicate HP Pay",
                "outdated pending",
            )
        return markers.any { snippet ->
            composeRule
                .onAllNodesWithText(snippet, substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun grantReadSmsForTestApp() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val pkg = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(
            pkg,
            Manifest.permission.READ_SMS,
        )
    }
}
