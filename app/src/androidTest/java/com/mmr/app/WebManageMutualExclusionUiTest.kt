package com.mmr.app

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.app.testing.MmrHiltUiTest
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.testing.traceClickContentDescription
import com.mmr.app.testing.traceClickTag
import com.mmr.app.testing.traceTextInputTag
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class WebManageMutualExclusionUiTest : MmrHiltUiTest() {
    @Test
    fun enablingPeerSync_afterWebManage_turnsWebManageOff() {
        openWebManageAndEnable("lan-test-pass-99")

        composeRule.traceClickContentDescription("Back")
        composeRule.waitForIdle()

        composeRule.traceClickTag(MmrTestTags.settingsNavLanSync)
        composeRule.waitForIdle()
        composeRule.traceClickTag(MmrTestTags.lanSyncEnabledSwitch)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Peer Device Sync is on. Web Manage was turned off.").assertExists()

        composeRule.traceClickContentDescription("Back")
        composeRule.waitForIdle()
        composeRule.traceClickTag(MmrTestTags.settingsNavWebManage)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(MmrTestTags.webManageEnabledSwitch).assertIsOff()
    }

    @Test
    fun enablingWebManage_afterPeerSync_turnsPeerSyncOff() {
        composeRule.traceClickTag(MmrTestTags.navSettings)
        composeRule.waitForIdle()
        composeRule.traceClickTag(MmrTestTags.settingsNavLanSync)
        composeRule.waitForIdle()
        composeRule.traceClickTag(MmrTestTags.lanSyncEnabledSwitch)
        composeRule.waitForIdle()

        composeRule.traceClickContentDescription("Back")
        composeRule.waitForIdle()
        openWebManageAndEnable("web-test-pass-88")

        composeRule.onNodeWithText("Web Manage is on. Peer Device Sync was turned off.").assertExists()

        composeRule.traceClickContentDescription("Back")
        composeRule.waitForIdle()
        composeRule.traceClickTag(MmrTestTags.settingsNavLanSync)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(MmrTestTags.lanSyncEnabledSwitch).assertIsOff()
    }

    private fun openWebManageAndEnable(password: String) {
        composeRule.traceClickTag(MmrTestTags.navSettings)
        composeRule.waitForIdle()
        composeRule.traceClickTag(MmrTestTags.settingsNavWebManage)
        composeRule.waitForIdle()
        composeRule.traceTextInputTag(MmrTestTags.webManagePasswordField, password)
        composeRule.traceClickTag(MmrTestTags.webManagePasswordSave)
        composeRule.waitForIdle()
        composeRule.traceClickTag(MmrTestTags.webManageEnabledSwitch)
        composeRule.waitForIdle()
    }
}
