package com.rrparallelstudio.miapp

import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rrparallelstudio.miapp.core.data.preferences.LanSyncWifiTrustStore
import com.rrparallelstudio.miapp.core.domain.repository.LanSyncRepository
import com.rrparallelstudio.miapp.testing.MmrHiltUiTest
import com.rrparallelstudio.miapp.testing.MmrTestTags
import com.rrparallelstudio.miapp.testing.MmrUiTestFixtures
import com.rrparallelstudio.miapp.testing.PeerDeviceSyncTestSupport
import com.rrparallelstudio.miapp.testing.traceClickContentDescription
import com.rrparallelstudio.miapp.testing.traceClickTag
import com.rrparallelstudio.miapp.testing.traceWaitUntil
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Two-device Peer Device Sync over real Wi‑Fi + NSD.
 *
 * Run host and client separately via [scripts/run-peer-sync-dual-device-ui-test.ps1]:
 * - Host: `-e peer_sync_role=host`
 * - Client: `-e peer_sync_role=client` + pairing PIN and expected ledger note from host log.
 */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class PeerDeviceSyncE2eUiTest : MmrHiltUiTest() {
    @Inject
    lateinit var lanSyncRepository: LanSyncRepository

    @Inject
    lateinit var wifiTrustStore: LanSyncWifiTrustStore

    @Before
    fun setUpPeerSyncE2e() {
        PeerDeviceSyncTestSupport.enableCredentialBypass()
    }

    @After
    fun tearDownPeerSyncE2e() {
        PeerDeviceSyncTestSupport.disableCredentialBypass()
    }

    @Test
    fun hostRole_pairApproveAndSyncNow() =
        runBlocking {
            assumeTrue(
                "Set -e peer_sync_role=host to run this test.",
                InstrumentationRegistry.getArguments().getString("peer_sync_role") == "host",
            )
            prepareMainShell()
            PeerDeviceSyncTestSupport.assumeOnWifi(lanSyncRepository)
            val seedNote = "E2E-host-${System.currentTimeMillis()}"
            PeerDeviceSyncTestSupport.seedHostExpense(database, seedNote)
            PeerDeviceSyncTestSupport.trustCurrentWifiIfKnown(lanSyncRepository, wifiTrustStore)
            PeerDeviceSyncTestSupport.openPeerDeviceSync(composeRule)
            PeerDeviceSyncTestSupport.enableLanSyncSwitch(composeRule)
            PeerDeviceSyncTestSupport.waitForPairingCode(composeRule)
            val pin = PeerDeviceSyncTestSupport.readPairingCodeFromRepository(lanSyncRepository)
            assumeTrue("Host pairing PIN must be set.", pin.isNotEmpty())
            val hostDeviceId = lanSyncRepository.sessionState.value.deviceId
            PeerDeviceSyncTestSupport.logE2eHostHandshake(pin, seedNote, hostDeviceId)
            composeRule.traceWaitUntil("client_pair_pending", timeoutMillis = 180_000) {
                runBlocking {
                    lanSyncRepository.refreshSessionState()
                    lanSyncRepository.sessionState.value.pendingPairRequests
                        .isNotEmpty()
                }
            }
            val pending =
                lanSyncRepository.sessionState.value.pendingPairRequests
                    .first()
            PeerDeviceSyncTestSupport.openPeerDeviceSync(composeRule)
            val approveTag = "${MmrTestTags.lanSyncApprovePairPrefix}${pending.remoteDeviceId}"
            PeerDeviceSyncTestSupport.waitForApprovePairRequest(composeRule, approveTag)
            composeRule.traceClickTag(approveTag)
            composeRule.waitForIdle()
            PeerDeviceSyncTestSupport.waitForSyncNowEnabled(composeRule)
            PeerDeviceSyncTestSupport.tapSyncNow(composeRule)
            composeRule.traceWaitUntil("host_sync_log_success", timeoutMillis = 120_000) {
                composeRule
                    .onAllNodesWithText("Recent syncs", substring = true)
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
        }

    @Test
    fun clientRole_trustsHostAndReceivesLedger() {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(
            "Set -e peer_sync_role=client to run this test.",
            args.getString("peer_sync_role") == "client",
        )
        val pairingPin =
            args.getString("e2e_pairing_pin")?.trim().orEmpty()
        val seedNote = args.getString("e2e_seed_note")?.trim().orEmpty()
        assumeTrue("e2e_pairing_pin required.", pairingPin.isNotEmpty())
        assumeTrue("e2e_seed_note required.", seedNote.isNotEmpty())
        prepareMainShell()
        PeerDeviceSyncTestSupport.assumeOnWifi(lanSyncRepository)
        runBlocking { MmrUiTestFixtures.clearTransactions(database) }
        PeerDeviceSyncTestSupport.trustCurrentWifiIfKnown(lanSyncRepository, wifiTrustStore)
        PeerDeviceSyncTestSupport.openPeerDeviceSync(composeRule)
        PeerDeviceSyncTestSupport.enableLanSyncSwitch(composeRule)
        PeerDeviceSyncTestSupport.trustPeerWithPin(composeRule, pairingPin)
        composeRule.traceWaitUntil("client_trusted_peer", timeoutMillis = 60_000) {
            runBlocking {
                lanSyncRepository.refreshSessionState()
                lanSyncRepository.sessionState.value.trustedDevices
                    .isNotEmpty()
            }
        }
        PeerDeviceSyncTestSupport.waitForSyncNowEnabled(composeRule)
        PeerDeviceSyncTestSupport.tapSyncNow(composeRule)
        composeRule.traceClickContentDescription("Back")
        composeRule.waitForIdle()
        composeRule.traceClickTag(MmrTestTags.navHome)
        composeRule.waitForIdle()
        composeRule.traceWaitUntil("client_sees_host_txn", timeoutMillis = 60_000) {
            composeRule.onAllNodesWithText(seedNote).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(MmrTestTags.navHome).assertExists()
    }
}
