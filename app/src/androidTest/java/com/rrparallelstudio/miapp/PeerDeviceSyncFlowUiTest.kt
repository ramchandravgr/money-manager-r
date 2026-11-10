package com.rrparallelstudio.miapp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rrparallelstudio.miapp.core.data.preferences.LanSyncWifiTrustStore
import com.rrparallelstudio.miapp.core.domain.repository.LanSyncRepository
import com.rrparallelstudio.miapp.testing.MmrHiltUiTest
import com.rrparallelstudio.miapp.testing.MmrTestTags
import com.rrparallelstudio.miapp.testing.PeerDeviceSyncTestSupport
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class PeerDeviceSyncFlowUiTest : MmrHiltUiTest() {
    @Inject
    lateinit var lanSyncRepository: LanSyncRepository

    @Inject
    lateinit var wifiTrustStore: LanSyncWifiTrustStore

    @Before
    fun setUpPeerSync() {
        PeerDeviceSyncTestSupport.enableCredentialBypass()
    }

    @After
    fun tearDownPeerSync() {
        PeerDeviceSyncTestSupport.disableCredentialBypass()
    }

    @Test
    fun enablePeerSync_showsPairingCodeOnWifi() {
        prepareMainShell()
        PeerDeviceSyncTestSupport.assumeOnWifi(lanSyncRepository)
        PeerDeviceSyncTestSupport.trustCurrentWifiIfKnown(lanSyncRepository, wifiTrustStore)
        PeerDeviceSyncTestSupport.openPeerDeviceSync(composeRule)
        PeerDeviceSyncTestSupport.enableLanSyncSwitch(composeRule)
        PeerDeviceSyncTestSupport.waitForPairingCode(composeRule)
        composeRule.onNodeWithTag(MmrTestTags.lanSyncPairingOfferValue).assertIsDisplayed()
    }

    @Test
    fun syncNow_disabledUntilTrustedPeerExists() {
        prepareMainShell()
        PeerDeviceSyncTestSupport.assumeOnWifi(lanSyncRepository)
        PeerDeviceSyncTestSupport.trustCurrentWifiIfKnown(lanSyncRepository, wifiTrustStore)
        PeerDeviceSyncTestSupport.openPeerDeviceSync(composeRule)
        PeerDeviceSyncTestSupport.enableLanSyncSwitch(composeRule)
        composeRule.onNodeWithTag(MmrTestTags.lanSyncSyncNow).assertIsNotEnabled()
    }
}
