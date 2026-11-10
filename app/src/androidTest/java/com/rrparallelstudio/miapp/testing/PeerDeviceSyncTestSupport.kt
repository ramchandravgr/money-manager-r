package com.rrparallelstudio.miapp.testing

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.rrparallelstudio.miapp.core.data.preferences.LanSyncWifiTrustStore
import com.rrparallelstudio.miapp.core.domain.repository.LanSyncRepository
import com.rrparallelstudio.miapp.security.MmrDeviceCredentialGate
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assume.assumeTrue

object PeerDeviceSyncE2eLog {
    const val TAG = "PEER_SYNC_E2E"
}

object PeerDeviceSyncTestSupport {
    fun enableCredentialBypass() {
        MmrDeviceCredentialGate.bypassForInstrumentedTests = true
    }

    fun disableCredentialBypass() {
        MmrDeviceCredentialGate.bypassForInstrumentedTests = false
    }

    fun assumeOnWifi(lanSyncRepository: LanSyncRepository) {
        runBlocking { lanSyncRepository.refreshSessionState() }
        val onWifi = lanSyncRepository.sessionState.value.onWifi
        assumeTrue("Peer Device Sync UI tests require Wi‑Fi.", onWifi)
    }

    fun trustCurrentWifiIfKnown(
        lanSyncRepository: LanSyncRepository,
        wifiTrustStore: LanSyncWifiTrustStore,
    ) {
        runBlocking {
            lanSyncRepository.refreshSessionState()
            val ssid = lanSyncRepository.sessionState.value.wifiSsid
            if (!ssid.isNullOrBlank()) {
                wifiTrustStore.markTrusted(ssid)
            }
        }
    }

    fun openPeerDeviceSync(composeRule: ComposeTestRule) {
        composeRule.traceClickTag(MmrTestTags.navSettings)
        composeRule.waitForIdle()
        composeRule.traceClickTag(MmrTestTags.settingsNavLanSync)
        composeRule.waitForIdle()
    }

    fun enableLanSyncSwitch(composeRule: ComposeTestRule) {
        composeRule.traceClickTag(MmrTestTags.lanSyncEnabledSwitch)
        composeRule.waitForIdle()
        dismissWifiTrustDialogIfShown(composeRule)
    }

    fun dismissWifiTrustDialogIfShown(composeRule: ComposeTestRule) {
        val trust =
            composeRule
                .onAllNodesWithText("Trust network")
                .fetchSemanticsNodes()
        if (trust.isNotEmpty()) {
            composeRule.onNodeWithText("Trust network").assertExists()
            composeRule.onNodeWithText("Trust network").performClick()
            composeRule.waitForIdle()
        }
    }

    fun waitForPairingCode(composeRule: ComposeTestRule) {
        composeRule.traceWaitUntil("lan_sync_pairing_code", timeoutMillis = 15_000) {
            runCatching {
                composeRule
                    .onNodeWithTag(MmrTestTags.lanSyncPairingOfferValue)
                    .assertExists()
                true
            }.getOrDefault(false)
        }
    }

    fun readPairingCodeFromRepository(lanSyncRepository: LanSyncRepository): String =
        runBlocking {
            lanSyncRepository.refreshSessionState()
            lanSyncRepository.sessionState.value.pairingOfferText
                .trim()
        }

    fun logE2eHostHandshake(
        pairingPin: String,
        seedNote: String,
        deviceId: String,
    ) {
        val payload =
            JSONObject()
                .put("pairingPin", pairingPin)
                .put("seedNote", seedNote)
                .put("hostDeviceId", deviceId)
        android.util.Log.i(PeerDeviceSyncE2eLog.TAG, payload.toString())
    }

    fun waitForSyncNowEnabled(composeRule: ComposeTestRule) {
        composeRule.traceWaitUntil("sync_now_enabled", timeoutMillis = 120_000) {
            runCatching {
                composeRule.onNodeWithTag(MmrTestTags.lanSyncSyncNow).assertIsEnabled()
                true
            }.getOrDefault(false)
        }
    }

    fun tapSyncNow(composeRule: ComposeTestRule) {
        composeRule.traceClickTag(MmrTestTags.lanSyncSyncNow)
        composeRule.waitForIdle()
    }

    fun waitForApprovePairRequest(
        composeRule: ComposeTestRule,
        approveTestTag: String,
    ) {
        composeRule.traceWaitUntil("pending_pair_approve", timeoutMillis = 180_000) {
            runCatching {
                composeRule.onNodeWithTag(approveTestTag).assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
    }

    fun trustPeerWithPin(
        composeRule: ComposeTestRule,
        pairingPin: String,
    ) {
        composeRule.traceTextInputTag(MmrTestTags.lanSyncPairingInput, pairingPin)
        composeRule.traceClickTag(MmrTestTags.lanSyncTrustAdd)
        composeRule.waitForIdle()
    }

    suspend fun seedHostExpense(
        database: com.rrparallelstudio.miapp.core.database.MmrDatabase,
        note: String,
        amount: Double = 500.0,
    ) {
        MmrUiTestFixtures.clearTransactions(database)
        val accountId =
            database
                .accountDao()
                .getAccountsOnce()
                .firstOrNull()
                ?.id
                ?: database.accountDao().insert(
                    com.rrparallelstudio.miapp.core.database.entity.AccountEntity(
                        name = "E2E Cash",
                        type = com.rrparallelstudio.miapp.core.model.enums.AccountType.CASH,
                        isDefault = true,
                    ),
                )
        database.transactionDao().insert(
            com.rrparallelstudio.miapp.core.database.entity.TransactionEntity(
                dateTime = System.currentTimeMillis(),
                amount = amount,
                type = com.rrparallelstudio.miapp.core.model.enums.TransactionType.EXPENSE,
                accountId = accountId,
                note = note,
                sourceType = com.rrparallelstudio.miapp.core.model.enums.TransactionSourceType.MANUAL,
                status = com.rrparallelstudio.miapp.core.model.enums.RecordStatus.APPROVED,
            ),
        )
    }
}
